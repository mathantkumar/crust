package com.crust.menu.service

import com.crust.menu.domain.Predictive86Alert
import com.crust.menu.domain.ItemDemandForecast
import com.crust.menu.repository.MenuVersionRepository
import com.crust.menu.repository.OrderItemRepository
import com.crust.menu.repository.OrderRepository
import com.crust.menu.repository.Predictive86AlertRepository
import com.crust.menu.repository.ItemDemandForecastRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

@Service
class Intelligent86Service(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val menuVersionRepository: MenuVersionRepository,
    private val forecastRepository: ItemDemandForecastRepository,
    private val predictive86AlertRepository: Predictive86AlertRepository
) {
    private val log = LoggerFactory.getLogger(Intelligent86Service::class.java)

    /**
     * The Entry Point: Listens for new orders asynchronously so it never blocks the main transaction.
     */
    @Async
    @EventListener
    @Transactional
    fun onOrderCreated(event: OrderCreatedEvent) {
        log.info("Intelligent86Service woke up for order ${event.orderId}")
        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        // We only care about published items to get current stock
        val activeMenu = menuVersionRepository.findFirstByStatusOrderByCreatedAtDesc("PUBLISHED").orElse(null) ?: return
        val allItems = activeMenu.categories.flatMap { it.menuItems }

        for (item in order.items) {
            val menuItem = allItems.find { it.id == item.menuItemId } ?: continue
            val currentStock = menuItem.quantityRemaining

            // If we don't track inventory for this item, ignore it
            if (currentStock == null) continue

            // 1. Assemble Features
            val features = assembleFeatures(item.menuItemId, currentStock)

            // 2. ML Inference (XGBoost ONNX Simulator)
            val predictedDemand = XGBoostSimulator.predictNext60Mins(features)

            // 3. Heuristic Guardrail
            val riskScore = if (currentStock > 0) {
                predictedDemand / currentStock.toDouble()
            } else {
                999.0 // Already out of stock
            }

            log.info("Item ${item.menuItemName} - Predicted Demand: $predictedDemand, Stock: $currentStock, Risk: $riskScore")

            // 4. Action: Trigger alert if risk > 1.0
            if (riskScore > 1.0) {
                triggerAlert(item.menuItemId, item.menuItemName, order.id, currentStock, predictedDemand, riskScore)
            }
        }
    }

    private fun assembleFeatures(menuItemId: UUID, currentStock: Int): FeatureVector {
        val now = LocalDateTime.now()
        
        // A. Temporal
        val hourOfDay = now.hour
        val dayOfWeek = now.dayOfWeek.value
        val isHoliday = false // Stub
        val isPaydayWeekend = (now.dayOfMonth in 14..16 || now.dayOfMonth in 29..31) && (dayOfWeek >= 5)

        // B. Velocity
        val recentForecasts = forecastRepository.findByMenuItemIdOrderByForecastDateDescHourOfDayDesc(menuItemId)
        val wma4wPrediction = if (recentForecasts.isNotEmpty()) recentForecasts.first().predictedQuantity.toDouble() else 0.0

        // Find how many orders for this item happened in last 15 and 60 mins
        val recentItems = orderItemRepository.findAll() // In prod this would be a specific query: findSince(now.minusMinutes(60))
            .filter { it.menuItemId == menuItemId && it.order.createdAt.isAfter(now.minusMinutes(60)) }

        val ordersLast60Min = recentItems.sumOf { it.quantity }
        val ordersLast15Min = recentItems.filter { it.order.createdAt.isAfter(now.minusMinutes(15)) }.sumOf { it.quantity }

        // C. Inventory
        val stockToWmaRatio = if (wma4wPrediction > 0) currentStock / wma4wPrediction else currentStock.toDouble()

        // D. Contextual
        val weatherCondition = 1.0 // 1=Clear, 2=Rain, 3=Snow (Stub)
        val activePromotions = 0.0 // 0=No, 1=Yes (Stub)

        return FeatureVector(
            hourOfDay = hourOfDay.toDouble(),
            dayOfWeek = dayOfWeek.toDouble(),
            isHoliday = if (isHoliday) 1.0 else 0.0,
            isPaydayWeekend = if (isPaydayWeekend) 1.0 else 0.0,
            wma4wPrediction = wma4wPrediction,
            ordersLast15Min = ordersLast15Min.toDouble(),
            ordersLast60Min = ordersLast60Min.toDouble(),
            currentStockLevel = currentStock.toDouble(),
            stockToWmaRatio = stockToWmaRatio,
            weatherCondition = weatherCondition,
            activePromotions = activePromotions
        )
    }

    private fun triggerAlert(menuItemId: UUID, itemName: String, orderId: UUID, currentStock: Int, predictedDemand: Double, riskScore: Double) {
        val message = "⚠️ $itemName predicted to 86 in 60 minutes. $currentStock remaining."
        log.warn("PREDICTIVE 86 ALERT: $message (Risk Score: $riskScore)")

        val alert = Predictive86Alert(
            menuItemId = menuItemId,
            itemName = itemName,
            triggerOrderId = orderId,
            currentStock = currentStock,
            predictedDemand = BigDecimal(predictedDemand).setScale(2, RoundingMode.HALF_UP),
            riskScore = BigDecimal(riskScore).setScale(2, RoundingMode.HALF_UP),
            alertMessage = message,
            status = "UNRESOLVED"
        )
        predictive86AlertRepository.save(alert)
    }
}

/**
 * The Feature Vector representing the signals sent to the ML Model.
 */
data class FeatureVector(
    val hourOfDay: Double,
    val dayOfWeek: Double,
    val isHoliday: Double,
    val isPaydayWeekend: Double,
    val wma4wPrediction: Double,
    val ordersLast15Min: Double,
    val ordersLast60Min: Double,
    val currentStockLevel: Double,
    val stockToWmaRatio: Double,
    val weatherCondition: Double,
    val activePromotions: Double
)

/**
 * Simulator for the Embedded XGBoost inference. 
 * In production, this would use `xgboost4j` to load the `.onnx` or `.model` file:
 * `val booster = XGBoost.loadModel("predictive_86_model.bin")`
 * `val dMatrix = DMatrix(floatArrayOf(features...))`
 * `val prediction = booster.predict(dMatrix)`
 */
object XGBoostSimulator {
    fun predictNext60Mins(f: FeatureVector): Double {
        // We simulate the output of a decision tree structure
        var basePrediction = f.wma4wPrediction

        // Tree 1: Velocity spikes heavily influence short term demand
        if (f.ordersLast15Min > 2.0) {
            basePrediction += (f.ordersLast15Min * 1.5)
        }

        // Tree 2: Contextual adjustments
        if (f.isPaydayWeekend > 0.0) {
            basePrediction *= 1.3
        }

        // Tree 3: The model has learned that if stock ratio is low AND velocity is high, panic buying occurs
        if (f.stockToWmaRatio < 0.5 && f.ordersLast60Min > 5.0) {
            basePrediction *= 1.2
        }

        return basePrediction
    }
}
