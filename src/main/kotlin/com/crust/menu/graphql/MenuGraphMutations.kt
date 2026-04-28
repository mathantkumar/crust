package com.crust.menu.graphql

import com.crust.menu.service.MenuCommandService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import java.util.UUID

@DgsComponent
class MenuGraphMutations(
    private val menuCommandService: MenuCommandService
) {
    @DgsMutation
    fun publishMenu(@InputArgument versionId: String): Boolean {
        return menuCommandService.publishMenuVersion(UUID.fromString(versionId))
    }

    @DgsMutation
    fun revertToCleanVersion(@InputArgument versionId: String): Boolean {
        // Implement simple status rollback routing logic natively calling out service boundaries
        return menuCommandService.revertMenuVersion(UUID.fromString(versionId))
    }
}
