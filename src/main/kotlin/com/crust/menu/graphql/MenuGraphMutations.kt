package com.crust.menu.graphql

import com.crust.menu.service.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import java.math.BigDecimal
import java.time.temporal.ChronoUnit
import java.util.UUID

@DgsComponent
class MenuGraphMutations(
    private val menuCommandService: MenuCommandService,
    private val menuAvailabilityService: MenuAvailabilityService,
    private val orderService: OrderService,
    private val kitchenDisplayService: KitchenDisplayService,
    private val paymentService: PaymentService
) {
    // ─── Menu Mutations ──────────────────────────────────────────────────────

    @DgsMutation
    fun publishMenu(@InputArgument versionId: String): Boolean {
        return menuCommandService.publishMenuVersion(UUID.fromString(versionId))
    }

    @DgsMutation
    fun revertToCleanVersion(@InputArgument versionId: String): Boolean {
        return menuCommandService.revertMenuVersion(UUID.fromString(versionId))
    }

    @DgsMutation
    fun eightySixItem(@InputArgument itemId: String): com.crust.menu.domain.MenuItem {
        return menuAvailabilityService.eightySixItem(UUID.fromString(itemId))
    }

    @DgsMutation
    fun unEightySixItem(@InputArgument itemId: String, @InputArgument quantity: Int?): com.crust.menu.domain.MenuItem {
        return menuAvailabilityService.unEightySixItem(UUID.fromString(itemId), quantity)
    }

    // ─── Order Mutations ─────────────────────────────────────────────────────

    @DgsMutation
    fun createOrder(@InputArgument input: Map<String, Any>): com.crust.menu.domain.RestaurantOrder {
        val items = (input["items"] as List<Map<String, Any>>).map { item ->
            val modifiers = (item["modifiers"] as? List<Map<String, Any>>)?.map { m ->
                ModifierSelectionInput(
                    modifierId = m["modifierId"] as? String,
                    modifierName = m["modifierName"] as String,
                    priceImpact = (m["priceImpact"] as? Number)?.toDouble() ?: 0.0
                )
            }

            CreateOrderItemInput(
                menuItemId = item["menuItemId"] as String,
                quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                modifierSelections = item["modifierSelections"] as? String,
                modifiers = modifiers,
                specialInstructions = item["specialInstructions"] as? String
            )
        }

        val createInput = CreateOrderInput(
            channel = input["channel"] as String,
            restaurantId = input["restaurantId"] as? String,
            locationId = input["locationId"] as? String,
            tableNumber = input["tableNumber"] as? String,
            serverName = input["serverName"] as? String,
            guestCount = (input["guestCount"] as? Number)?.toInt(),
            idempotencyKey = input["idempotencyKey"] as? String,
            notes = input["notes"] as? String,
            items = items
        )

        return orderService.createOrder(createInput)
    }

    @DgsMutation
    fun updateOrderStatus(@InputArgument orderId: String, @InputArgument status: String): com.crust.menu.domain.RestaurantOrder {
        val order = orderService.updateOrderStatus(UUID.fromString(orderId), status)
        return order
    }

    @DgsMutation
    fun cancelOrder(@InputArgument orderId: String): com.crust.menu.domain.RestaurantOrder {
        return orderService.cancelOrder(UUID.fromString(orderId))
    }

    // ─── Kitchen Mutations ───────────────────────────────────────────────────

    @DgsMutation
    fun acknowledgeTicket(@InputArgument ticketId: String): com.crust.menu.domain.KitchenTicket {
        return kitchenDisplayService.acknowledgeTicket(UUID.fromString(ticketId))
    }

    @DgsMutation
    fun markTicketReady(@InputArgument ticketId: String): com.crust.menu.domain.KitchenTicket {
        return kitchenDisplayService.markReady(UUID.fromString(ticketId))
    }

    @DgsMutation
    fun bumpTicket(@InputArgument ticketId: String): com.crust.menu.domain.KitchenTicket {
        return kitchenDisplayService.bumpTicket(UUID.fromString(ticketId))
    }

    // ─── Payment Mutations ───────────────────────────────────────────────────

    @DgsMutation
    fun initiatePayment(@InputArgument input: Map<String, Any>): Map<String, Any> {
        val payment = paymentService.initiatePayment(
            orderId = UUID.fromString(input["orderId"] as String),
            amount = BigDecimal((input["amount"] as Number).toDouble()),
            tip = BigDecimal((input["tip"] as Number).toDouble()),
            method = input["method"] as String
        )
        return paymentToMap(payment)
    }

    @DgsMutation
    fun authorizePayment(@InputArgument paymentId: String): Map<String, Any> {
        return paymentToMap(paymentService.authorizePayment(UUID.fromString(paymentId)))
    }

    @DgsMutation
    fun capturePayment(@InputArgument paymentId: String): Map<String, Any> {
        return paymentToMap(paymentService.capturePayment(UUID.fromString(paymentId)))
    }

    @DgsMutation
    fun refundPayment(@InputArgument paymentId: String, @InputArgument reason: String?): Map<String, Any> {
        return paymentToMap(paymentService.refundPayment(UUID.fromString(paymentId), reason))
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun orderToMap(o: com.crust.menu.domain.RestaurantOrder): Map<String, Any> = mapOf(
        "id" to o.id.toString(), "orderNumber" to (o.orderNumber ?: 0),
        "channel" to o.channel, "status" to o.status,
        "restaurantId" to (o.restaurantId?.toString() ?: ""),
        "locationId" to (o.locationId?.toString() ?: ""),
        "tableNumber" to (o.tableNumber ?: ""), "serverName" to (o.serverName ?: ""),
        "guestCount" to (o.guestCount ?: 0),
        "subtotal" to o.subtotal, "tax" to o.tax, "tip" to o.tip, "total" to o.total,
        "notes" to (o.notes ?: ""),
        "createdAt" to o.createdAt.toString(), "updatedAt" to o.updatedAt.toString(),
        "items" to o.items.map { i ->
            mapOf(
                "id" to i.id.toString(), "menuItemId" to i.menuItemId.toString(),
                "menuItemName" to i.menuItemName, "quantity" to i.quantity,
                "unitPrice" to i.unitPrice, "lineTotal" to i.lineTotal,
                "modifierSelections" to (i.modifierSelections ?: ""),
                "specialInstructions" to (i.specialInstructions ?: ""),
                "status" to i.status,
                "categoryId" to (i.categoryId?.toString() ?: ""),
                "categoryName" to (i.categoryName ?: ""),
                "menuVersionId" to (i.menuVersionId?.toString() ?: ""),
                "createdAt" to i.createdAt.toString(),
                "completedAt" to (i.completedAt?.toString() ?: ""),
                "modifiers" to i.modifiers.map { m ->
                    mapOf(
                        "id" to m.id.toString(),
                        "modifierId" to (m.modifierId?.toString() ?: ""),
                        "modifierName" to m.modifierName,
                        "priceImpact" to m.priceImpact
                    )
                }
            )
        }
    )

    private fun ticketToMap(t: com.crust.menu.domain.KitchenTicket): Map<String, Any> {
        val elapsed = ChronoUnit.SECONDS.between(t.createdAt, java.time.LocalDateTime.now())
        return mapOf(
            "id" to t.id.toString(), "orderId" to t.orderId.toString(),
            "orderNumber" to (t.orderNumber ?: 0), "stationId" to t.stationId.toString(),
            "stationName" to (t.stationName ?: ""), "status" to t.status,
            "items" to (t.items ?: "[]"), "createdAt" to t.createdAt.toString(),
            "acknowledgedAt" to (t.acknowledgedAt?.toString() ?: ""),
            "completedAt" to (t.completedAt?.toString() ?: ""),
            "elapsedSeconds" to elapsed
        )
    }

    private fun paymentToMap(p: com.crust.menu.domain.Payment): Map<String, Any> = mapOf(
        "id" to p.id.toString(), "orderId" to p.orderId.toString(),
        "amount" to p.amount, "tipAmount" to p.tipAmount, "totalCharged" to p.totalCharged,
        "status" to p.status, "paymentMethod" to (p.paymentMethod ?: ""),
        "transactionRef" to (p.transactionRef ?: ""), "failureReason" to (p.failureReason ?: ""),
        "createdAt" to p.createdAt.toString()
    )
}
