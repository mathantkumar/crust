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
                // Non-blocking send with async callback instead of blocking .get()
                kafkaTemplate.send("menu.version.published", event.aggregateId, event.payload)
                    .whenComplete { result, ex ->
                        if (ex != null) {
                            log.error("Failed to send outbox event ${event.id} to Kafka: ${ex.message}", ex)
                        } else {
                            log.info("Outbox event ${event.id} delivered to partition ${result.recordMetadata.partition()} at offset ${result.recordMetadata.offset()}")
                        }
                    }

                // Mark as PROCESSED optimistically — if Kafka fails, the DLT/retry handles it
                event.status = "PROCESSED"
                outboxEventRepository.save(event)
            } catch (e: Exception) {
                log.error("Failed to process outbox event ${event.id}: ${e.message}", e)
                event.status = "FAILED"
                outboxEventRepository.save(event)
            }
        }
        log.info("Processed ${pendingEvents.size} outbox events")
    }
}
