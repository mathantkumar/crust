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

    @Query("SELECT o FROM RestaurantOrder o WHERE o.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY o.createdAt DESC")
    fun findActiveOrders(): List<RestaurantOrder>

    @Query("SELECT o FROM RestaurantOrder o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    fun findOrdersSince(since: LocalDateTime): List<RestaurantOrder>
}

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {
    fun findByOrderId(orderId: UUID): List<OrderItem>
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
