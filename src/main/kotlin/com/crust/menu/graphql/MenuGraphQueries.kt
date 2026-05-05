package com.crust.menu.graphql

import com.crust.menu.repository.MenuAuditResultRepository
import com.crust.menu.repository.MenuVersionRepository
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class MenuGraphQueries(
    private val auditResultRepository: MenuAuditResultRepository,
    private val menuVersionRepository: MenuVersionRepository
) {
    @DgsQuery
    fun getMenuRisks(@InputArgument versionId: String): List<Map<String, Any>> {
        return auditResultRepository.findByMenuVersionId(versionId).map {
            mapOf(
                "id" to it.id.toString(),
                "category" to (it.category ?: "PRICING_STRATEGY"),
                "impactScore" to (it.impactScore ?: it.severityScore),
                "plainEnglishSummary" to (it.plainEnglishSummary ?: it.riskDescription),
                "suggestedAction" to (it.suggestedAction ?: "Review this item manually.")
            )
        }
    }

    @DgsQuery
    fun getActiveMenu(): Map<String, Any>? {
        val version = menuVersionRepository
            .findFirstByStatusOrderByCreatedAtDesc("PUBLISHED")
            .orElse(null) ?: return null

        return mapOf(
            "id" to version.id.toString(),
            "status" to version.status,
            "createdAt" to version.createdAt.toString(),
            "categories" to version.categories.map { cat ->
                mapOf(
                    "id" to cat.id.toString(),
                    "name" to cat.name,
                    "menuItems" to cat.menuItems.map { item ->
                        mapOf(
                            "id" to item.id.toString(),
                            "name" to item.name,
                            "basePrice" to item.basePrice,
                            "modifierGroups" to item.modifierGroups.map { group ->
                                mapOf(
                                    "id" to group.id.toString(),
                                    "name" to group.name,
                                    "modifiers" to group.modifiers.map { mod ->
                                        mapOf(
                                            "id" to mod.id.toString(),
                                            "name" to mod.name,
                                            "priceAdjustment" to mod.priceAdjustment
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
    }
}
