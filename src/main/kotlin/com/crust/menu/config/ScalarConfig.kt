package com.crust.menu.config

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring

@DgsComponent
class ScalarConfig {
    @DgsRuntimeWiring
    fun addScalars(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        return builder.scalar(ExtendedScalars.Json)
    }
}
