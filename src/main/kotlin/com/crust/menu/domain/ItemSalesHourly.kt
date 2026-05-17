package com.crust.menu.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Pre-aggregated hourly item sales — one row per item/location/hour.
 * Built by the scheduled SalesAggregationService from completed orders.
 * This is the training dataset for demand forecasting and product-mix analysis.
 */
@Entity
@Table(name = "item_sales_hourly")
class ItemSalesHourly(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id")
    val restaurantId: UUID? = null,

    @Column(name = "location_id")
    val locationId: UUID? = null,

    @Column(name = "menu_item_id", nullable = false)
    val menuItemId: UUID,

    @Column(name = "item_name", nullable = false)
    val itemName: String,

    @Column(name = "category_name")
    val categoryName: String? = null,

    @Column(name = "sales_date", nullable = false)
    val salesDate: LocalDate,

    @Column(name = "hour_of_day", nullable = false)
    val hourOfDay: Int,

    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int,

    @Column(name = "quantity_sold", nullable = false)
    var quantitySold: Int = 0,

    @Column(name = "gross_revenue", precision = 12, scale = 2, nullable = false)
    var grossRevenue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "modifier_revenue", precision = 12, scale = 2, nullable = false)
    var modifierRevenue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "order_count", nullable = false)
    var orderCount: Int = 0,

    @Column(name = "avg_unit_price", precision = 10, scale = 2, nullable = false)
    var avgUnitPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
