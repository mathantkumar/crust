package com.crust.menu.graphql.types

import java.math.BigDecimal
import java.util.UUID

data class OrderData(
    val id: String,
    val orderNumber: String,
    val status: String,
    val channel: String,
    val tableNumber: String?,
    val serverName: String?,
    val guestCount: Int?,
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val items: List<OrderItemData>,
    val createdAt: String,
    val updatedAt: String
)

data class OrderItemData(
    val id: String,
    val menuItemId: String,
    val menuItemName: String,
    val quantity: Int,
    val basePrice: BigDecimal,
    val lineTotal: BigDecimal,
    val modifierSelections: String?,
    val specialInstructions: String?,
    val modifiers: List<OrderItemModifierData>
)

data class OrderItemModifierData(
    val id: String,
    val modifierId: String?,
    val modifierName: String,
    val priceImpact: BigDecimal
)

data class MenuVersionData(
    val id: String,
    val versionNotes: String?,
    val status: String,
    val createdAt: String,
    val categories: List<CategoryData>
)

data class CategoryData(
    val id: String,
    val name: String,
    val description: String?,
    val menuItems: List<MenuItemData>
)

data class MenuItemData(
    val id: String,
    val name: String,
    val description: String?,
    val basePrice: BigDecimal?,
    val available: Boolean,
    val quantityRemaining: Int?,
    val modifierGroups: List<ModifierGroupData>
)

data class ModifierGroupData(
    val id: String,
    val name: String,
    val modifiers: List<ModifierData>
)

data class ModifierData(
    val id: String,
    val name: String,
    val priceAdjustment: BigDecimal
)

data class KitchenTicketData(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val stationId: String,
    val stationName: String,
    val items: String,
    val status: String,
    val createdAt: String,
    val acknowledgedAt: String?,
    val completedAt: String?
)

data class KitchenStationData(
    val id: String,
    val name: String,
    val isActive: Boolean
)

data class PaymentData(
    val id: String,
    val orderId: String,
    val amount: BigDecimal,
    val tipAmount: BigDecimal,
    val totalCaptured: BigDecimal,
    val status: String,
    val method: String,
    val receiptUrl: String?,
    val createdAt: String
)

data class ItemSalesHourlyData(
    val id: String,
    val restaurantId: String?,
    val locationId: String?,
    val menuItemId: String,
    val itemName: String,
    val categoryName: String?,
    val salesDate: String,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val quantitySold: Int,
    val grossRevenue: BigDecimal,
    val modifierRevenue: BigDecimal,
    val orderCount: Int,
    val avgUnitPrice: BigDecimal,
    val lastUpdatedAt: String
)

data class ItemDemandForecastData(
    val id: String,
    val menuItemId: String,
    val itemName: String,
    val forecastDate: String,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val predictedQuantity: BigDecimal,
    val predictedRevenue: BigDecimal,
    val confidence: BigDecimal,
    val modelVersion: String,
    val generatedAt: String
)

data class Predictive86AlertData(
    val id: String,
    val menuItemId: String,
    val itemName: String,
    val triggerOrderId: String,
    val currentStock: Int,
    val predictedDemand: BigDecimal,
    val riskScore: BigDecimal,
    val alertMessage: String,
    val status: String,
    val createdAt: String,
    val resolvedAt: String?
)
