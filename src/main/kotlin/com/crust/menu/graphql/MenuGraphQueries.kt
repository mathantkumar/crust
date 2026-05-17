package com.crust.menu.graphql

import com.crust.menu.repository.MenuAuditResultRepository
import com.crust.menu.repository.MenuVersionRepository
import com.crust.menu.repository.ItemSalesHourlyRepository
import com.crust.menu.service.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@DgsComponent
class MenuGraphQueries(
    private val auditResultRepository: MenuAuditResultRepository,
    private val menuVersionRepository: MenuVersionRepository,
    private val orderService: OrderService,
    private val kitchenDisplayService: KitchenDisplayService,
    private val paymentService: PaymentService,
    private val reportingService: ReportingService,
    private val itemSalesHourlyRepository: ItemSalesHourlyRepository,
    private val demandForecastService: DemandForecastService
) {
    // ─── Menu Queries ────────────────────────────────────────────────────────

    @DgsQuery
    fun getMenuRisks(@InputArgument versionId: String): List<Map<String, Any>> {
        return auditResultRepository.findByMenuVersionId(versionId).map {
            mapOf(
                "id" to it.id.toString(),
                "category" to (it.category ?: "PRICING_STRATEGY"),
                "impactScore" to (it.impactScore ?: it.severityScore),
                "plainEnglishSummary" to (it.plainEnglishSummary ?: it.riskDescription),
                "suggestedAction" to (it.suggestedAction ?: "Review this item manually.")
            )
        }
    }

    @DgsQuery
    fun getActiveMenu(): Map<String, Any>? {
        val version = menuVersionRepository
            .findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElse(null) ?: return null

        return mapOf(
            "id" to version.id.toString(),
            "status" to version.status,
            "createdAt" to version.createdAt.toString(),
            "categories" to version.categories.map { cat ->
                mapOf(
                    "id" to cat.id.toString(),
                    "name" to cat.name,
                    "menuItems" to cat.menuItems.map { item ->
                        mapOf(
                            "id" to item.id.toString(),
                            "name" to item.name,
                            "basePrice" to item.basePrice,
                            "available" to item.available,
                            "quantityRemaining" to item.quantityRemaining,
                            "modifierGroups" to item.modifierGroups.map { group ->
                                mapOf(
                                    "id" to group.id.toString(),
                                    "name" to group.name,
                                    "modifiers" to group.modifiers.map { mod ->
                                        mapOf(
                                            "id" to mod.id.toString(),
                                            "name" to mod.name,
                                            "priceAdjustment" to mod.priceAdjustment
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    // ─── Order Queries ───────────────────────────────────────────────────────

    @DgsQuery
    fun getOrder(@InputArgument id: String): Map<String, Any>? {
        val order = orderService.getOrder(UUID.fromString(id)) ?: return null
        return orderToMap(order)
    }

    @DgsQuery
    fun getActiveOrders(): List<Map<String, Any>> {
        return orderService.getActiveOrders().map { orderToMap(it) }
    }

    @DgsQuery
    fun getOrdersByStatus(@InputArgument status: String): List<Map<String, Any>> {
        return orderService.getOrdersByStatus(status).map { orderToMap(it) }
    }

    // ─── Kitchen Queries ─────────────────────────────────────────────────────

    @DgsQuery
    fun getKitchenStations(): List<Map<String, Any>> {
        return kitchenDisplayService.getStations().map {
            mapOf("id" to it.id.toString(), "name" to it.name, "displayOrder" to it.displayOrder, "isActive" to it.isActive)
        }
    }

    @DgsQuery
    fun getKitchenTickets(@InputArgument stationId: String?): List<Map<String, Any>> {
        val tickets = if (stationId != null)
            kitchenDisplayService.getTicketsByStation(UUID.fromString(stationId))
        else
            kitchenDisplayService.getOpenTickets()

        return tickets.map { t ->
            val elapsed = ChronoUnit.SECONDS.between(t.createdAt, java.time.LocalDateTime.now())
            mapOf(
                "id" to t.id.toString(), "orderId" to t.orderId.toString(),
                "orderNumber" to (t.orderNumber ?: 0), "stationId" to t.stationId.toString(),
                "stationName" to (t.stationName ?: ""), "status" to t.status,
                "items" to (t.items ?: "[]"), "createdAt" to t.createdAt.toString(),
                "acknowledgedAt" to (t.acknowledgedAt?.toString() ?: ""),
                "completedAt" to (t.completedAt?.toString() ?: ""),
                "elapsedSeconds" to elapsed
            )
        }
    }

    // ─── Payment Queries ─────────────────────────────────────────────────────

    @DgsQuery
    fun getPaymentsForOrder(@InputArgument orderId: String): List<Map<String, Any>> {
        return paymentService.getPaymentsForOrder(UUID.fromString(orderId)).map { paymentToMap(it) }
    }

    // ─── Reporting Queries ───────────────────────────────────────────────────

    @DgsQuery
    fun salesSummary(@InputArgument dateFrom: String, @InputArgument dateTo: String): Map<String, Any> {
        return reportingService.getSalesSummary(LocalDate.parse(dateFrom), LocalDate.parse(dateTo))
    }

    @DgsQuery
    fun productMix(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<Map<String, Any>> {
        return reportingService.getProductMix(LocalDate.parse(dateFrom), LocalDate.parse(dateTo))
    }

    @DgsQuery
    fun auditTrends(): List<Map<String, Any>> {
        return reportingService.getAuditTrends()
    }

    @DgsQuery
    fun modifierRevenue(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<Map<String, Any>> {
        return reportingService.getModifierRevenue(LocalDate.parse(dateFrom), LocalDate.parse(dateTo))
    }

    @DgsQuery
    fun itemSalesHourly(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<Map<String, Any>> {
        val from = LocalDate.parse(dateFrom)
        val to = LocalDate.parse(dateTo)
        return itemSalesHourlyRepository.findBySalesDateBetween(from, to).map { s ->
            mapOf(
                "id" to s.id.toString(),
                "restaurantId" to (s.restaurantId?.toString() ?: ""),
                "locationId" to (s.locationId?.toString() ?: ""),
                "menuItemId" to s.menuItemId.toString(),
                "itemName" to s.itemName,
                "categoryName" to (s.categoryName ?: ""),
                "salesDate" to s.salesDate.toString(),
                "hourOfDay" to s.hourOfDay,
                "dayOfWeek" to s.dayOfWeek,
                "quantitySold" to s.quantitySold,
                "grossRevenue" to s.grossRevenue,
                "modifierRevenue" to s.modifierRevenue,
                "orderCount" to s.orderCount,
                "avgUnitPrice" to s.avgUnitPrice
            )
        }
    }

    // ─── Forecast Queries ─────────────────────────────────────────────────────

    @DgsQuery
    fun demandForecast(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<Map<String, Any>> {
        return demandForecastService.getForecast(LocalDate.parse(dateFrom), LocalDate.parse(dateTo))
            .map { forecastToMap(it) }
    }

    @DgsQuery
    fun itemForecast(@InputArgument menuItemId: String): List<Map<String, Any>> {
        return demandForecastService.getForecastForItem(UUID.fromString(menuItemId))
            .map { forecastToMap(it) }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun forecastToMap(f: com.crust.menu.domain.ItemDemandForecast): Map<String, Any> = mapOf(
        "id" to f.id.toString(),
        "menuItemId" to f.menuItemId.toString(),
        "itemName" to f.itemName,
        "forecastDate" to f.forecastDate.toString(),
        "hourOfDay" to f.hourOfDay,
        "dayOfWeek" to f.dayOfWeek,
        "predictedQuantity" to f.predictedQuantity,
        "predictedRevenue" to f.predictedRevenue,
        "confidence" to f.confidence,
        "modelVersion" to f.modelVersion,
        "generatedAt" to f.generatedAt.toString()
    )

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

    private fun paymentToMap(p: com.crust.menu.domain.Payment): Map<String, Any> = mapOf(
        "id" to p.id.toString(), "orderId" to p.orderId.toString(),
        "amount" to p.amount, "tipAmount" to p.tipAmount, "totalCharged" to p.totalCharged,
        "status" to p.status, "paymentMethod" to (p.paymentMethod ?: ""),
        "transactionRef" to (p.transactionRef ?: ""), "failureReason" to (p.failureReason ?: ""),
        "createdAt" to p.createdAt.toString()
    )
}
