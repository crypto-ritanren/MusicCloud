package com.max.musiccloud.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.max.musiccloud.MusicCloudApplication
import com.max.musiccloud.data.auth.AuthState
import com.max.musiccloud.data.local.TrackEntity
import com.max.musiccloud.data.remote.SyncState
import com.max.musiccloud.playback.PlayerState
import com.max.musiccloud.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrackFilter {
    ALL, FAVORITES
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicCloudApplication
    private val authRepo = app.authRepository
    private val trackDao = app.database.trackDao()
    private val driveRepo = app.driveRepository
    private val playback = app.playbackConnection

    val authState: StateFlow<AuthState> = authRepo.authState
    val playerState: StateFlow<PlayerState> = playback.playerState

    val webClientId: StateFlow<String> = authRepo.webClientId
    val clientSecret: StateFlow<String> = authRepo.clientSecret
    val targetFolderName: StateFlow<String> = authRepo.targetFolderName
    val themeMode: StateFlow<ThemeMode> = authRepo.themeMode

    private val _isThemeDialogVisible = MutableStateFlow(false)
    val isThemeDialogVisible: StateFlow<Boolean> = _isThemeDialogVisible.asStateFlow()

    private val _isByokSettingsVisible = MutableStateFlow(false)
    val isByokSettingsVisible: StateFlow<Boolean> = _isByokSettingsVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(TrackFilter.ALL)
    val currentFilter: StateFlow<TrackFilter> = _currentFilter.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    val allTracksCount: StateFlow<Int> = trackDao.getAllTracks()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val favoriteTracksCount: StateFlow<Int> = trackDao.getFavoriteTracks()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val tracks: StateFlow<List<TrackEntity>> = combine(
        _searchQuery,
        _currentFilter
    ) { query, filter -> Pair(query, filter) }
        .flatMapLatest { (query, filter) ->
            when (filter) {
                TrackFilter.FAVORITES -> {
                    trackDao.getFavoriteTracks().map { list ->
                        if (query.isBlank()) list
                        else list.filter {
                            it.title.contains(query.trim(), ignoreCase = true) ||
                            it.artist.contains(query.trim(), ignoreCase = true) ||
                            it.album.contains(query.trim(), ignoreCase = true)
                        }
                    }
                }
                TrackFilter.ALL -> {
                    if (query.isBlank()) {
                        trackDao.getAllTracks()
                    } else {
                        trackDao.searchTracks(query.trim())
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Instantly remove watermarks ({audiovk.com}) from cached database tracks
            app.metadataExtractor.cleanStoredTracks()
            // Enrich unparsed tracks with real ID3 tags and album covers in background
            app.metadataExtractor.enrichLibrary()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setFilter(filter: TrackFilter) {
        _currentFilter.value = filter
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _isFullPlayerVisible.value = visible
    }

    fun setByokSettingsVisible(visible: Boolean) {
        _isByokSettingsVisible.value = visible
    }

    fun setThemeDialogVisible(visible: Boolean) {
        _isThemeDialogVisible.value = visible
    }

    fun setThemeMode(mode: ThemeMode) {
        authRepo.setThemeMode(mode)
    }

    fun saveByokKeys(clientId: String, secret: String, targetFolder: String = authRepo.targetFolderName.value) {
        authRepo.saveByokKeys(clientId, secret, targetFolder)
    }

    fun setTargetFolderName(folderName: String) {
        authRepo.saveTargetFolder(folderName)
    }

    fun resetByokKeys() {
        authRepo.resetByokKeys()
    }

    fun startOAuthFlow(context: Context) {
        authRepo.startOAuthFlow(context)
    }

    fun handleAuthCode(code: String, redirectUri: String) {
        viewModelScope.launch {
            authRepo.exchangeCode(code, redirectUri)
        }
    }

    fun startSync() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            driveRepo.syncTracks().collect { state ->
                _syncState.value = state
                if (state is SyncState.Success) {
                    enrichUnparsedTracks()
                }
            }
        }
    }

    fun enrichUnparsedTracks() {
        viewModelScope.launch {
            app.metadataExtractor.enrichLibrary()
        }
    }

    fun playTrack(track: TrackEntity) {
        val currentList = tracks.value
        playback.playTrack(track, currentList)
    }

    fun togglePlayPause() {
        playback.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playback.seekTo(positionMs)
    }

    fun skipToNext() {
        playback.skipToNext()
    }

    fun skipToPrevious() {
        playback.skipToPrevious()
    }

    fun toggleShuffle() {
        playback.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playback.cycleRepeatMode()
    }

    fun moveTrackInQueue(fromIndex: Int, toIndex: Int) {
        playback.moveTrackInQueue(fromIndex, toIndex)
    }

    fun removeTrackFromQueue(index: Int) {
        playback.removeTrackFromQueue(index)
    }

    fun playQueueItem(index: Int) {
        playback.playQueueItem(index)
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            val updated = track.copy(isFavorite = !track.isFavorite)
            trackDao.updateTrack(updated)
        }
    }

    fun signOut(clearKeys: Boolean = false) {
        viewModelScope.launch {
            authRepo.signOut(clearKeys = clearKeys)
            trackDao.clearAll()
        }
    }
}
