package com.crust.menu.service

import com.crust.menu.domain.MenuAuditResult
import com.crust.menu.repository.MenuAuditResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.model.chat.ChatLanguageModel
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.stereotype.Service

@Service
class MenuAuditConsumer(
    private val chatLanguageModel: ChatLanguageModel,
    private val menuAuditResultRepository: MenuAuditResultRepository,
    private val objectMapper: ObjectMapper
) {

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
            - "plain_english_summary": one sentence a restaurant owner would immediately understand, e.g. "You are selling Ribeye Steak for less than the cost of a side salad."
            - "suggested_action": one concrete corrective step, e.g. "Update price to at least ${'$'}28.00 to maintain a 30% margin."

            Flag these patterns: items priced under ${'$'}1, alcoholic drinks missing a tax category, modifiers priced higher than their parent dish, any item with a zero or null price.

            Example:
            [{"category":"REVENUE_LEAKAGE","impact_score":8,"plain_english_summary":"House Burger is priced at ${'$'}0.99, far below cost.","suggested_action":"Raise price to at least ${'$'}14.00 to cover food cost and maintain margin."}]

            Menu Data:
            $payload
        """.trimIndent()

        val response = chatLanguageModel.generate(prompt)
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
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to decode LLM response into required JSON layout", e)
        }
    }
}
