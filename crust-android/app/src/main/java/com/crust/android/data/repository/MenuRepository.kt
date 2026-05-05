package com.crust.android.data.repository

import com.crust.android.graphql.GetActiveMenuQuery
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    suspend fun fetchActiveMenu(): Result<GetActiveMenuQuery.GetActiveMenu>
    fun cachedMenu(): Flow<GetActiveMenuQuery.GetActiveMenu?>
}
