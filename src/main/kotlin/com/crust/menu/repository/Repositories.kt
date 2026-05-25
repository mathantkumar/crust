package com.crust.menu.repository

import com.crust.menu.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

// ─── Menu ────────────────────────────────────────────────────────────────────

interface MenuVersionRepository : JpaRepository<MenuVersion, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(
        attributePaths = [
            "categories",
            "categories.menuItems",
            "categories.menuItems.modifierGroups",
            "categories.menuItems.modifierGroups.modifiers"
        ]
    )
    fun findFirstByStatusOrderByCreatedAtDesc(status: String): Optional<MenuVersion>

    fun findFirstByStatusAndIdNotOrderByCreatedAtDesc(status: String, excludeId: UUID): Optional<MenuVersion>
}

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    fun findByStatus(status: String): List<OutboxEvent>
}

interface MenuAuditResultRepository : JpaRepository<MenuAuditResult, UUID> {
    fun findByMenuVersionId(menuVersionId: String): List<MenuAuditResult>
}

// ─── Orders ──────────────────────────────────────────────────────────────────

interface OrderRepository : JpaRepository<RestaurantOrder, UUID> {
    fun findByStatus(status: String): List<RestaurantOrder>
    fun findByChannel(channel: String): List<RestaurantOrder>
    fun findByIdempotencyKey(key: String): Optional<RestaurantOrder>
    fun findByRestaurantId(restaurantId: UUID): List<RestaurantOrder>
    fun findByLocationId(locationId: UUID): List<RestaurantOrder>

    @Query("SELECT o FROM RestaurantOrder o WHERE o.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY o.createdAt DESC")
    fun findActiveOrders(): List<RestaurantOrder>

    @Query("SELECT o FROM RestaurantOrder o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    fun findOrdersSince(since: LocalDateTime): List<RestaurantOrder>

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM RestaurantOrder o WHERE o.status = 'COMPLETED' AND o.aggregated = false ORDER BY o.createdAt ASC")
    fun findCompletedUnaggregated(): List<RestaurantOrder>
}

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {
    fun findByOrderId(orderId: UUID): List<OrderItem>
}

interface OrderItemModifierRepository : JpaRepository<OrderItemModifier, UUID> {
    fun findByOrderItemId(orderItemId: UUID): List<OrderItemModifier>
}

// ─── Analytics Aggregates ────────────────────────────────────────────────────

interface ItemSalesHourlyRepository : JpaRepository<ItemSalesHourly, UUID> {
    @Query("""
        SELECT s FROM ItemSalesHourly s
        WHERE s.restaurantId = :restaurantId
          AND s.locationId = :locationId
          AND s.menuItemId = :menuItemId
          AND s.salesDate = :salesDate
          AND s.hourOfDay = :hourOfDay
    """)
    fun findByCompositeKey(
        restaurantId: UUID?,
        locationId: UUID?,
        menuItemId: UUID,
        salesDate: java.time.LocalDate,
        hourOfDay: Int
    ): Optional<ItemSalesHourly>

    fun findBySalesDateBetween(from: java.time.LocalDate, to: java.time.LocalDate): List<ItemSalesHourly>
    fun findByMenuItemId(menuItemId: UUID): List<ItemSalesHourly>
    fun findByRestaurantId(restaurantId: UUID): List<ItemSalesHourly>
    fun findByCategoryName(categoryName: String): List<ItemSalesHourly>

    /** Training data: historical hourly sales for a specific item on a specific day-of-week */
    @Query("""
        SELECT s FROM ItemSalesHourly s
        WHERE s.restaurantId = :restaurantId
          AND s.locationId = :locationId
          AND s.menuItemId = :menuItemId
          AND s.dayOfWeek = :dayOfWeek
          AND s.salesDate >= :since
        ORDER BY s.salesDate DESC, s.hourOfDay ASC
    """)
    fun findTrainingData(
        restaurantId: UUID,
        locationId: UUID,
        menuItemId: UUID,
        dayOfWeek: Int,
        since: java.time.LocalDate
    ): List<ItemSalesHourly>

    /** All distinct tenant/item combinations that have sales history */
    @Query("SELECT DISTINCT s.restaurantId, s.locationId, s.menuItemId FROM ItemSalesHourly s")
    fun findDistinctTenantItems(): List<Array<Any>>
}

// ─── Demand Forecasting ──────────────────────────────────────────────────────

interface ItemDemandForecastRepository : JpaRepository<ItemDemandForecast, UUID> {
    fun findByForecastDateAndMenuItemId(forecastDate: java.time.LocalDate, menuItemId: UUID): List<ItemDemandForecast>
    fun findByForecastDateBetween(from: java.time.LocalDate, to: java.time.LocalDate): List<ItemDemandForecast>
    fun findByMenuItemId(menuItemId: UUID): List<ItemDemandForecast>
    fun findByMenuItemIdOrderByForecastDateDescHourOfDayDesc(menuItemId: UUID): List<ItemDemandForecast>

    @Query("""
        SELECT f FROM ItemDemandForecast f
        WHERE f.restaurantId = :restaurantId
          AND f.locationId = :locationId
          AND f.menuItemId = :menuItemId
          AND f.forecastDate = :forecastDate
          AND f.hourOfDay = :hourOfDay
    """)
    fun findExisting(
        restaurantId: UUID,
        locationId: UUID,
        menuItemId: UUID,
        forecastDate: java.time.LocalDate,
        hourOfDay: Int
    ): Optional<ItemDemandForecast>
}

// ─── Kitchen Display ─────────────────────────────────────────────────────────

interface KitchenStationRepository : JpaRepository<KitchenStation, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<KitchenStation>
}

interface KitchenTicketRepository : JpaRepository<KitchenTicket, UUID> {
    fun findByStationIdAndStatusOrderByCreatedAt(stationId: UUID, status: String): List<KitchenTicket>
    fun findByOrderId(orderId: UUID): List<KitchenTicket>

    @Query("SELECT t FROM KitchenTicket t WHERE t.status NOT IN ('READY', 'BUMPED') ORDER BY t.createdAt ASC")
    fun findOpenTickets(): List<KitchenTicket>

    fun findByStationIdAndStatusNotInOrderByCreatedAt(stationId: UUID, excludeStatuses: List<String>): List<KitchenTicket>
}

// ─── Payments ────────────────────────────────────────────────────────────────

interface PaymentRepository : JpaRepository<Payment, UUID> {
    fun findByOrderId(orderId: UUID): List<Payment>
    fun findByStatus(status: String): List<Payment>
}

// ─── Multi-Tenant ────────────────────────────────────────────────────────────

interface RestaurantRepository : JpaRepository<Restaurant, UUID>

interface LocationRepository : JpaRepository<Location, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Location>
    fun findByIsActiveTrue(): List<Location>
}

// ─── Machine Learning & Intelligent Alerts ───────────────────────────────────

interface Predictive86AlertRepository : JpaRepository<Predictive86Alert, UUID> {
    fun findByStatusOrderByCreatedAtDesc(status: String): List<Predictive86Alert>
}
