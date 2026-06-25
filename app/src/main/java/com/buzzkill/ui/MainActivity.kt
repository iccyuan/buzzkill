package com.buzzkill.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buzzkill.data.ThemeStore
import com.buzzkill.ui.nav.BuzzKillNavHost
import com.buzzkill.ui.theme.BuzzKillTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        ThemeStore.ensureLoaded(this)
        super.onCreate(savedInstanceState)
        setContent {
            val mode by ThemeStore.mode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeStore.LIGHT -> false
                ThemeStore.DARK -> true
                else -> isSystemInDarkTheme()
            }
            BuzzKillTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1100)
                        showSplash = false
                    }
                    Box(Modifier.fillMaxSize()) {
                        BuzzKillNavHost()
                        AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(420))) {
                            SplashIntro()
                        }
                    }
                }
            }
        }
    }
}
