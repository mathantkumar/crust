package com.crust.menu.service

import com.crust.menu.domain.KitchenTicket
import com.crust.menu.domain.RestaurantOrder
import com.crust.menu.repository.KitchenStationRepository
import com.crust.menu.repository.KitchenTicketRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class KitchenOrderReadyEvent(val orderId: UUID)

@Service
class KitchenDisplayService(
    private val ticketRepository: KitchenTicketRepository,
    private val stationRepository: KitchenStationRepository,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(KitchenDisplayService::class.java)

    /**
     * Splits an order into kitchen tickets by station.
     * For now, all items go to the "Expo" station. In production, station routing
     * would be based on item category or explicit station_routing on the OrderItem.
     */
    @Transactional
    fun createTicketsForOrder(order: RestaurantOrder) {
        val stations = stationRepository.findByIsActiveTrueOrderByDisplayOrder()
        val expoStation = stations.find { it.name == "Expo" } ?: stations.firstOrNull()
            ?: throw IllegalStateException("No active kitchen stations configured")

        // Group items by station routing (default to Expo)
        val itemsByStation = order.items.groupBy { item ->
            val routingName = item.stationRouting ?: "Expo"
            stations.find { it.name == routingName } ?: expoStation
        }

        for ((station, items) in itemsByStation) {
            val itemsJson = objectMapper.writeValueAsString(items.map { item ->
                mapOf(
                    "id" to item.id.toString(),
                    "name" to item.menuItemName,
                    "quantity" to item.quantity,
                    "modifiers" to item.modifierSelections,
                    "instructions" to item.specialInstructions
                )
            })

            val ticket = KitchenTicket(
                orderId = order.id,
                orderNumber = order.orderNumber,
                stationId = station.id,
                stationName = station.name,
                items = itemsJson
            )
            ticketRepository.save(ticket)
            log.info("Created KDS ticket ${ticket.id} for order ${order.id} → station ${station.name} with ${items.size} items")
        }
    }

    @Transactional
    fun acknowledgeTicket(ticketId: UUID): KitchenTicket {
        val ticket = ticketRepository.findById(ticketId).orElseThrow {
            IllegalArgumentException("Ticket $ticketId not found")
        }
        ticket.status = "ACKNOWLEDGED"
        ticket.acknowledgedAt = LocalDateTime.now()
        log.info("Ticket $ticketId acknowledged at station ${ticket.stationName}")
        return ticketRepository.save(ticket)
    }

    @Transactional
    fun markReady(ticketId: UUID): KitchenTicket {
        val ticket = ticketRepository.findById(ticketId).orElseThrow {
            IllegalArgumentException("Ticket $ticketId not found")
        }
        ticket.status = "READY"
        ticket.completedAt = LocalDateTime.now()
        log.info("Ticket $ticketId marked READY at station ${ticket.stationName}")
        val saved = ticketRepository.save(ticket)
        checkAllTicketsReadyForOrder(ticket.orderId)
        return saved
    }

    @Transactional
    fun bumpTicket(ticketId: UUID): KitchenTicket {
        val ticket = ticketRepository.findById(ticketId).orElseThrow {
            IllegalArgumentException("Ticket $ticketId not found")
        }
        ticket.status = "BUMPED"
        if (ticket.completedAt == null) ticket.completedAt = LocalDateTime.now()
        log.info("Ticket $ticketId bumped at station ${ticket.stationName}")
        val saved = ticketRepository.save(ticket)
        checkAllTicketsReadyForOrder(ticket.orderId)
        return saved
    }

    private fun checkAllTicketsReadyForOrder(orderId: UUID) {
        val allTickets = ticketRepository.findByOrderId(orderId)
        val allReady = allTickets.all { it.status == "READY" || it.status == "BUMPED" }
        if (allReady && allTickets.isNotEmpty()) {
            eventPublisher.publishEvent(KitchenOrderReadyEvent(orderId))
        }
    }

    fun getTicketsByStation(stationId: UUID): List<KitchenTicket> =
        ticketRepository.findByStationIdAndStatusNotInOrderByCreatedAt(stationId, listOf("READY", "BUMPED"))

    fun getOpenTickets(): List<KitchenTicket> = ticketRepository.findOpenTickets()

    fun getStations() = stationRepository.findByIsActiveTrueOrderByDisplayOrder()
}
