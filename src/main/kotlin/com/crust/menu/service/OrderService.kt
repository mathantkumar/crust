package com.crust.menu.service

import com.crust.menu.domain.*
import com.crust.menu.domain.enums.OrderStatus
import com.crust.menu.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

data class ModifierSelectionInput(
    val modifierId: String? = null,
    val modifierName: String,
    val priceImpact: Double = 0.0
)

data class CreateOrderInput(
    val channel: String,
    val restaurantId: String? = null,
    val locationId: String? = null,
    val tableNumber: String? = null,
    val serverName: String? = null,
    val guestCount: Int? = null,
    val idempotencyKey: String? = null,
    val notes: String? = null,
    val items: List<CreateOrderItemInput>
)

data class CreateOrderItemInput(
    val menuItemId: String,
    val quantity: Int = 1,
    val modifierSelections: String? = null,
    val modifiers: List<ModifierSelectionInput>? = null,
    val specialInstructions: String? = null
)

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val menuVersionRepository: MenuVersionRepository,
    private val kitchenDisplayService: KitchenDisplayService,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)
    private val TAX_RATE = BigDecimal("0.08875") // NYC tax rate as example

    @Transactional
    fun createOrder(input: CreateOrderInput): RestaurantOrder {
        // Idempotency check — return existing order if key already used
        if (input.idempotencyKey != null) {
            val existing = orderRepository.findByIdempotencyKey(input.idempotencyKey)
            if (existing.isPresent) {
                log.info("Idempotency key ${input.idempotencyKey} already exists, returning existing order")
                return existing.get()
            }
        }

        // Resolve menu items from active menu
        val activeMenu = menuVersionRepository.findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElseThrow { IllegalStateException("No active menu found — cannot create order") }

        val allCategories = activeMenu.categories
        val allItems = allCategories.flatMap { it.menuItems }

        val order = RestaurantOrder(
            channel = input.channel,
            restaurantId = input.restaurantId?.let { UUID.fromString(it) },
            locationId = input.locationId?.let { UUID.fromString(it) },
            tableNumber = input.tableNumber,
            serverName = input.serverName,
            guestCount = input.guestCount,
            idempotencyKey = input.idempotencyKey,
            notes = input.notes
        )

        var subtotal = BigDecimal.ZERO

        for (itemInput in input.items) {
            val menuItem = allItems.find { it.id.toString() == itemInput.menuItemId }
                ?: throw IllegalArgumentException("Menu item ${itemInput.menuItemId} not found in active menu")

            // Snapshot category context
            val category = allCategories.find { cat -> cat.menuItems.any { it.id == menuItem.id } }

            val unitPrice = menuItem.basePrice ?: BigDecimal.ZERO

            // Resolve modifiers — accept both structured input and legacy JSON
            val resolvedModifiers = resolveModifiers(itemInput)
            val modifierTotal = resolvedModifiers.fold(BigDecimal.ZERO) { acc, m ->
                acc.add(BigDecimal(m.priceImpact.toString()))
            }

            val effectiveUnitPrice = unitPrice.add(modifierTotal)
            val lineTotal = effectiveUnitPrice.multiply(BigDecimal(itemInput.quantity))

            val orderItem = OrderItem(
                order = order,
                menuItemId = menuItem.id,
                menuItemName = menuItem.name,
                quantity = itemInput.quantity,
                unitPrice = unitPrice,
                lineTotal = lineTotal,
                modifierSelections = itemInput.modifierSelections,
                specialInstructions = itemInput.specialInstructions,
                categoryId = category?.id,
                categoryName = category?.name,
                menuVersionId = activeMenu.id
            )

            // Attach structured modifier rows
            for (mod in resolvedModifiers) {
                val modEntity = OrderItemModifier(
                    orderItem = orderItem,
                    modifierId = mod.modifierId?.let { UUID.fromString(it) },
                    modifierName = mod.modifierName,
                    priceImpact = BigDecimal(mod.priceImpact.toString())
                        .setScale(2, RoundingMode.HALF_UP)
                )
                orderItem.modifiers.add(modEntity)
            }

            order.items.add(orderItem)
            subtotal = subtotal.add(lineTotal)
        }

        order.subtotal = subtotal
        order.tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP)
        order.total = order.subtotal.add(order.tax)

        val saved = orderRepository.save(order)
        log.info("Created order ${saved.id} (${saved.channel}) with ${saved.items.size} items, total: $${saved.total}")

        // Publish order event
        val event = OutboxEvent(
            aggregateType = "Order",
            aggregateId = saved.id.toString(),
            payload = objectMapper.writeValueAsString(mapOf(
                "orderId" to saved.id.toString(),
                "status" to saved.status,
                "channel" to saved.channel,
                "restaurantId" to saved.restaurantId?.toString(),
                "locationId" to saved.locationId?.toString(),
                "total" to saved.total
            ))
        )
        outboxEventRepository.save(event)

        return saved
    }

    @Transactional
    fun updateOrderStatus(orderId: UUID, newStatusStr: String): RestaurantOrder {
        val order = orderRepository.findById(orderId).orElseThrow {
            IllegalArgumentException("Order $orderId not found")
        }

        val currentStatus = OrderStatus.valueOf(order.status)
        val newStatus = OrderStatus.valueOf(newStatusStr)

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw IllegalStateException("Cannot transition order from $currentStatus to $newStatus")
        }

        order.status = newStatus.name
        order.updatedAt = LocalDateTime.now()
        val saved = orderRepository.save(order)

        // When order is sent to kitchen, create KDS tickets
        if (newStatus == OrderStatus.SENT_TO_KITCHEN) {
            kitchenDisplayService.createTicketsForOrder(saved)
        }

        log.info("Order ${order.id} status updated: $currentStatus → $newStatus")
        return saved
    }

    @Transactional
    fun cancelOrder(orderId: UUID): RestaurantOrder {
        return updateOrderStatus(orderId, "CANCELLED")
    }

    fun getOrder(orderId: UUID): RestaurantOrder? =
        orderRepository.findById(orderId).orElse(null)

    fun getActiveOrders(): List<RestaurantOrder> =
        orderRepository.findActiveOrders()

    fun getOrdersByStatus(status: String): List<RestaurantOrder> =
        orderRepository.findByStatus(status)

    // ─── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Resolves modifier selections from either the new structured input
     * or the legacy JSON string (backward compatible).
     */
    private fun resolveModifiers(itemInput: CreateOrderItemInput): List<ModifierSelectionInput> {
        // Prefer structured input if provided
        if (!itemInput.modifiers.isNullOrEmpty()) {
            return itemInput.modifiers
        }

        // Fall back to parsing legacy modifierSelections JSON
        if (!itemInput.modifierSelections.isNullOrBlank()) {
            return try {
                objectMapper.readValue<List<Map<String, Any>>>(itemInput.modifierSelections).map { m ->
                    ModifierSelectionInput(
                        modifierId = m["modifierId"]?.toString() ?: m["id"]?.toString(),
                        modifierName = m["name"]?.toString() ?: m["modifierName"]?.toString() ?: "Unknown",
                        priceImpact = (m["priceImpact"] ?: m["priceAdjustment"] ?: m["price"] ?: 0.0)
                            .let { (it as? Number)?.toDouble() ?: 0.0 }
                    )
                }
            } catch (e: Exception) {
                log.warn("Could not parse modifierSelections JSON, treating as empty: ${e.message}")
                emptyList()
            }
        }

        return emptyList()
    }
}
