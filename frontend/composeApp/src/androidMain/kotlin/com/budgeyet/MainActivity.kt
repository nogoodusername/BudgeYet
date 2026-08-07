package com.budgeyet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.budgeyet.core.persistence.AndroidAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must happen before App()/AppContainer() build — createSettingsStorage() (called from
        // AppContainer's init) reads this synchronously.
        AndroidAppContext.applicationContext = applicationContext
        setContent {
            App()
        }
    }
}
