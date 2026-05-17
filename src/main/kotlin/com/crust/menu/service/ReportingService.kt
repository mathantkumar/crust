package com.crust.menu.service

import com.crust.menu.repository.OrderRepository
import com.crust.menu.repository.PaymentRepository
import com.crust.menu.repository.MenuAuditResultRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ReportingService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val auditResultRepository: MenuAuditResultRepository
) {

    fun getSalesSummary(dateFrom: LocalDate, dateTo: LocalDate): Map<String, Any> {
        val since = dateFrom.atStartOfDay()
        val orders = orderRepository.findOrdersSince(since)
            .filter { it.createdAt.toLocalDate() <= dateTo }

        val completedOrders = orders.filter { it.status == "COMPLETED" }
        val totalRevenue = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.total) }
        val totalTax = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.tax) }
        val totalTips = completedOrders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.tip) }
        val avgOrderValue = if (completedOrders.isNotEmpty())
            totalRevenue.divide(BigDecimal(completedOrders.size), 2, java.math.RoundingMode.HALF_UP)
        else BigDecimal.ZERO

        val byChannel = completedOrders.groupBy { it.channel }.mapValues { (_, orders) ->
            mapOf(
                "count" to orders.size,
                "revenue" to orders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.total) }
            )
        }

        val byHour = completedOrders.groupBy { it.createdAt.hour }.mapValues { (_, orders) ->
            mapOf("count" to orders.size, "revenue" to orders.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.total) })
        }

        return mapOf(
            "dateRange" to mapOf("from" to dateFrom.toString(), "to" to dateTo.toString()),
            "totalOrders" to orders.size,
            "completedOrders" to completedOrders.size,
            "cancelledOrders" to orders.count { it.status == "CANCELLED" },
            "totalRevenue" to totalRevenue,
            "totalTax" to totalTax,
            "totalTips" to totalTips,
            "averageOrderValue" to avgOrderValue,
            "revenueByChannel" to byChannel,
            "revenueByHour" to byHour
        )
    }

    fun getProductMix(dateFrom: LocalDate, dateTo: LocalDate): List<Map<String, Any>> {
        val since = dateFrom.atStartOfDay()
        val orders = orderRepository.findOrdersSince(since)
            .filter { it.status == "COMPLETED" && it.createdAt.toLocalDate() <= dateTo }

        val allItems = orders.flatMap { it.items }

        // Group by category → item for richer analytics
        return allItems.groupBy { it.menuItemName }.map { (name, items) ->
            val totalQty = items.sumOf { it.quantity }
            val totalRev = items.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.lineTotal) }
            val modifierRev = items.flatMap { it.modifiers }
                .fold(BigDecimal.ZERO) { acc, m -> acc.add(m.priceImpact) }
            val categoryName = items.firstOrNull()?.categoryName ?: "Uncategorized"

            mapOf(
                "itemName" to name,
                "categoryName" to categoryName,
                "totalQuantity" to totalQty,
                "totalRevenue" to totalRev,
                "modifierRevenue" to modifierRev,
                "orderCount" to items.map { it.order.id }.distinct().size
            )
        }.sortedByDescending { (it["totalRevenue"] as BigDecimal) }
    }

    /**
     * Returns modifier revenue contribution across all completed orders.
     * Useful for understanding which modifiers drive upsell revenue.
     */
    fun getModifierRevenue(dateFrom: LocalDate, dateTo: LocalDate): List<Map<String, Any>> {
        val since = dateFrom.atStartOfDay()
        val orders = orderRepository.findOrdersSince(since)
            .filter { it.status == "COMPLETED" && it.createdAt.toLocalDate() <= dateTo }

        val allModifiers = orders.flatMap { it.items }.flatMap { it.modifiers }

        return allModifiers.groupBy { it.modifierName }.map { (name, mods) ->
            val totalImpact = mods.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.priceImpact) }
            mapOf(
                "modifierName" to name,
                "selectionCount" to mods.size,
                "totalRevenue" to totalImpact,
                "averageImpact" to if (mods.isNotEmpty())
                    totalImpact.divide(BigDecimal(mods.size), 2, java.math.RoundingMode.HALF_UP)
                else BigDecimal.ZERO
            )
        }.sortedByDescending { (it["totalRevenue"] as BigDecimal) }
    }

    fun getAuditTrends(): List<Map<String, Any>> {
        val results = auditResultRepository.findAll()
        return results.groupBy { it.category ?: "UNCATEGORIZED" }.map { (cat, risks) ->
            mapOf(
                "category" to cat,
                "count" to risks.size,
                "averageSeverity" to if (risks.isNotEmpty())
                    risks.map { it.severityScore }.average() else 0.0,
                "latestRisk" to (risks.maxByOrNull { it.createdAt }?.plainEnglishSummary ?: "")
            )
        }
    }
}
