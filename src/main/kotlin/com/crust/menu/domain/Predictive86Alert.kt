package com.crust.menu.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Audit trail and action queue for Intelligent 86ing.
 * Fired by the XGBoost Inference Simulator when predicted demand > current stock.
 */
@Entity
@Table(name = "predictive_86_alert")
class Predictive86Alert(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "menu_item_id", nullable = false)
    val menuItemId: UUID,

    @Column(name = "item_name", nullable = false)
    val itemName: String,

    @Column(name = "trigger_order_id", nullable = false)
    val triggerOrderId: UUID,

    @Column(name = "current_stock", nullable = false)
    val currentStock: Int,

    @Column(name = "predicted_demand", precision = 10, scale = 2, nullable = false)
    val predictedDemand: BigDecimal,

    @Column(name = "risk_score", precision = 10, scale = 2, nullable = false)
    val riskScore: BigDecimal,

    @Column(name = "alert_message", nullable = false)
    val alertMessage: String,

    @Column(name = "status", nullable = false)
    var status: String = "UNRESOLVED",

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null
)
