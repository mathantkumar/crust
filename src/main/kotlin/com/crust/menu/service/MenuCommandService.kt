package com.crust.menu.service

import com.crust.menu.domain.OutboxEvent
import com.crust.menu.repository.MenuVersionRepository
import com.crust.menu.repository.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuCommandService(
    private val menuVersionRepository: MenuVersionRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(MenuCommandService::class.java)

    @Transactional
    fun publishMenuVersion(versionId: UUID): Boolean {
        val version = menuVersionRepository.findById(versionId).orElseThrow {
            IllegalArgumentException("MenuVersion with ID $versionId not found")
        }
        
        version.status = "PUBLISHED"
        menuVersionRepository.save(version)

        // Generating a safe event payload to avoid Jackson LazyInitializationException against the full entity graph.
        val eventPayload = mapOf(
            "id" to version.id.toString(),
            "status" to version.status,
            "correlationId" to version.correlationId
        )

        val event = OutboxEvent(
            aggregateType = "MenuVersion",
            aggregateId = versionId.toString(),
            payload = objectMapper.writeValueAsString(eventPayload),
            status = "PENDING"
        )
        
        outboxEventRepository.save(event)
        log.info("Published menu version $versionId and queued outbox event ${event.id}")
        return true
    }

    @Transactional
    fun revertMenuVersion(versionId: UUID): Boolean {
        val version = menuVersionRepository.findById(versionId).orElseThrow {
            IllegalArgumentException("MenuVersion with ID $versionId not found")
        }
        
        version.status = "REVERTED_DUE_TO_RISK"
        menuVersionRepository.save(version)

        // Atomically promote the previous PUBLISHED version so getActiveMenu never returns null.
        // Look for the most recent version that was PUBLISHED before this one.
        val previousVersion = menuVersionRepository
            .findFirstByStatusAndIdNotOrderByCreatedAtDesc("PUBLISHED", versionId)
            .orElseGet {
                // If no other PUBLISHED version exists, look for most recent DRAFT
                menuVersionRepository
                    .findFirstByStatusAndIdNotOrderByCreatedAtDesc("DRAFT", versionId)
                    .orElse(null)
            }

        if (previousVersion != null) {
            previousVersion.status = "PUBLISHED"
            menuVersionRepository.save(previousVersion)
            log.info("Reverted version $versionId → promoted version ${previousVersion.id} to PUBLISHED")
        } else {
            log.warn("Reverted version $versionId but no previous version found to promote — getActiveMenu will return null!")
        }

        return true
    }
}
