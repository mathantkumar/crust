package com.crust.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus { Online, Offline }

fun Context.observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(NetworkStatus.Online) }
        override fun onLost(network: Network) { trySend(NetworkStatus.Offline) }
        override fun onUnavailable() { trySend(NetworkStatus.Offline) }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    manager.registerNetworkCallback(request, callback)

    // Emit initial state
    val isConnected = manager.activeNetwork
        ?.let { manager.getNetworkCapabilities(it) }
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    trySend(if (isConnected) NetworkStatus.Online else NetworkStatus.Offline)

    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
