package com.crust.menu.service

import com.crust.menu.repository.OutboxEventRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OutboxRelay(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    @Scheduled(fixedDelay = 5000)
    fun processOutbox() {
        val pendingEvents = outboxEventRepository.findByStatus("PENDING")
        for (event in pendingEvents) {
            // Blocking get to simulate "at-least-once" delivery ensuring we only mark PROCESSED upon ACK.
            kafkaTemplate.send("menu.version.published", event.aggregateId, event.payload).get()
            
            event.status = "PROCESSED"
            outboxEventRepository.save(event)
        }
    }
}
