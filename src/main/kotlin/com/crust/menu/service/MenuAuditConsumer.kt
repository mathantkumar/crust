package com.crust.menu.service

import com.crust.menu.domain.MenuAuditResult
import com.crust.menu.repository.MenuAuditResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.model.chat.ChatLanguageModel
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class MenuAuditConsumer(
    private val chatLanguageModel: ChatLanguageModel,
    private val menuAuditResultRepository: MenuAuditResultRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(MenuAuditConsumer::class.java)

    @RetryableTopic(attempts = "4")
    @KafkaListener(topics = ["menu.version.published"])
    fun onMenuPublished(payload: String) {
        val rootNode = objectMapper.readTree(payload)
        val versionId = rootNode.path("id").asText()

        if (versionId.isBlank()) return

        val prompt = """
            You are a Restaurant Revenue Specialist and financial auditor. Analyze this published menu JSON for revenue risks.

            Categorize each risk as exactly one of: REVENUE_LEAKAGE, TAX_COMPLIANCE, or PRICING_STRATEGY.

            Return ONLY a raw JSON array with no markdown, no code fences. Each element must have exactly these fields:
            - "category": one of REVENUE_LEAKAGE, TAX_COMPLIANCE, PRICING_STRATEGY
            - "impact_score": integer 1-10 based on estimated dollar loss potential (10 = highest loss)
            - "plain_english_summary": one sentence a restaurant owner would immediately understand
            - "suggested_action": one concrete corrective step

            Flag these patterns: items priced under ${'$'}1, alcoholic drinks missing a tax category, modifiers priced higher than their parent dish, any item with a zero or null price.

            Menu Data:
            $payload
        """.trimIndent()

        // Wrap AI call in a CompletableFuture with a 30-second timeout to prevent
        // blocking the Kafka consumer thread indefinitely
        val response = try {
            CompletableFuture.supplyAsync {
                chatLanguageModel.generate(prompt)
            }.get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            log.error("Gemini AI timed out after 30s for version $versionId — saving sentinel error record")
            saveSentinelError(versionId, "AI_TIMEOUT", "Gemini did not respond within 30 seconds. Menu was not audited.")
            return
        } catch (e: Exception) {
            log.error("Gemini AI failed for version $versionId: ${e.message}", e)
            throw RuntimeException("AI audit failed, will retry via @RetryableTopic", e)
        }

        val replyText = response.replace("```json", "").replace("```", "").trim()

        try {
            val risksNode = objectMapper.readTree(replyText)
            if (risksNode.isArray) {
                for (riskNode in risksNode) {
                    val category = riskNode.path("category").asText("PRICING_STRATEGY")
                    val impactScore = riskNode.path("impact_score").asInt(1)
                    val summary = riskNode.path("plain_english_summary").asText()
                    val action = riskNode.path("suggested_action").asText()

                    menuAuditResultRepository.save(
                        MenuAuditResult(
                            menuVersionId = versionId,
                            riskDescription = summary,
                            severityScore = impactScore,
                            category = category,
                            impactScore = impactScore,
                            plainEnglishSummary = summary,
                            suggestedAction = action
                        )
                    )
                }
                log.info("Saved ${risksNode.size()} audit risks for version $versionId")
            }
        } catch (e: Exception) {
            log.error("Failed to parse AI response for version $versionId: ${e.message}", e)
            throw RuntimeException("Failed to decode LLM response into required JSON layout", e)
        }
    }

    /**
     * Dead Letter Topic handler — fires after all @RetryableTopic retries are exhausted.
     * Instead of silently dropping the message, we save a sentinel error record
     * so operators can see that the audit failed.
     */
    @DltHandler
    fun handleDlt(payload: String) {
        log.error("Menu audit message exhausted all retries and landed in DLT: ${payload.take(200)}")
        try {
            val rootNode = objectMapper.readTree(payload)
            val versionId = rootNode.path("id").asText("UNKNOWN")
            saveSentinelError(versionId, "DLT_EXHAUSTED", "Menu audit failed after 4 retry attempts. Manual review required.")
        } catch (e: Exception) {
            log.error("Could not parse DLT payload to save sentinel: ${e.message}")
        }
    }

    private fun saveSentinelError(versionId: String, category: String, message: String) {
        menuAuditResultRepository.save(
            MenuAuditResult(
                menuVersionId = versionId,
                riskDescription = message,
                severityScore = 10,
                category = category,
                impactScore = 10,
                plainEnglishSummary = message,
                suggestedAction = "Re-trigger the audit manually or check Gemini API health."
            )
        )
    }
}
