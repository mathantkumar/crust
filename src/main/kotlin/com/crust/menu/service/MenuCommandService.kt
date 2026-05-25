package com.crust.menu.service

import com.crust.menu.domain.OutboxEvent
import com.crust.menu.domain.exception.NoFallbackMenuException
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

        val allVersions = menuVersionRepository.findAll()
        val previousPublished = allVersions
            .filter { it.status == "PUBLISHED" && it.createdAt.isBefore(version.createdAt) }
            .maxByOrNull { it.createdAt }

        if (previousPublished != null) {
            log.info("Reverted version $versionId → prior PUBLISHED version ${previousPublished.id} remains active")
        } else {
            val mostRecentDraft = allVersions
                .filter { it.status == "DRAFT" }
                .maxByOrNull { it.createdAt }
                
            if (mostRecentDraft != null) {
                mostRecentDraft.status = "PUBLISHED"
                menuVersionRepository.save(mostRecentDraft)
                log.info("Reverted version $versionId → promoted most recent DRAFT version ${mostRecentDraft.id} to PUBLISHED")
            } else {
                throw NoFallbackMenuException("Reverted version $versionId but no fallback menu version found")
            }
        }

        return true
    }
}
