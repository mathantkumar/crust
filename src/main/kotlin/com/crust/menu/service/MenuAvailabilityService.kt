package com.crust.menu.service

import com.crust.menu.domain.MenuItem
import com.crust.menu.repository.MenuVersionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuAvailabilityService(
    private val menuVersionRepository: MenuVersionRepository
) {
    private val log = LoggerFactory.getLogger(MenuAvailabilityService::class.java)

    /**
     * 86 an item — marks it as unavailable on the active menu.
     * In restaurant terminology, "86'd" means the item is out of stock.
     */
    @Transactional
    fun eightySixItem(itemId: UUID): MenuItem {
        val item = findMenuItemById(itemId)
        item.available = false
        item.quantityRemaining = 0
        log.info("86'd menu item '${item.name}' (${item.id})")

        return item
    }

    /**
     * Un-86 an item — restores availability with optional quantity.
     */
    @Transactional
    fun unEightySixItem(itemId: UUID, quantity: Int?): MenuItem {
        val item = findMenuItemById(itemId)
        item.available = true
        item.quantityRemaining = quantity
        log.info("Un-86'd menu item '${item.name}' (${item.id}), quantity: ${quantity ?: "unlimited"}")

        return item
    }

    private fun findMenuItemById(itemId: UUID): MenuItem {
        val activeMenu = menuVersionRepository.findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElseThrow { IllegalStateException("No active menu found") }

        return activeMenu.categories
            .flatMap { it.menuItems }
            .find { it.id == itemId }
            ?: throw IllegalArgumentException("Menu item $itemId not found in active menu")
    }
}
