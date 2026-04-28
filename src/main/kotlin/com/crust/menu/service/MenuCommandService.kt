package com.crust.menu.service

import com.crust.menu.domain.OutboxEvent
import com.crust.menu.repository.MenuVersionRepository
import com.crust.menu.repository.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuCommandService(
    private val menuVersionRepository: MenuVersionRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {
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
        return true
    }

    @Transactional
    fun revertMenuVersion(versionId: UUID): Boolean {
        val version = menuVersionRepository.findById(versionId).orElseThrow {
            IllegalArgumentException("MenuVersion with ID $versionId not found")
        }
        
        version.status = "REVERTED_DUE_TO_RISK"
        menuVersionRepository.save(version)
        // A full production app would also find the LAST clean version and mark it PUBLISHED here!
        return true
    }
}
