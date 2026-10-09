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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
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
import com.max.musiccloud.ui.components.TrackItem
import com.max.musiccloud.ui.theme.AccentCyan
import com.max.musiccloud.ui.theme.AccentPink
import com.max.musiccloud.ui.theme.AccentPurple
import com.max.musiccloud.ui.theme.DarkBg
import com.max.musiccloud.ui.theme.DarkSurface
import com.max.musiccloud.ui.theme.DarkSurfaceElevated
import com.max.musiccloud.ui.theme.DarkSurfaceVariant
import com.max.musiccloud.ui.theme.SuccessGreen
import com.max.musiccloud.ui.theme.TextPrimary
import com.max.musiccloud.ui.theme.TextSecondary
import com.max.musiccloud.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsStateWithLifecycle()
    val isByokSettingsVisible by viewModel.isByokSettingsVisible.collectAsStateWithLifecycle()
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
            .background(DarkBg)
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
                            .background(Brush.linearGradient(listOf(AccentPurple, AccentCyan))),
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
                            color = TextPrimary
                        )
                        Text(
                            text = if (targetFolderName.isNotBlank()) "Google Drive • $targetFolderName" else "Google Drive Streamer (BYOK)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
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
                                tint = if (syncState is SyncState.Syncing) AccentCyan else TextPrimary,
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
                                        .background(AccentPurple.copy(alpha = 0.3f))
                                        .border(1.dp, AccentPurple, CircleShape)
                                        .clickable { showMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.user.displayName.firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
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
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            if (authState is AuthState.SignedIn) {
                                val user = (authState as AuthState.SignedIn).user
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(user.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text(user.email, color = TextSecondary, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {}
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Ключи API (BYOK)", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentCyan) },
                                onClick = {
                                    showMenu = false
                                    viewModel.setByokSettingsVisible(true)
                                }
                            )

                            if (authState is AuthState.SignedIn) {
                                DropdownMenuItem(
                                    text = { Text("Выйти из аккаунта", color = AccentPink) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AccentPink) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.signOut(clearKeys = false)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Сбросить ключи и выйти", color = AccentPink) },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AccentPink) },
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
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = AccentCyan,
                            trackColor = Color.White.copy(alpha = 0.05f)
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
                    placeholder = { Text("Search songs, artists, albums...", color = TextTertiary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
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
                        label = { Text("All Tracks (${tracks.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurple,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = currentFilter == TrackFilter.FAVORITES,
                        onClick = { viewModel.setFilter(TrackFilter.FAVORITES) },
                        label = { Text("Favorites") },
                        leadingIcon = {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPink,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
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
                                .background(DarkSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                                .padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(AccentPurple, AccentCyan))),
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
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Потоковое воспроизведение музыки из облака Google Drive с поддержкой собственных ключей (BYOK).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
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
                                    .background(DarkSurfaceVariant)
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
                                        tint = if (webClientId.isNotBlank() && !isBad) SuccessGreen else AccentPink,
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
                                        color = if (webClientId.isNotBlank() && !isBad) TextSecondary else AccentPink
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
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Авторизоваться через Google", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { viewModel.setByokSettingsVisible(true) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
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
                        CircularProgressIndicator(color = AccentPurple)
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching tracks" else "No audio files indexed yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap 'Sync Drive' to scan your Google Drive for audio files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.startSync() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync Now")
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
                onToggleFavorite = { viewModel.toggleFavorite(it) }
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
    }
}
