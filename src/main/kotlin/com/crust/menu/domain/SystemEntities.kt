package com.crust.menu.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "kitchen_station")
class KitchenStation(
        @Id val id: UUID = UUID.randomUUID(),
        @Column(nullable = false, unique = true) var name: String,
        @Column(name = "display_order") var displayOrder: Int = 0,
        @Column(name = "is_active") var isActive: Boolean = true
)

@Entity
@Table(name = "kitchen_ticket")
class KitchenTicket(
        @Id val id: UUID = UUID.randomUUID(),
        @Column(name = "order_id", nullable = false) val orderId: UUID,
        @Column(name = "order_number") var orderNumber: Long? = null,
        @Column(name = "station_id", nullable = false) val stationId: UUID,
        @Column(name = "station_name") var stationName: String? = null,
        @Column(name = "status", nullable = false) var status: String = "NEW",
        @Column(name = "items", columnDefinition = "jsonb") var items: String? = null,
        @Column(name = "created_at", nullable = false)
        val createdAt: LocalDateTime = LocalDateTime.now(),
        @Column(name = "acknowledged_at") var acknowledgedAt: LocalDateTime? = null,
        @Column(name = "completed_at") var completedAt: LocalDateTime? = null
)

@Entity
@Table(name = "payment")
class Payment(
        @Id val id: UUID = UUID.randomUUID(),
        @Column(name = "order_id", nullable = false) val orderId: UUID,
        @Column(name = "amount", precision = 10, scale = 2, nullable = false)
        var amount: BigDecimal,
        @Column(name = "tip_amount", precision = 10, scale = 2)
        var tipAmount: BigDecimal = BigDecimal.ZERO,
        @Column(name = "total_charged", precision = 10, scale = 2)
        var totalCharged: BigDecimal = BigDecimal.ZERO,
        @Column(name = "status", nullable = false) var status: String = "INITIATED",
        @Column(name = "payment_method") var paymentMethod: String? = null,
        @Column(name = "transaction_ref") var transactionRef: String? = null,
        @Column(name = "failure_reason") var failureReason: String? = null,
        @Column(name = "created_at", nullable = false)
        val createdAt: LocalDateTime = LocalDateTime.now(),
        @Column(name = "updated_at") var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "restaurant")
class Restaurant(
        @Id val id: UUID = UUID.randomUUID(),
        @Column(nullable = false) var name: String,
        @Column(name = "timezone") var timezone: String = "America/New_York",
        @Column(name = "created_at", nullable = false)
        val createdAt: LocalDateTime = LocalDateTime.now(),
        @OneToMany(mappedBy = "restaurant", cascade = [CascadeType.ALL])
        val locations: MutableList<Location> = mutableListOf()
)

@Entity
@Table(name = "location")
class Location(
        @Id val id: UUID = UUID.randomUUID(),
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "restaurant_id", nullable = false)
        val restaurant: Restaurant,
        @Column(nullable = false) var name: String,
        @Column var address: String? = null,
        @Column(name = "is_active") var isActive: Boolean = true,
        @Column(name = "created_at", nullable = false)
        val createdAt: LocalDateTime = LocalDateTime.now()
)
