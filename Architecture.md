# Flow Architecture: Graceful Degradation & Event Driven Patterns

## The "Observer" Sidecar (Event-Driven Sidecar)
When a menu is mutated / published, the core pipeline immediately resolves locally. We offload intensive non-atomic operations—like submitting a multi-level JSON blob to an LLM for parsing—entirely onto a parallel Message Queue via Spring Kafka.

This pattern enforces **Graceful Degradation**. Should the Gemini API exhibit severe transient failures, latency spikes, or quota exhaustion, it *cannot* cascade and bring down the main administrative API. The internal dashboard will seamlessly publish the version.

## LLM Backpressure and Rate Limit Resiliency (Kafka Retries)
By applying Spring Kafka's `@RetryableTopic`, failures (such as `429 Too Many Requests` API ceilings from Langchain4j integrations) immediately push the target payload onto isolated internal fallback queues incrementally backing off traffic patterns (`multiplier = 2.0`). This effectively establishes queue-based rate limits and retry boundaries protecting the target ecosystem natively!

## Graceful Degradation at the Bean Scope Level (Explicit Injection)
The traditional trap with auto-configurations is throwing raw `UnsatisfiedDependencyException` crashes during bootstrap when required cloud tokens (`LANGCHAIN4J_GOOGLE_AI_GEMINI_API_KEY`) are missing inside the underlying context.

As a **Staff-Level architectural decision**, we pivoted from relying blindly on magic auto-configurations to natively declaring the `ChatLanguageModel` explicitly via `AiConfig`.
If the expected Token is structurally unbound or missing, the system intercepts the error and injects a mapped local fallback. 

The application launches smoothly, menu publishing networks continue untouched, and the fallback simply logs `"Audit skipped: No AI key configured"`. True graceful degradation shields the central system's persistence lifecycle against peripheral sidecar constraints!
