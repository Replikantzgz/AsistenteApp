package com.iatualcance.alfred

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.CompositionLocalProvider
import com.iatualcance.alfred.ui.AlfredTheme
import com.iatualcance.alfred.ui.AppRoot
import com.iatualcance.alfred.ui.LocalServices
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** Contenido compartido desde otra app (texto, imagen, audio o PDF), pendiente de procesar. */
    val shared = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        val sv = (application as AlfredApp).sv
        setContent {
            CompositionLocalProvider(LocalServices provides sv) {
                AlfredTheme { AppRoot(shared) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND) shared.value = i
    }
}
