package com.crust.menu.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "restaurant_order")
class RestaurantOrder(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "order_number")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var orderNumber: Long? = null,

    @Column(name = "channel", nullable = false)
    var channel: String,

    @Column(name = "status", nullable = false)
    var status: String = "CREATED",

    @Column(name = "table_number")
    var tableNumber: String? = null,

    @Column(name = "server_name")
    var serverName: String? = null,

    @Column(name = "guest_count")
    var guestCount: Int? = null,

    @Column(name = "subtotal", precision = 10, scale = 2)
    var subtotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tax", precision = 10, scale = 2)
    var tax: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tip", precision = 10, scale = 2)
    var tip: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total", precision = 10, scale = 2)
    var total: BigDecimal = BigDecimal.ZERO,

    @Column(name = "restaurant_id")
    var restaurantId: java.util.UUID? = null,

    @Column(name = "location_id")
    var locationId: java.util.UUID? = null,

    @Column(name = "idempotency_key", unique = true)
    var idempotencyKey: String? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "aggregated", nullable = false)
    var aggregated: Boolean = false,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf()
)

@Entity
@Table(name = "order_item")
class OrderItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: RestaurantOrder,

    @Column(name = "menu_item_id", nullable = false)
    val menuItemId: UUID,

    @Column(name = "menu_item_name", nullable = false)
    var menuItemName: String,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1,

    @Column(name = "unit_price", precision = 10, scale = 2)
    var unitPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_total", precision = 10, scale = 2)
    var lineTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "modifier_selections", columnDefinition = "jsonb")
    var modifierSelections: String? = null,

    @Column(name = "special_instructions")
    var specialInstructions: String? = null,

    @Column(name = "status", nullable = false)
    var status: String = "PENDING",

    @Column(name = "station_routing")
    var stationRouting: String? = null,

    // ── Analytics snapshot fields ────────────────────────────────────────────

    @Column(name = "category_id")
    val categoryId: UUID? = null,

    @Column(name = "category_name")
    val categoryName: String? = null,

    @Column(name = "menu_version_id")
    val menuVersionId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "orderItem", cascade = [CascadeType.ALL], orphanRemoval = true)
    val modifiers: MutableList<OrderItemModifier> = mutableListOf()
)

@Entity
@Table(name = "order_item_modifier")
class OrderItemModifier(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    val orderItem: OrderItem,

    @Column(name = "modifier_id")
    val modifierId: UUID? = null,

    @Column(name = "modifier_name", nullable = false)
    val modifierName: String,

    @Column(name = "price_impact", precision = 10, scale = 2, nullable = false)
    val priceImpact: java.math.BigDecimal = java.math.BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
