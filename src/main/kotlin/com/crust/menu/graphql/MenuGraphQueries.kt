package com.crust.menu.graphql

import com.crust.menu.repository.MenuAuditResultRepository
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class MenuGraphQueries(
    private val auditResultRepository: MenuAuditResultRepository
) {
    @DgsQuery
    fun getMenuRisks(@InputArgument versionId: String): List<Map<String, Any>> {
        return auditResultRepository.findByMenuVersionId(versionId).map {
            mapOf(
                "id" to it.id.toString(),
                "description" to it.riskDescription,
                "severityScore" to it.severityScore
            )
        }
    }
}
