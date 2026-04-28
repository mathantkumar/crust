package com.crust.menu.service

import com.crust.menu.domain.MenuAuditResult
import com.crust.menu.repository.MenuAuditResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.model.chat.ChatLanguageModel
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.retry.annotation.Backoff
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
            You are a Restaurant Revenue Specialist. Analyze this menu for potential revenue leakage or pricing anomalies (e.g., items priced under ${'$'}1, alcoholic drinks without tax categories, or modifiers that are more expensive than the main dish). 
            Return a JSON list of objects strictly following this format, where each object has a 'riskDescription' string and a 'severityScore' integer (1-10):
            [{"riskDescription": "...", "severityScore": 5}]
            
            Menu Data:
            $payload
        """.trimIndent()

        val response = chatLanguageModel.generate(prompt)
        val replyText = response.replace("```json", "").replace("```", "").trim()

        try {
            val risksNode = objectMapper.readTree(replyText)
            if (risksNode.isArray) {
                for (riskNode in risksNode) {
                    val description = riskNode.path("riskDescription").asText()
                    val score = riskNode.path("severityScore").asInt()
                    
                    val result = MenuAuditResult(
                        menuVersionId = versionId,
                        riskDescription = description,
                        severityScore = score
                    )
                    menuAuditResultRepository.save(result)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to decode LLM response into required JSON layout", e)
        }
    }
}
