package com.crust.menu.service

import com.crust.menu.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboxRelay(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(OutboxRelay::class.java)

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun processOutbox() {
        val pendingEvents = outboxEventRepository.findByStatus("PENDING")
        if (pendingEvents.isEmpty()) return

        for (event in pendingEvents) {
            try {
                val topic = when (event.aggregateType) {
                    "MenuVersion" -> "menu.version.published"
                    "Order" -> "order.created"
                    else -> "system.events"
                }

                // Non-blocking send, but database state only updates upon broker confirmation
                kafkaTemplate.send(topic, event.aggregateId, event.payload)
                    .whenComplete { result, ex ->
                        if (ex != null) {
                            log.error("Failed to send outbox event ${event.id} to Kafka: ${ex.message}", ex)
                            event.status = "FAILED"
                            outboxEventRepository.save(event)
                        } else {
                            log.info("Outbox event ${event.id} delivered to $topic (partition ${result.recordMetadata.partition()})")
                            event.status = "PROCESSED"
                            outboxEventRepository.save(event)
                        }
                    }
            } catch (e: Exception) {
                log.error("Failed to enqueue outbox event ${event.id}: ${e.message}", e)
                event.status = "FAILED"
                outboxEventRepository.save(event)
            }
        }
        log.info("Processed ${pendingEvents.size} outbox events")
    }
}
