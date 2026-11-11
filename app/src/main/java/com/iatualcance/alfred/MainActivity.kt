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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Contenido compartido desde otra app (texto, imagen, audio o PDF), pendiente de procesar. */
    val shared = MutableStateFlow<Intent?>(null)
    /** Pantalla a la que lleva el aviso en el que el usuario ha tocado. */
    val route = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        val sv = (application as AlfredApp).sv
        setContent {
            CompositionLocalProvider(LocalServices provides sv) {
                AlfredTheme { AppRoot(shared, route) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sv = (application as AlfredApp).sv
        kotlinx.coroutines.MainScope().launch { sv.refreshReminders(true) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND) shared.value = i
        i?.getStringExtra(com.iatualcance.alfred.notify.Notif.EXTRA_ROUTE)?.let { route.value = it; i.removeExtra(com.iatualcance.alfred.notify.Notif.EXTRA_ROUTE) }
    }
}
