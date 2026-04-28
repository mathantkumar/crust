package com.crust.menu.repository

import com.crust.menu.domain.MenuVersion
import com.crust.menu.domain.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MenuVersionRepository : JpaRepository<MenuVersion, UUID>

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    fun findByStatus(status: String): List<OutboxEvent>
}

interface MenuAuditResultRepository : JpaRepository<com.crust.menu.domain.MenuAuditResult, UUID> {
    fun findByMenuVersionId(menuVersionId: String): List<com.crust.menu.domain.MenuAuditResult>
}

