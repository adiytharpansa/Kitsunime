package com.kitsunime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kitsunime.app.data.Episode
import com.kitsunime.app.data.Title

@Composable
fun HomeScreen(
    titles: List<Title>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpen: (Title) -> Unit,
    onPlay: (Title, Episode) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(titles, query) {
        if (query.isBlank()) titles
        else titles.filter { it.title.contains(query, ignoreCase = true) }
    }
    val topRated = remember(titles) {
        titles.filter { it.score != null }.sortedByDescending { it.score }.take(15)
    }
    val fresh = remember(titles) { titles.take(15) }
    val hero = titles.firstOrNull { it.banner != null } ?: titles.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(KitsunimeYellow),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "KITSUNIME",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Cari judul…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )

        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = KitsunimeYellow)
            }
            error != null && titles.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Katalog belum bisa dimuat.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KitsunimeYellow,
                        contentColor = Color(0xFF1A1500),
                    ),
                ) {
                    Text("Coba Lagi")
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (query.isBlank()) {
                    if (hero != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HeroBanner(hero = hero, onPlay = onPlay)
                        }
                    }
                    if (topRated.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RailHeader("Nilai Tertinggi")
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PosterRail(list = topRated, onOpen = onOpen)
                        }
                    }
                    if (fresh.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RailHeader("Baru Ditambah")
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PosterRail(list = fresh, onOpen = onOpen)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RailHeader("Semua Judul")
                    }
                }
                items(filtered, key = { it.id }) { title ->
                    PosterCard(title = title, onClick = { onOpen(title) })
                }
            }
        }
    }
}

@Composable
private fun RailHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun HeroBanner(hero: Title, onPlay: (Title, Episode) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = hero.banner ?: hero.poster,
            contentDescription = hero.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        startY = 60f,
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text = hero.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (hero.genres.isNotEmpty()) {
                Text(
                    text = hero.genres.take(4).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD8D4C8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            val first = hero.episodes.firstOrNull()
            Button(
                onClick = { if (first != null) onPlay(hero, first) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KitsunimeYellow,
                    contentColor = Color(0xFF1A1500),
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Putar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PosterRail(list: List<Title>, onOpen: (Title) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.id }) { title ->
            Box(modifier = Modifier.width(112.dp)) {
                PosterCard(title = title, onClick = { onOpen(title) })
            }
        }
    }
}

@Composable
fun PosterCard(title: Title, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = title.poster,
            contentDescription = title.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (title.score != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = KitsunimeYellow,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = String.format("%.1f", title.score),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
