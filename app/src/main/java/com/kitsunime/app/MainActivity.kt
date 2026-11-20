package com.kitsunime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kitsunime.app.data.CatalogApi
import com.kitsunime.app.data.Episode
import com.kitsunime.app.data.Title
import com.kitsunime.app.ui.DetailScreen
import com.kitsunime.app.ui.HomeScreen
import com.kitsunime.app.ui.KitsunimeTheme
import com.kitsunime.app.ui.PlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KitsunimeTheme {
                KitsunimeApp()
            }
        }
    }
}

private sealed interface Screen {
    object Home : Screen
    data class Detail(val title: Title) : Screen
    data class Player(val title: Title, val episode: Episode) : Screen
}

@Composable
private fun KitsunimeApp() {
    var titles by remember { mutableStateOf<List<Title>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            titles = CatalogApi.fetchCatalog(force = reloadKey > 0)
        } catch (e: Exception) {
            error = e.message ?: "Gagal memuat katalog"
        }
        loading = false
    }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Player -> Screen.Detail(s.title)
            is Screen.Detail -> Screen.Home
            Screen.Home -> Screen.Home
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                titles = titles,
                loading = loading,
                error = error,
                onRetry = { reloadKey++ },
                onOpen = { screen = Screen.Detail(it) },
                onPlay = { t, e -> screen = Screen.Player(t, e) },
            )
            is Screen.Detail -> DetailScreen(
                title = s.title,
                onBack = { screen = Screen.Home },
                onPlay = { e -> screen = Screen.Player(s.title, e) },
            )
            is Screen.Player -> PlayerScreen(
                title = s.title,
                episode = s.episode,
                onBack = { screen = Screen.Detail(s.title) },
            )
        }
    }
}
