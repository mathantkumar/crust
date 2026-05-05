package com.crust.android.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crust.android.CrustApplication
import com.crust.android.data.repository.MenuRepository
import com.crust.android.data.repository.MenuRepositoryImpl
import com.crust.android.graphql.GetActiveMenuQuery
import com.crust.android.network.NetworkStatus
import com.crust.android.network.observeNetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MenuUiState {
    data object Loading : MenuUiState
    data class Success(val menu: GetActiveMenuQuery.GetActiveMenu) : MenuUiState
    data class Error(val message: String, val cachedMenu: GetActiveMenuQuery.GetActiveMenu?) : MenuUiState
}

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MenuRepository = MenuRepositoryImpl(
        context = application,
        apolloClient = (application as CrustApplication).apolloClient
    )

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    val networkStatus: StateFlow<NetworkStatus> = application
        .observeNetworkStatus()
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetworkStatus.Offline)

    init {
        loadMenu()
    }

    fun loadMenu() {
        viewModelScope.launch {
            _uiState.value = MenuUiState.Loading
            val result = repository.fetchActiveMenu()
            result.fold(
                onSuccess = { _uiState.value = MenuUiState.Success(it) },
                onFailure = { error ->
                    // Fall back to the last cached menu so the app stays usable offline
                    repository.cachedMenu().collect { cached ->
                        _uiState.value = MenuUiState.Error(
                            message = error.message ?: "Network error",
                            cachedMenu = cached
                        )
                        return@collect
                    }
                }
            )
        }
    }
}
