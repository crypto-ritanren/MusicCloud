package com.max.musiccloud.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.max.musiccloud.data.auth.AuthState
import com.max.musiccloud.data.remote.SyncState
import com.max.musiccloud.ui.MainViewModel
import com.max.musiccloud.ui.TrackFilter
import com.max.musiccloud.ui.components.ByokSettingsDialog
import com.max.musiccloud.ui.components.FullPlayerSheet
import com.max.musiccloud.ui.components.MiniPlayer
import com.max.musiccloud.ui.components.ThemeSelectionDialog
import com.max.musiccloud.ui.components.TrackItem
import com.max.musiccloud.ui.theme.AppTheme
import com.max.musiccloud.ui.theme.SuccessGreen
import com.max.musiccloud.ui.theme.ThemeMode

@Composable
fun LibraryScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val colors = AppTheme.colors

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val allTracksCount by viewModel.allTracksCount.collectAsStateWithLifecycle()
    val favoriteTracksCount by viewModel.favoriteTracksCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsStateWithLifecycle()
    val isByokSettingsVisible by viewModel.isByokSettingsVisible.collectAsStateWithLifecycle()
    val isThemeDialogVisible by viewModel.isThemeDialogVisible.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val webClientId by viewModel.webClientId.collectAsStateWithLifecycle()
    val clientSecret by viewModel.clientSecret.collectAsStateWithLifecycle()
    val targetFolderName by viewModel.targetFolderName.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(colors.accentPurple, colors.accentCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MusicCloud",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (targetFolderName.isNotBlank()) "Google Drive • $targetFolderName" else "Google Drive Streamer (BYOK)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync Button
                    if (authState is AuthState.SignedIn) {
                        IconButton(
                            onClick = { viewModel.startSync() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync",
                                tint = if (syncState is SyncState.Syncing) colors.accentCyan else colors.textPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(if (syncState is SyncState.Syncing) syncRotation else 0f)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Settings / Profile Avatar / Menu
                    Box {
                        when (val state = authState) {
                            is AuthState.SignedIn -> {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentPurple.copy(alpha = 0.3f))
                                        .border(1.dp, colors.accentPurple, CircleShape)
                                        .clickable { showMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.user.displayName.firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (colors.isDark) Color.White else colors.accentPurple
                                    )
                                }
                            }
                            else -> {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(colors.surfaceElevated)
                        ) {
                            if (authState is AuthState.SignedIn) {
                                val user = (authState as AuthState.SignedIn).user
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(user.displayName, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                            Text(user.email, color = colors.textSecondary, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {}
                                )
                            }

                            // Theme Selector Menu Item
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Тема оформления", color = colors.textPrimary)
                                        Text(
                                            text = when (themeMode) {
                                                ThemeMode.SYSTEM -> "Системная"
                                                ThemeMode.DARK -> "Тёмная"
                                                ThemeMode.LIGHT -> "Светлая"
                                            },
                                            color = colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (themeMode) {
                                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                            ThemeMode.DARK -> Icons.Default.DarkMode
                                            ThemeMode.LIGHT -> Icons.Default.LightMode
                                        },
                                        contentDescription = null,
                                        tint = colors.accentPurple
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.setThemeDialogVisible(true)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Ключи API (BYOK)", color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = colors.accentCyan) },
                                onClick = {
                                    showMenu = false
                                    viewModel.setByokSettingsVisible(true)
                                }
                            )

                            if (authState is AuthState.SignedIn) {
                                DropdownMenuItem(
                                    text = { Text("Выйти из аккаунта", color = colors.accentPink) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = colors.accentPink) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.signOut(clearKeys = false)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Сбросить ключи и выйти", color = colors.accentPink) },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = colors.accentPink) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.signOut(clearKeys = true)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Sync status message banner
            when (val state = syncState) {
                is SyncState.Syncing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.accentCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = colors.accentCyan,
                            trackColor = colors.border.copy(alpha = 0.2f)
                        )
                    }
                }
                is SyncState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                else -> Unit
            }

            // Search Bar & Filter Chips
            if (authState is AuthState.SignedIn) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search songs, artists, albums...", color = colors.textTertiary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colors.textSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.accentPurple,
                        unfocusedBorderColor = colors.border.copy(alpha = if (colors.isDark) 0.15f else 0.6f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // Filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentFilter == TrackFilter.ALL,
                        onClick = { viewModel.setFilter(TrackFilter.ALL) },
                        label = { Text("All Tracks ($allTracksCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPurple,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surfaceVariant,
                            labelColor = colors.textSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = currentFilter == TrackFilter.FAVORITES,
                        onClick = { viewModel.setFilter(TrackFilter.FAVORITES) },
                        label = { Text("Favorites ($favoriteTracksCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPink,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surfaceVariant,
                            labelColor = colors.textSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Track List / Empty State
            when (val state = authState) {
                is AuthState.SignedOut, is AuthState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border.copy(alpha = if (colors.isDark) 0.15f else 0.8f), RoundedCornerShape(24.dp))
                                .padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(colors.accentPurple, colors.accentCyan))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Войти в Google Drive (OAuth 2.0)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Потоковое воспроизведение музыки из облака Google Drive с поддержкой собственных ключей (BYOK).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )

                            if (state is AuthState.Error) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Client ID Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant)
                                    .border(1.dp, colors.border.copy(alpha = if (colors.isDark) 0.15f else 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setByokSettingsVisible(true) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isAndroid = webClientId.contains("s3f6tr")
                                    val isShort = webClientId.length < 65 && webClientId.isNotBlank()
                                    val isBad = isAndroid || isShort
                                    Icon(
                                        imageVector = if (webClientId.isNotBlank() && !isBad) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (webClientId.isNotBlank() && !isBad) SuccessGreen else colors.accentPink,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            webClientId.isBlank() -> "Ключи не заданы (нажмите для ввода)"
                                            isAndroid -> "⚠️ Ошибка: введен Android ID! Нужен Web ID"
                                            isShort -> "⚠️ Не хватает символов в ID (${webClientId.length} из 72)"
                                            else -> {
                                                val middle = webClientId.substringAfter("-").take(6)
                                                "Web ID: ...$middle... (${webClientId.length} симв.)"
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = if (webClientId.isNotBlank() && !isBad) colors.textSecondary else colors.accentPink
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (webClientId.isBlank() || clientSecret.isBlank()) {
                                        viewModel.setByokSettingsVisible(true)
                                    } else {
                                        viewModel.startOAuthFlow(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentPurple),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Авторизоваться через Google", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { viewModel.setByokSettingsVisible(true) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentCyan),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Настроить ключи (Client ID & Secret)", fontSize = 13.sp)
                            }
                        }
                    }
                }
                is AuthState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accentPurple)
                    }
                }
                is AuthState.SignedIn -> {
                    if (tracks.isEmpty() && syncState !is SyncState.Syncing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentFilter == TrackFilter.FAVORITES) {
                                // Favorites empty state
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentPink.copy(alpha = if (colors.isDark) 0.15f else 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = colors.accentPink,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Ничего не найдено в избранном" else "В избранном пока пусто",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Попробуйте изменить поисковый запрос" else "Нажмите на сердечко у любого трека, чтобы добавить его сюда",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.setFilter(TrackFilter.ALL) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentPurple),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Все треки ($allTracksCount)", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                // All tracks empty state
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No matching tracks" else "No audio files indexed yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap 'Sync Drive' to scan your Google Drive for audio files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textTertiary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.startSync() },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentPurple),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sync Now", color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = if (playerState.currentTrack != null) 90.dp else 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = tracks,
                                key = { it.id }
                            ) { track ->
                                TrackItem(
                                    track = track,
                                    isCurrentTrack = playerState.currentTrack?.id == track.id,
                                    isPlaying = playerState.isPlaying,
                                    onClick = { viewModel.playTrack(track) },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mini player anchored at bottom
        MiniPlayer(
            playerState = playerState,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onSkipNextClick = { viewModel.skipToNext() },
            onExpandClick = { viewModel.setFullPlayerVisible(true) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // Full player modal
        if (isFullPlayerVisible && playerState.currentTrack != null) {
            FullPlayerSheet(
                playerState = playerState,
                onDismissRequest = { viewModel.setFullPlayerVisible(false) },
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onSkipNextClick = { viewModel.skipToNext() },
                onSkipPreviousClick = { viewModel.skipToPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onMoveTrackInQueue = { from, to -> viewModel.moveTrackInQueue(from, to) },
                onRemoveTrackFromQueue = { viewModel.removeTrackFromQueue(it) },
                onPlayQueueItem = { viewModel.playQueueItem(it) }
            )
        }

        // BYOK Settings Dialog
        if (isByokSettingsVisible) {
            ByokSettingsDialog(
                initialClientId = webClientId,
                initialClientSecret = clientSecret,
                initialTargetFolder = targetFolderName,
                onSaveKeys = { id, secret, folder ->
                    viewModel.saveByokKeys(id, secret, folder)
                    viewModel.setByokSettingsVisible(false)
                    viewModel.startSync()
                },
                onResetKeys = {
                    viewModel.resetByokKeys()
                },
                onDismissRequest = {
                    viewModel.setByokSettingsVisible(false)
                }
            )
        }

        // Theme Selection Dialog
        if (isThemeDialogVisible) {
            ThemeSelectionDialog(
                currentMode = themeMode,
                onSelectMode = { viewModel.setThemeMode(it) },
                onDismissRequest = { viewModel.setThemeDialogVisible(false) }
            )
        }
    }
}
