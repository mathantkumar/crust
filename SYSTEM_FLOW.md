# Crust — Living Architecture & System Flow
*This is a living document. It must be updated whenever major architectural changes or new domains are introduced to the codebase.*

Crust is a modern, enterprise-grade restaurant management platform built with Spring Boot, Kotlin, and Netflix DGS (GraphQL). It is designed around domain-driven principles and event-driven architecture, featuring Graceful Degradation for AI features.

Here is the architectural flow broken down by core domains:

### 1. The API Gateway (GraphQL Layer)
All client traffic (Next.js Admin Dashboard, Android App) enters through a unified GraphQL API powered by Netflix DGS.
*   **`MenuGraphMutations`**: Handles all write operations (creating orders, publishing menus, bumping kitchen tickets, processing payments).
*   **`MenuGraphQueries`**: Handles all read operations (fetching active menus, active orders, reporting data, and demand forecasts).

### 2. Menu Management & AI Auditing (Event-Driven Pipeline)
This is the core flow for managing what the restaurant sells.
1.  **Publishing (`MenuCommandService`)**: When a manager publishes a new menu version, it is saved to the PostgreSQL database (`menu_version`, `category`, `menu_item`, etc.).
2.  **The Outbox Pattern (`OutboxRelay`)**: To ensure reliability, a "publish" event is written to an `outbox_event` table in the same database transaction. A scheduled background worker (`OutboxRelay`) polls this table and pushes the events to an Apache Kafka topic (`menu.version.published`).
3.  **AI Auditing (`MenuAuditConsumer`)**: A Kafka listener picks up the event and sends the menu JSON to Google Gemini (via LangChain4j). The AI analyzes the menu for revenue leakage, missing upsell modifiers, and pricing risks, then saves the findings to `menu_audit_result`.
    *   *Resiliency Note*: This uses **Graceful Degradation**. If the AI is down or rate-limited, the Kafka listener retries with exponential backoff. The main API is never blocked by the AI processing.

### 3. Order Management System (OMS)
When a customer or cashier creates an order, the flow goes through `OrderService`:
1.  **Creation**: An order is created with an `idempotencyKey` to prevent duplicate charges.
2.  **Historical Snapshots**: The system captures a "snapshot" of the menu at that exact moment. It saves the `categoryName`, `menuVersionId`, and structured `priceImpact` of every modifier to the `order_item` and `order_item_modifier` tables. This ensures that if a menu item's price changes tomorrow, historical analytics remain accurate.
3.  **Status Lifecycle**: Orders move through states (`PENDING` → `RECEIVED` → `PREPARING` → `READY` → `COMPLETED` / `CANCELLED`).

### 4. Kitchen Display System (KDS) & Payments
Once an order is placed, it branches into operational workflows:
*   **Kitchen (`KitchenDisplayService`)**: Items are routed to specific `KitchenStation`s (e.g., Grill, Fryer) as `KitchenTicket`s. Chefs interact with these tickets, moving them from `RECEIVED` → `PREPARING` → `READY` → `BUMPED`.
*   **Payments (`PaymentService`)**: Manages the financial state machine (`INITIATED` → `AUTHORIZED` → `CAPTURED` → `REFUNDED`).

### 5. Analytics & Data Pipeline
Crust doesn't just read raw tables for reporting; it features a background data pipeline to handle heavy analytics without slowing down the transactional database.
1.  **Real-time (`ReportingService`)**: Fetches immediate metrics like `getSalesSummary`, `productMix`, and `modifierRevenue` using fast, indexed queries.
2.  **Aggregation (`SalesAggregationService`)**: A scheduled job runs every 60 seconds. It looks for `COMPLETED` orders that haven't been processed yet (tracked by an `aggregated = false` flag).
3.  **Rollup**: It rolls these orders up into a pre-calculated, highly optimized `item_sales_hourly` table. It groups data by restaurant, location, item, date, and hour. Once rolled up, the order is marked `aggregated = true`.

### 6. Machine Learning / Demand Forecasting
The platform uses the aggregated analytics data to predict future sales.
1.  **The Engine (`DemandForecastService`)**: Runs every 6 hours in the background.
2.  **The Model (WMA-4w)**: It uses a Weighted Moving Average. For any given item and hour (e.g., Burgers on a Friday at 7 PM), it looks at the exact same hour over the last 4 weeks.
3.  **Weighting & Confidence**: It applies exponential decay weights (recent weeks matter more: 40%, 30%, 20%, 10%). It calculates a `confidence` score based on data completeness and historical variance.
4.  **Projection**: It multiplies the predicted quantity by the *current* menu price to project `predicted_revenue` and saves this to `item_demand_forecast`, which is surfaced to the UI via GraphQL.

### 7. Intelligent 86ing (Embedded ML Inference)
This pipeline bridges offline Machine Learning with real-time operational execution using an Embedded Inference architecture.
1.  **The Trigger**: When an order is completed, the `OrderService` fires an asynchronous `OrderCreatedEvent`.
2.  **Feature Assembly**: The `Intelligent86Service` intercepts the event and extracts a real-time `FeatureVector` for every item in the order. It gathers:
    *   *Temporal*: Time of day, day of week, payday weekend context.
    *   *Velocity*: Baseline WMA forecast, plus real-time database lookups for orders in the last 15 and 60 minutes.
    *   *Inventory*: Current stock level and stock-to-demand ratio.
3.  **Inference Simulator**: The feature vector is passed into the `XGBoostSimulator` object (which mocks an XGBoost `predict()` call) to get the predicted demand for the next 60 minutes.
4.  **Heuristic Guardrail & Audit**: Calculates a Risk Score (`predictedDemand / currentStock`). If > 1.0, it generates a `Predictive86Alert` database entry. This ensures human-in-the-loop observability (the "Toast Workflow") so managers can see exactly *why* an item is flagged for stock-out risk.

---

### End-to-End Execution Summary
*   **Setup:** Menu Published → Outbox → Kafka → AI Audit (Async)
*   **Transaction:** Order Placed → Snapshots Taken → Kitchen Tickets Routed → Payment Captured → Order Completed
*   **Real-time Intelligence:** Order Completed → Event Fired → Feature Assembly → ML Inference → Predictive 86 Alert Generated
*   **Batch Intelligence:** Order Completed → (60s later) Rolled up into Hourly Sales → (every 6 hours) Ingested by ML Engine → Demand Forecasts Generated for the next 7 days.
