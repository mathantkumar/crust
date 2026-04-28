package com.crust.menu.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "menu_audit_result")
class MenuAuditResult(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "menu_version_id", nullable = false)
    val menuVersionId: String,

    @Column(name = "risk_description", nullable = false, columnDefinition = "TEXT")
    val riskDescription: String,

    @Column(name = "severity_score", nullable = false)
    val severityScore: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
