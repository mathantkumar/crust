package com.crust.android

import android.app.Application
import com.apollographql.apollo.ApolloClient

class CrustApplication : Application() {

    lateinit var apolloClient: ApolloClient
        private set

    override fun onCreate() {
        super.onCreate()
        // Use 10.0.2.2 for Android emulator to reach host machine localhost
        apolloClient = ApolloClient.Builder()
            .serverUrl("http://10.0.2.2:8080/graphql")
            .build()
    }
}
