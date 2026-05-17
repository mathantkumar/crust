package com.crust.menu.service

import com.crust.menu.domain.ItemSalesHourly
import com.crust.menu.domain.OrderItem
import com.crust.menu.domain.RestaurantOrder
import com.crust.menu.repository.ItemSalesHourlyRepository
import com.crust.menu.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Scheduled job that rolls completed orders into pre-aggregated
 * hourly item-sales rows. This becomes the training dataset for
 * demand forecasting, product-mix analysis, and revenue intelligence.
 *
 * Runs every 60 seconds. Each completed order is processed exactly once
 * (guarded by the `aggregated` flag on restaurant_order).
 *
 * For each order item, it finds-or-creates the matching
 * `item_sales_hourly` row (keyed on restaurant + location + item + date + hour)
 * and increments the counters.
 */
@Service
class SalesAggregationService(
    private val orderRepository: OrderRepository,
    private val salesHourlyRepository: ItemSalesHourlyRepository
) {
    private val log = LoggerFactory.getLogger(SalesAggregationService::class.java)

    /**
     * Poll for completed, un-aggregated orders and roll them up.
     * Fixed delay of 60s ensures the previous run finishes before the next starts.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    fun rollUpCompletedOrders() {
        val orders = orderRepository.findCompletedUnaggregated()
        if (orders.isEmpty()) return

        log.info("Sales aggregation: processing ${orders.size} completed orders")

        var rowsUpserted = 0
        for (order in orders) {
            for (item in order.items) {
                upsertHourlyRow(order, item)
                rowsUpserted++
            }
            // Mark as aggregated so we never double-count
            order.aggregated = true
            orderRepository.save(order)
        }

        log.info("Sales aggregation complete: $rowsUpserted item rows rolled up from ${orders.size} orders")
    }

    /**
     * Find-or-create the hourly aggregate row for this item/location/hour
     * and increment its counters.
     */
    private fun upsertHourlyRow(order: RestaurantOrder, item: OrderItem) {
        val orderDate = order.createdAt.toLocalDate()
        val orderHour = order.createdAt.hour
        val dayOfWeek = order.createdAt.dayOfWeek.value // 1=Mon, 7=Sun

        val existing = salesHourlyRepository.findByCompositeKey(
            restaurantId = order.restaurantId,
            locationId = order.locationId,
            menuItemId = item.menuItemId,
            salesDate = orderDate,
            hourOfDay = orderHour
        )

        if (existing.isPresent) {
            // Increment existing row
            val row = existing.get()
            row.quantitySold += item.quantity
            row.grossRevenue = row.grossRevenue.add(item.lineTotal)
            row.modifierRevenue = row.modifierRevenue.add(
                item.modifiers.fold(BigDecimal.ZERO) { acc, m ->
                    acc.add(m.priceImpact.multiply(BigDecimal(item.quantity)))
                }
            )
            row.orderCount += 1
            row.avgUnitPrice = if (row.quantitySold > 0)
                row.grossRevenue.divide(BigDecimal(row.quantitySold), 2, RoundingMode.HALF_UP)
            else BigDecimal.ZERO
            row.updatedAt = LocalDateTime.now()
            salesHourlyRepository.save(row)
        } else {
            // Create new row
            val modRev = item.modifiers.fold(BigDecimal.ZERO) { acc, m ->
                acc.add(m.priceImpact.multiply(BigDecimal(item.quantity)))
            }
            val row = ItemSalesHourly(
                restaurantId = order.restaurantId,
                locationId = order.locationId,
                menuItemId = item.menuItemId,
                itemName = item.menuItemName,
                categoryName = item.categoryName,
                salesDate = orderDate,
                hourOfDay = orderHour,
                dayOfWeek = dayOfWeek,
                quantitySold = item.quantity,
                grossRevenue = item.lineTotal,
                modifierRevenue = modRev,
                orderCount = 1,
                avgUnitPrice = if (item.quantity > 0)
                    item.lineTotal.divide(BigDecimal(item.quantity), 2, RoundingMode.HALF_UP)
                else BigDecimal.ZERO
            )
            salesHourlyRepository.save(row)
        }
    }
}
