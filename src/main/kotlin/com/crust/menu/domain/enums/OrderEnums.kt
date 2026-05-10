package com.crust.menu.domain.enums

/** Channel through which an order originates */
enum class OrderChannel {
    DINE_IN, TAKEOUT, ONLINE, THIRD_PARTY
}

/** Lifecycle states of a restaurant order */
enum class OrderStatus {
    CREATED, SENT_TO_KITCHEN, IN_PROGRESS, READY, COMPLETED, CANCELLED;

    /** Valid state transitions — enforces the order lifecycle */
    fun canTransitionTo(next: OrderStatus): Boolean = when (this) {
        CREATED -> next in listOf(SENT_TO_KITCHEN, CANCELLED)
        SENT_TO_KITCHEN -> next in listOf(IN_PROGRESS, CANCELLED)
        IN_PROGRESS -> next in listOf(READY, CANCELLED)
        READY -> next in listOf(COMPLETED)
        COMPLETED -> false
        CANCELLED -> false
    }
}

/** Lifecycle states of individual order line items */
enum class OrderItemStatus {
    PENDING, PREPARING, READY, SERVED, VOIDED
}

/** Kitchen ticket states for KDS */
enum class TicketStatus {
    NEW, ACKNOWLEDGED, IN_PROGRESS, READY, BUMPED
}

/** Payment lifecycle */
enum class PaymentStatus {
    INITIATED, AUTHORIZED, CAPTURED, REFUNDED, FAILED
}

/** Payment method types */
enum class PaymentMethod {
    CARD, CASH, MOBILE_PAY, GIFT_CARD
}
