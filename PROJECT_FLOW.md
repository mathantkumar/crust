# Crust — End-to-End Architecture & Project Flow

## System Overview

Crust is an AI-powered restaurant menu management platform. Operators publish menu versions via a GraphQL API; an event-driven pipeline asynchronously audits each publish for revenue risks using Google Gemini. Results surface in a React dashboard and an Android companion app.

---

## Entry Points

| Entry Point | Protocol | Port | Description |
|---|---|---|---|
| Next.js UI | HTTP / GraphQL | 3000 | Admin dashboard — publishes menus, views AI risk reports |
| Android App | HTTP / GraphQL | 8080 | Guest-facing menu browser with offline support |
| MCP Server | Direct JDBC | 5432 | Claude tool integration — reads menu and audit tables directly |
| Spring Boot API | GraphQL over HTTP | 8080 | Single unified API surface via Netflix DGS |

---

## Request / Data Flows

### Flow 1 — Publish a Menu Version (Write Path)

```
Operator (UI) 
  → POST /graphql { mutation publishMenu(versionId) }
  → DGS MenuGraphMutations.publishMenu()
  → MenuCommandService.publishMenuVersion()       [Transactional]
      → UPDATE menu_version SET status='PUBLISHED'
      → INSERT outbox_event (status=PENDING, payload={id, status})
  ← returns: true
```
The mutation returns immediately. The audit is fully async.

### Flow 2 — Outbox → Kafka (Relay Path)

```
OutboxRelay.processOutbox()                       [@Scheduled fixedDelay=5000ms]
  → SELECT * FROM outbox_event WHERE status='PENDING'
  → FOR EACH event:
      → kafkaTemplate.send("menu.version.published", id, payload).get()   [BLOCKING]
      → UPDATE outbox_event SET status='PROCESSED'
```
Events are serialised one-by-one. The `.get()` blocks the scheduler thread per event.

### Flow 3 — AI Audit (Consumer Path)

```
MenuAuditConsumer.onMenuPublished(payload)         [@KafkaListener, @RetryableTopic attempts=4]
  → Parse versionId from payload JSON
  → Build Gemini prompt (full payload embedded as context)
  → chatLanguageModel.generate(prompt)             [BLOCKING — Gemini HTTP call]
  → Parse JSON response array
  → FOR EACH risk:
      → INSERT menu_audit_result (category, impactScore, summary, action)
```
The entire consumer thread blocks on the Gemini call. Failed JSON parse → RuntimeException → Kafka retry × 4 → silent drop (no DLT handler configured).

### Flow 4 — Read Active Menu + Risks (Query Path)

```
Client → POST /graphql { query getActiveMenu }
  → DGS MenuGraphQueries.getActiveMenu()
  → MenuVersionRepository.findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
      → Single SQL with 4-level @EntityGraph JOIN FETCH
        (menu_version ⋈ category ⋈ menu_item ⋈ modifier_group ⋈ modifier)
  ← Full menu tree returned

Client → POST /graphql { query getMenuRisks(versionId) }
  → MenuAuditResultRepository.findByMenuVersionId(versionId)
  ← List of risk findings
```

### Flow 5 — Revert a Menu (Revert Path)

```
Operator → mutation revertToCleanVersion(versionId)
  → MenuCommandService.revertMenuVersion()
      → UPDATE menu_version SET status='REVERTED_DUE_TO_RISK'
  ← returns: true
⚠️ No version is promoted to PUBLISHED — getActiveMenu returns null after revert.
```

---

## Data Persistence Layer

### PostgreSQL Schema (Flyway V1–V4)

```
menu_version
  └── category (FK: menu_version_id)
        └── menu_item (FK: category_id)
              └── modifier_group (FK: menu_item_id | FK: parent_modifier_id)
                    └── modifier (FK: modifier_group_id)
                          └── modifier_group (self-referential child groups)

outbox_event          — transactional inbox/outbox; payload stored as JSONB
menu_audit_result     — AI risk findings per menu version (V3 + V4 schema expansion)
```

