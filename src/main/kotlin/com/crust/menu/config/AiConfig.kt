package com.crust.menu.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    @Value("\${LANGCHAIN4J_GOOGLE_AI_GEMINI_API_KEY:}")
    lateinit var apiKey: String

    @Bean
    fun chatLanguageModel(): ChatLanguageModel {
        if (apiKey.isBlank() || apiKey == "dummy-key-for-now") {
            return ChatLanguageModel { _ ->
                dev.langchain4j.model.output.Response.from(
                    dev.langchain4j.data.message.AiMessage("Audit skipped: No AI key configured.")
                )
            }
        }
        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gemini-1.5-flash")
            .build()
    }
}
