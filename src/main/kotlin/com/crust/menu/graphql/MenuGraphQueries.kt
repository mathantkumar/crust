package com.crust.menu.graphql

import com.crust.menu.repository.MenuAuditResultRepository
import com.crust.menu.repository.MenuVersionRepository
import com.crust.menu.repository.ItemSalesHourlyRepository
import com.crust.menu.repository.Predictive86AlertRepository
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
    private val demandForecastService: DemandForecastService,
    private val predictive86AlertRepository: Predictive86AlertRepository
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
    fun getActiveMenu(): com.crust.menu.domain.MenuVersion? {
        val version = menuVersionRepository
            .findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElse(null) ?: return null

        return version
    }

    // ─── Order Queries ───────────────────────────────────────────────────────

    @DgsQuery
    fun getOrder(@InputArgument id: String): com.crust.menu.domain.RestaurantOrder? {
        val order = orderService.getOrder(UUID.fromString(id)) ?: return null
        return order
    }

    @DgsQuery
    fun getActiveOrders(): List<com.crust.menu.domain.RestaurantOrder> {
        return orderService.getActiveOrders()
    }

    @DgsQuery
    fun getOrdersByStatus(@InputArgument status: String): List<com.crust.menu.domain.RestaurantOrder> {
        return orderService.getOrdersByStatus(status)
    }

    // ─── Kitchen Queries ─────────────────────────────────────────────────────

    @DgsQuery
    fun getKitchenStations(): List<com.crust.menu.domain.KitchenStation> {
        return kitchenDisplayService.getStations()
    }

    @DgsQuery
    fun getKitchenTickets(@InputArgument stationId: String?): List<com.crust.menu.domain.KitchenTicket> {
        return if (stationId != null)
            kitchenDisplayService.getTicketsByStation(UUID.fromString(stationId))
        else
            kitchenDisplayService.getOpenTickets()
    }

    // ─── Payment Queries ─────────────────────────────────────────────────────

    @DgsQuery
    fun getPaymentsForOrder(@InputArgument orderId: String): List<com.crust.menu.domain.Payment> {
        return paymentService.getPaymentsForOrder(UUID.fromString(orderId))
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
    fun itemSalesHourly(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<com.crust.menu.domain.ItemSalesHourly> {
        val from = LocalDate.parse(dateFrom)
        val to = LocalDate.parse(dateTo)
        return itemSalesHourlyRepository.findBySalesDateBetween(from, to)
    }

    // ─── Forecast Queries ─────────────────────────────────────────────────────

    @DgsQuery
    fun demandForecast(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<com.crust.menu.domain.ItemDemandForecast> {
        return demandForecastService.getForecast(LocalDate.parse(dateFrom), LocalDate.parse(dateTo))
            
    }

    @DgsQuery
    fun itemForecast(@InputArgument menuItemId: String): List<com.crust.menu.domain.ItemDemandForecast> {
        return demandForecastService.getForecastForItem(UUID.fromString(menuItemId))
    }

    @DgsQuery
    fun predictive86Alerts(@InputArgument status: String?): List<com.crust.menu.domain.Predictive86Alert> {
        val alerts = if (status != null) {
            predictive86AlertRepository.findByStatusOrderByCreatedAtDesc(status)
        } else {
            predictive86AlertRepository.findAll().sortedByDescending { it.createdAt }
        }
        return alerts
    }

}