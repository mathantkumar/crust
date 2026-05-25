package com.crust.menu.service

import com.crust.menu.domain.ItemDemandForecast
import com.crust.menu.domain.ItemSalesHourly
import com.crust.menu.repository.ItemDemandForecastRepository
import com.crust.menu.repository.ItemSalesHourlyRepository
import com.crust.menu.repository.MenuVersionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/**
 * Lightweight demand forecasting engine.
 *
 * Uses a **weighted moving average** over historical hourly sales data
 * (from item_sales_hourly) to predict future demand per item per hour.
 *
 * Model: WMA-4w (Weighted Moving Average, 4-week lookback)
 * ─────────────────────────────────────────────────────────
 * For each (item, hour, day-of-week):
 *   - Pulls the last 4 weeks of same-day/same-hour sales
 *   - Applies exponential decay weights: [0.4, 0.3, 0.2, 0.1] (most recent → oldest)
 *   - predicted_quantity = Σ(weight_i × qty_i)
 *   - predicted_revenue = predicted_quantity × current_menu_price
 *   - confidence = f(data completeness, variance)
 *
 * This gives useful baseline results fast. The model_version field lets us
 * A/B test against more sophisticated models later without schema changes.
 */
@Service
class DemandForecastService(
    private val salesHourlyRepository: ItemSalesHourlyRepository,
    private val forecastRepository: ItemDemandForecastRepository,
    private val menuVersionRepository: MenuVersionRepository
) {
    private val log = LoggerFactory.getLogger(DemandForecastService::class.java)

    companion object {
        const val MODEL_VERSION = "wma-4w-v1"
        const val LOOKBACK_WEEKS = 4
        val WEIGHTS = listOf(0.4, 0.3, 0.2, 0.1) // Most recent week first
        val OPERATING_HOURS = 6..23 // 6 AM to 11 PM
    }

    /**
     * Runs every 6 hours. Generates forecasts for the next 7 days.
     * Uses a 30s initial delay so the aggregation job can run first.
     */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000, initialDelay = 30_000)
    @Transactional
    fun generateForecasts() {
        val tenantItems = salesHourlyRepository.findDistinctTenantItems()
        if (tenantItems.isEmpty()) {
            log.info("Forecast: no sales history yet, skipping")
            return
        }

        // Resolve current prices from active menu
        val priceMap = resolveCurrentPrices()
        val itemNameMap = resolveItemNames()

        val today = LocalDate.now()
        var forecastCount = 0

        for (row in tenantItems) {
            val restaurantId = row[0] as UUID
            val locationId = row[1] as UUID
            val menuItemId = row[2] as UUID

            val itemName = itemNameMap[menuItemId] ?: "Unknown"
            val currentPrice = priceMap[menuItemId] ?: BigDecimal.ZERO

            // Generate forecasts for the next 7 days
            for (dayOffset in 0L..6L) {
                val forecastDate = today.plusDays(dayOffset)
                val dayOfWeek = forecastDate.dayOfWeek.value

                // Get training data: same day-of-week, last N weeks
                val since = forecastDate.minusWeeks(LOOKBACK_WEEKS.toLong() + 1)
                val trainingData = salesHourlyRepository.findTrainingData(restaurantId, locationId, menuItemId, dayOfWeek, since)

                for (hour in OPERATING_HOURS) {
                    val prediction = predictForHour(trainingData, hour, currentPrice)
                    upsertForecast(restaurantId, locationId, menuItemId, itemName, forecastDate, hour, dayOfWeek, prediction)
                    forecastCount++
                }
            }
        }

        log.info("Forecast: generated $forecastCount predictions for ${tenantItems.size} tenant-items using model $MODEL_VERSION")
    }

    // ─── Prediction Engine ───────────────────────────────────────────────────

    data class HourlyPrediction(
        val predictedQuantity: BigDecimal,
        val predictedRevenue: BigDecimal,
        val confidence: BigDecimal
    )

    /**
     * Weighted Moving Average prediction for a single hour.
     *
     * Groups the training data by week (most recent first), applies decay
     * weights, and computes weighted average quantity. Revenue is projected
     * using current menu price, not historical price — so it reflects
     * what we'd earn at today's pricing.
     */
    private fun predictForHour(
        trainingData: List<ItemSalesHourly>,
        hour: Int,
        currentPrice: BigDecimal
    ): HourlyPrediction {
        // Filter to just this hour, group by week (most recent first)
        val hourData = trainingData.filter { it.hourOfDay == hour }
            .sortedByDescending { it.salesDate }

        if (hourData.isEmpty()) {
            return HourlyPrediction(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("0.1000"))
        }

        // Group by sales_date (each date = one week's data point for this day-of-week)
        val weeklyQuantities = hourData
            .map { it.quantitySold }
            .take(LOOKBACK_WEEKS)

        // Apply weights (pad with zeros if fewer weeks available)
        val paddedQty = weeklyQuantities + List(LOOKBACK_WEEKS - weeklyQuantities.size) { 0 }
        val activeWeights = WEIGHTS.take(paddedQty.size)

        val weightedSum = paddedQty.zip(activeWeights).sumOf { (qty, weight) ->
            qty.toDouble() * weight
        }

        val predictedQty = BigDecimal(weightedSum).setScale(2, RoundingMode.HALF_UP)
        val predictedRevenue = predictedQty.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP)

        // Confidence: based on data completeness and coefficient of variation
        val confidence = calculateConfidence(weeklyQuantities)

        return HourlyPrediction(predictedQty, predictedRevenue, confidence)
    }

    /**
     * Confidence score: higher when we have more data and lower variance.
     *
     * - Data completeness: how many of the 4 weeks have data (0-1)
     * - Stability: 1 - normalized CV (coefficient of variation) (0-1)
     * - Final = 0.6 * completeness + 0.4 * stability, clamped to [0.1, 0.99]
     */
    private fun calculateConfidence(weeklyQuantities: List<Int>): BigDecimal {
        val completeness = weeklyQuantities.size.toDouble() / LOOKBACK_WEEKS
        if (weeklyQuantities.size < 2) {
            return BigDecimal(completeness * 0.5).setScale(4, RoundingMode.HALF_UP)
                .coerceIn(BigDecimal("0.1000"), BigDecimal("0.9900"))
        }

        val mean = weeklyQuantities.average()
        val variance = weeklyQuantities.map { (it - mean) * (it - mean) }.average()
        val stdDev = Math.sqrt(variance)
        val cv = if (mean > 0) stdDev / mean else 1.0
        val stability = (1.0 - cv.coerceAtMost(1.0)).coerceAtLeast(0.0)

        val raw = 0.6 * completeness + 0.4 * stability
        return BigDecimal(raw).setScale(4, RoundingMode.HALF_UP)
            .coerceIn(BigDecimal("0.1000"), BigDecimal("0.9900"))
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private fun upsertForecast(
        restaurantId: UUID,
        locationId: UUID,
        menuItemId: UUID,
        itemName: String,
        forecastDate: LocalDate,
        hour: Int,
        dayOfWeek: Int,
        prediction: HourlyPrediction
    ) {
        val existing = forecastRepository.findExisting(restaurantId, locationId, menuItemId, forecastDate, hour)

        if (existing.isPresent) {
            val row = existing.get()
            row.predictedQuantity = prediction.predictedQuantity
            row.predictedRevenue = prediction.predictedRevenue
            row.confidence = prediction.confidence
            forecastRepository.save(row)
        } else {
            forecastRepository.save(
                ItemDemandForecast(
                    restaurantId = restaurantId,
                    locationId = locationId,
                    menuItemId = menuItemId,
                    itemName = itemName,
                    forecastDate = forecastDate,
                    hourOfDay = hour,
                    dayOfWeek = dayOfWeek,
                    predictedQuantity = prediction.predictedQuantity,
                    predictedRevenue = prediction.predictedRevenue,
                    confidence = prediction.confidence,
                    modelVersion = MODEL_VERSION
                )
            )
        }
    }

    // ─── Menu Price Resolution ───────────────────────────────────────────────

    private fun resolveCurrentPrices(): Map<UUID, BigDecimal> {
        val activeMenu = menuVersionRepository
            .findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElse(null) ?: return emptyMap()

        return activeMenu.categories
            .flatMap { it.menuItems }
            .associate { it.id to (it.basePrice ?: BigDecimal.ZERO) }
    }

    private fun resolveItemNames(): Map<UUID, String> {
        val activeMenu = menuVersionRepository
            .findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElse(null) ?: return emptyMap()

        return activeMenu.categories
            .flatMap { it.menuItems }
            .associate { it.id to it.name }
    }

    // ─── Public API (for GraphQL / on-demand) ────────────────────────────────

    fun getForecast(dateFrom: LocalDate, dateTo: LocalDate): List<ItemDemandForecast> =
        forecastRepository.findByForecastDateBetween(dateFrom, dateTo)

    fun getForecastForItem(menuItemId: UUID): List<ItemDemandForecast> =
        forecastRepository.findByMenuItemId(menuItemId)
}
