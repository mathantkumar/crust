package com.crust.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.crust.android.ui.menu.MenuBrowsingScreen
import com.crust.android.ui.theme.CrustTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrustTheme {
                MenuBrowsingScreen()
            }
        }
    }
}
