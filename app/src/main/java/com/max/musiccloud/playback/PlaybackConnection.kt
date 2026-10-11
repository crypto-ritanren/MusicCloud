package com.max.musiccloud.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.max.musiccloud.data.local.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playlist: List<TrackEntity> = emptyList()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

@OptIn(UnstableApi::class)
class PlaybackConnection(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentPlaylist: List<TrackEntity> = emptyList()
    private val trackMap = mutableMapOf<String, TrackEntity>()

    private var progressJob: Job? = null

    companion object {
        private const val TAG = "PlaybackConnection"
    }

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.let { setupPlayerListener(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MediaSession", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateStateFromPlayer()
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateStateFromPlayer()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                updateStateFromPlayer()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val track = mediaId?.let { trackMap[it] }
                _playerState.update { it.copy(currentTrack = track) }
                updateStateFromPlayer()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playerState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playerState.update { it.copy(repeatMode = repeatMode) }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val currentTrack = _playerState.value.currentTrack ?: return
                val newTitle = mediaMetadata.title?.toString()?.trim()
                val newArtist = mediaMetadata.artist?.toString()?.trim()
                val newAlbum = mediaMetadata.albumTitle?.toString()?.trim()
                val artworkData = mediaMetadata.artworkData

                val app = context.applicationContext as? com.max.musiccloud.MusicCloudApplication ?: return
                scope.launch(Dispatchers.IO) {
                    var coverPath = currentTrack.coverArtPath
                    if (artworkData != null && artworkData.isNotEmpty()) {
                        coverPath = app.metadataExtractor.saveArtworkBytes(currentTrack.id, artworkData)
                    }

                    val cleanTitle = newTitle?.let { com.max.musiccloud.data.metadata.AudioTagSanitizer.clean(it) }?.takeIf { it.isNotBlank() }
                    val cleanArtist = newArtist?.let { com.max.musiccloud.data.metadata.AudioTagSanitizer.clean(it) }?.takeIf { it.isNotBlank() }
                    val cleanAlbum = newAlbum?.let { com.max.musiccloud.data.metadata.AudioTagSanitizer.clean(it) }?.takeIf { it.isNotBlank() }

                    if (cleanTitle != null || cleanArtist != null || coverPath != null) {
                        val updated = currentTrack.copy(
                            title = cleanTitle ?: currentTrack.title,
                            artist = cleanArtist ?: currentTrack.artist,
                            album = cleanAlbum ?: currentTrack.album,
                            coverArtPath = coverPath ?: currentTrack.coverArtPath,
                            isMetadataParsed = true
                        )
                        app.database.trackDao().updateTrack(updated)
                        trackMap[updated.id] = updated
                        _playerState.update { it.copy(currentTrack = updated) }
                    }
                }
            }
        })
        updateStateFromPlayer()
    }

    private fun updateStateFromPlayer() {
        val p = controller ?: return
        val currentMediaId = p.currentMediaItem?.mediaId
        val track = currentMediaId?.let { trackMap[it] } ?: _playerState.value.currentTrack

        val dur = if (p.duration > 0) p.duration else 0L
        val pos = p.currentPosition.coerceAtLeast(0L)
        val buf = p.bufferedPosition.coerceAtLeast(0L)

        _playerState.update {
            it.copy(
                currentTrack = track,
                isPlaying = p.isPlaying,
                isBuffering = p.playbackState == Player.STATE_BUFFERING,
                currentPositionMs = pos,
                durationMs = dur,
                bufferedPositionMs = buf,
                shuffleEnabled = p.shuffleModeEnabled,
                repeatMode = p.repeatMode,
                playlist = currentPlaylist
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val p = controller
                if (p != null && p.isPlaying) {
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    val dur = if (p.duration > 0) p.duration else _playerState.value.durationMs
                    val buf = p.bufferedPosition.coerceAtLeast(0L)
                    _playerState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            bufferedPositionMs = buf
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: TrackEntity, playlist: List<TrackEntity>) {
        val p = controller ?: return
        currentPlaylist = playlist
        trackMap.clear()
        playlist.forEach { trackMap[it.id] = it }

        val mediaItems = playlist.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(Uri.parse(item.streamUrl))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .build()
                )
                .build()
        }

        val startIndex = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        p.setMediaItems(mediaItems, startIndex, /* startPositionMs = */ 0L)
        p.prepare()
        p.play()

        _playerState.update {
            it.copy(
                currentTrack = track,
                playlist = playlist,
                isPlaying = true
            )
        }
    }

    fun togglePlayPause() {
        val p = controller ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_IDLE) {
                p.prepare()
            }
            p.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val p = controller ?: return
        p.seekTo(positionMs)
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipToNext() {
        val p = controller ?: return
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val p = controller ?: return
        if (p.currentPosition > 3000L || !p.hasPreviousMediaItem()) {
            p.seekTo(0L)
        } else {
            p.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val p = controller ?: return
        p.shuffleModeEnabled = !p.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val p = controller ?: return
        val nextMode = when (p.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        p.repeatMode = nextMode
    }

    fun moveTrackInQueue(fromIndex: Int, toIndex: Int) {
        val p = controller ?: return
        if (fromIndex !in currentPlaylist.indices || toIndex !in currentPlaylist.indices || fromIndex == toIndex) return

        val mutableList = currentPlaylist.toMutableList()
        val movedItem = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, movedItem)
        currentPlaylist = mutableList

        p.moveMediaItem(fromIndex, toIndex)

        _playerState.update {
            it.copy(playlist = currentPlaylist)
        }
    }

    fun removeTrackFromQueue(index: Int) {
        val p = controller ?: return
        if (index !in currentPlaylist.indices) return

        val mutableList = currentPlaylist.toMutableList()
        mutableList.removeAt(index)
        currentPlaylist = mutableList

        p.removeMediaItem(index)

        if (currentPlaylist.isEmpty()) {
            _playerState.update {
                it.copy(
                    currentTrack = null,
                    playlist = emptyList(),
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L
                )
            }
        } else {
            _playerState.update {
                it.copy(playlist = currentPlaylist)
            }
        }
    }

    fun playQueueItem(index: Int) {
        val p = controller ?: return
        if (index !in currentPlaylist.indices) return
        p.seekTo(index, 0L)
        p.play()
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