All primary keys are UUIDs. Correlation IDs thread through all entities for distributed tracing.

---

## External Integrations

| System | Library | Topic / Endpoint | Notes |
|---|---|---|---|
| Apache Kafka | spring-kafka | `menu.version.published` | Produce: OutboxRelay; Consume: MenuAuditConsumer |
| Google Gemini 2.0 Flash | LangChain4j 0.35 | Gemini API (HTTP) | Graceful degradation: no-op if API key missing |
| PostgreSQL 15 | Spring Data JPA + Flyway | localhost:5432 | JSONB for outbox payload; indexes on all correlation IDs |

---

## Identified Bottlenecks

### B1 — Blocking `.get()` in Scheduler Thread  `OutboxRelay.kt:18`
`kafkaTemplate.send(...).get()` is a synchronous block inside `@Scheduled`. Each event serialises Kafka delivery. A 100-event backlog means 100 × (Kafka RTT) before the scheduler thread is free again. **Fix:** fire-and-forget with async callbacks, or batch with `allOf()`.

### B2 — Fixed 5-Second Polling with No Backoff  `OutboxRelay.kt:13`
`@Scheduled(fixedDelay = 5000)` runs unconditionally. Idle periods waste DB connections; burst publishes build a backlog the fixed window cannot drain. **Fix:** use `fixedDelay` with an adaptive sleep, or switch to `@TransactionalEventListener` for immediate dispatch.

### B3 — Synchronous Gemini Call Blocks Kafka Consumer Thread  `MenuAuditConsumer.kt:46`
`chatLanguageModel.generate(prompt)` has no timeout. A slow or throttled Gemini response freezes the consumer partition until completion or retry. **Fix:** wrap in a `CompletableFuture` with a deadline, or move AI work to a separate thread pool and ack the Kafka message early.

### B4 — `revertMenuVersion` Leaves No Active Menu  `MenuCommandService.kt:52`
After `revertToCleanVersion`, no version is promoted to `PUBLISHED`. Every subsequent `getActiveMenu` query returns `null`, breaking the Android app and the admin dashboard. The comment in the source acknowledges this. **Fix:** find the most recent pre-revert `PUBLISHED` or `DRAFT` version and promote it atomically in the same transaction.

### B5 — Full 4-Level Cartesian Product on Every Menu Fetch  `Repositories.kt`
`@EntityGraph` across four `@OneToMany` levels generates a single SQL with multiple JOINs. For a menu with 5 categories × 10 items × 3 modifier groups × 5 modifiers, the ResultSet is 750 rows before Hibernate deduplicates. No pagination. **Fix:** use `@BatchSize` on child collections + separate queries, or paginate at the category level.

### B6 — Silent Drop After 4 AI Retries  `MenuAuditConsumer.kt:71`
`@RetryableTopic(attempts = "4")` retries on `RuntimeException` (bad LLM JSON). After 4 attempts, the message lands in a DLT with no configured handler. The operator sees no indication the audit failed. **Fix:** add a `@DltHandler` that writes a sentinel `menu_audit_result` with a system error category, or alerts via webhook.

---

## Tech Stack Summary

| Layer | Technology | Version |
|---|---|---|
| API Framework | Spring Boot + Netflix DGS (GraphQL) | 3.4.0 + 9.0.4 |
| Language | Kotlin | 2.1.0 |
| Database | PostgreSQL + Flyway | 15 + V4 migrations |
| Event Bus | Apache Kafka + spring-kafka | — |
| AI | LangChain4j + Google Gemini 2.0 Flash | 0.35.0 |
| Admin UI | Next.js + Apollo Client + Tailwind | 15 + React 19 |
| Mobile | Android + Jetpack Compose + Apollo Kotlin | API 26+ |
| Tooling | MCP Server (TypeScript) for Claude integration | — |
