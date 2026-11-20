package com.kitsunime.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kitsunime.app.data.CatalogApi
import com.kitsunime.app.data.Episode
import com.kitsunime.app.data.Title
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SPEEDS = listOf(1.0, 1.25, 1.5, 1.75, 2.0)

private fun fmt(sec: Double): String {
    val s = sec.toInt().coerceAtLeast(0)
    return if (s >= 3600) {
        "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    } else {
        "%d:%02d".format(s / 60, s % 60)
    }
}

/**
 * Pemutar mpv. Satu instance MPVLib per layar; permukaan video dari
 * SurfaceView, semua kontrol digambar Compose di atasnya.
 */
@Composable
fun PlayerScreen(
    title: Title,
    episode: Episode,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var position by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var paused by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1.0) }
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }
    var surfaceReady by remember { mutableStateOf(false) }
    var loadedFor by remember { mutableStateOf<String?>(null) }

    val mpv = remember {
        MPVLib.create(context.applicationContext)?.also { m ->
            m.setOptionString("vo", "gpu")
            m.setOptionString("gpu-context", "android")
            m.setOptionString("hwdec", "mediacodec")
            // mpv tidak bisa membaca gudang sertifikat Android; untuk v1
            // verifikasi TLS dimatikan supaya HLS dari CDN bisa diputar.
            m.setOptionString("tls-verify", "no")
            m.setOptionString("cache", "yes")
            m.setOptionString("demuxer-max-bytes", "64MiB")
            m.setOptionString("demuxer-max-back-bytes", "32MiB")
            m.init()
        }
    }

    DisposableEffect(mpv) {
        if (mpv == null) {
            failed = "Mesin mpv tidak bisa dibuat di perangkat ini."
            onDispose { }
        } else {
            val observer = object : MPVLib.EventObserver {
                override fun eventProperty(property: String) {}
                override fun eventProperty(property: String, value: Long) {}
                override fun eventProperty(property: String, value: Double) {
                    when (property) {
                        "time-pos" -> if (!seeking) position = value
                        "duration" -> duration = value
                    }
                }
                override fun eventProperty(property: String, value: Boolean) {
                    if (property == "pause") paused = value
                }
                override fun eventProperty(property: String, value: String) {}
                override fun event(eventId: Int) {
                    if (eventId == MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED) loaded = true
                }
            }
            mpv.addObserver(observer)
            mpv.observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            mpv.observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            mpv.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            onDispose {
                try { mpv.removeObserver(observer) } catch (e: Exception) { }
                try { mpv.destroy() } catch (e: Exception) { }
            }
        }
    }

    // Layar miring + tetap menyala selama menonton.
    DisposableEffect(Unit) {
        val oldOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (oldOrientation != null) activity?.requestedOrientation = oldOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Muat berkas begitu permukaan siap (pilih pintu CDN tercepat dulu).
    LaunchedEffect(surfaceReady, episode.playlist) {
        val m = mpv ?: return@LaunchedEffect
        if (!surfaceReady || loadedFor == episode.playlist) return@LaunchedEffect
        loadedFor = episode.playlist
        loaded = false
        val door = try {
            CatalogApi.pickDoor(episode.playlist)
        } catch (e: Exception) {
            episode.playlist
        }
        try {
            m.command(arrayOf("loadfile", door))
        } catch (e: Exception) {
            failed = "Perintah putar gagal: ${e.message}"
        }
    }

    // Batas waktu muat: kalau 25 detik belum juga mulai, kabari pengguna.
    LaunchedEffect(loadedFor) {
        if (loadedFor == null) return@LaunchedEffect
        delay(25_000)
        if (!loaded && failed == null) {
            failed = "Video belum juga mulai. Cek koneksi, lalu kembali dan coba lagi."
        }
    }

    // Kontrol sembunyi sendiri saat video jalan.
    LaunchedEffect(controlsVisible, paused, seeking) {
        if (controlsVisible && !paused && !seeking) {
            delay(3_500)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (mpv != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try { mpv.attachSurface(holder.surface) } catch (e: Exception) { }
                                surfaceReady = true
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {
                                try {
                                    mpv.setPropertyString("android-surface-size", "${width}x$height")
                                } catch (e: Exception) { }
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                surfaceReady = false
                                try { mpv.detachSurface() } catch (e: Exception) { }
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Lapisan ketuk untuk memunculkan/menyembunyikan kontrol.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { controlsVisible = !controlsVisible },
        )

        if (!loaded && failed == null) {
            CircularProgressIndicator(
                color = KitsunimeYellow,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (failed != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = failed!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onBack) {
                    Text("Kembali", color = KitsunimeYellow, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (controlsVisible) {
            // Bar atas: kembali + judul.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xB3000000), Color.Transparent),
                        )
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = episode.title ?: "Episode ${episode.number}",
                        color = Color(0xFFD8D4C8),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Bar bawah: putar/jeda, geser, waktu, kecepatan, subtitle, audio.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xB3000000)),
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            try { mpv?.setPropertyBoolean("pause", !paused) } catch (e: Exception) { }
                        },
                    ) {
                        Icon(
                            if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (paused) "Putar" else "Jeda",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = fmt(if (seeking) seekValue.toDouble() else position),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        value = if (seeking) seekValue else position.toFloat(),
                        onValueChange = {
                            seeking = true
                            seekValue = it
                        },
                        onValueChangeFinished = {
                            scope.launch {
                                try {
                                    mpv?.command(arrayOf("seek", seekValue.toDouble().toString(), "absolute"))
                                } catch (e: Exception) { }
                                seeking = false
                            }
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = KitsunimeYellow,
                            activeTrackColor = KitsunimeYellow,
                            inactiveTrackColor = Color(0x55FFFFFF),
                        ),
                    )
                    Text(
                        text = fmt(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(
                        onClick = {
                            val next = SPEEDS[(SPEEDS.indexOf(speed) + 1) % SPEEDS.size]
                            speed = next
                            try { mpv?.setPropertyDouble("speed", next) } catch (e: Exception) { }
                        },
                    ) {
                        Text("${speed}×", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            try { mpv?.command(arrayOf("cycle", "sub")) } catch (e: Exception) { }
                        },
                    ) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = "Ganti subtitle",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = {
                            try { mpv?.command(arrayOf("cycle", "audio")) } catch (e: Exception) { }
                        },
                    ) {
                        Icon(
                            Icons.Filled.Audiotrack,
                            contentDescription = "Ganti audio",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
