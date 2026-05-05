package com.crust.android.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.crust.android.graphql.GetActiveMenuQuery
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "menu_cache")

class MenuRepositoryImpl(
    private val context: Context,
    private val apolloClient: ApolloClient
) : MenuRepository {

    private val gson = Gson()
    private val cacheKey = stringPreferencesKey("active_menu_json")

    override suspend fun fetchActiveMenu(): Result<GetActiveMenuQuery.GetActiveMenu> {
        return try {
            val response = apolloClient.query(GetActiveMenuQuery()).execute()
            val menu = response.data?.getActiveMenu
                ?: return Result.failure(Exception("No active published menu found"))

            // Persist to DataStore as JSON for offline use
            context.dataStore.edit { prefs ->
                prefs[cacheKey] = gson.toJson(menu)
            }
            Result.success(menu)
        } catch (e: ApolloException) {
            Result.failure(e)
        }
    }

    override fun cachedMenu(): Flow<GetActiveMenuQuery.GetActiveMenu?> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[cacheKey] ?: return@map null
            runCatching {
                gson.fromJson<GetActiveMenuQuery.GetActiveMenu>(
                    json,
                    object : TypeToken<GetActiveMenuQuery.GetActiveMenu>() {}.type
                )
            }.getOrNull()
        }
    }
}
