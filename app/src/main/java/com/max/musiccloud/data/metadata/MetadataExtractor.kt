package com.max.musiccloud.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.max.musiccloud.data.auth.AuthRepository
import com.max.musiccloud.data.remote.ResilientDns
import com.max.musiccloud.data.local.TrackDao
import com.max.musiccloud.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MetadataExtractor(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val trackDao: TrackDao
) {

    companion object {
        private const val TAG = "MetadataExtractor"
        private const val INITIAL_CHUNK_SIZE = 393216L // 384 KB
        private const val MAX_TAG_SIZE = 2097152L // 2 MB safety cap
    }

    private val coversDir: File by lazy {
        File(context.filesDir, "covers").apply { mkdirs() }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ResilientDns)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addNetworkInterceptor { chain ->
                val original = chain.request()
                val token = runBlocking { authRepository.getValidAccessToken() }
                val request = if (original.header("Authorization") == null && !token.isNullOrBlank()) {
                    original.newBuilder().header("Authorization", "Bearer $token").build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .build()
    }

    /**
     * Sanitizes all tracks currently in the database to strip residual watermarks immediately.
     */
    suspend fun cleanStoredTracks() = withContext(Dispatchers.IO) {
        val all = trackDao.getAllTracksList()
        val toUpdate = mutableListOf<TrackEntity>()
        for (track in all) {
            val cleanTitle = AudioTagSanitizer.clean(track.title)
            val cleanArtist = AudioTagSanitizer.clean(track.artist)
            val hasWatermark = track.title.contains("audiovk", ignoreCase = true) ||
                              track.artist.contains("audiovk", ignoreCase = true) ||
                              track.title.contains("{") ||
                              track.artist.contains("{")

            if (cleanTitle != track.title || cleanArtist != track.artist || hasWatermark) {
                toUpdate.add(
                    track.copy(
                        title = cleanTitle,
                        artist = cleanArtist,
                        isMetadataParsed = false // allow ID3 re-parse
                    )
                )
            }
        }
        if (toUpdate.isNotEmpty()) {
            trackDao.insertAll(toUpdate)
            Log.d(TAG, "Sanitized ${toUpdate.size} tracks with watermarks in local database")
        }
    }

    /**
     * Extracts ID3 / container metadata (title, artist, album, duration, album art)
     * via local MediaMetadataRetriever using OkHttp Range buffer.
     */
    suspend fun extractMetadata(track: TrackEntity): TrackEntity = withContext(Dispatchers.IO) {
        val hasWatermark = track.title.contains("audiovk", ignoreCase = true) ||
                          track.artist.contains("audiovk", ignoreCase = true) ||
                          track.title.contains("{")

        if (track.isMetadataParsed && track.coverArtPath != null && !hasWatermark) {
            return@withContext track
        }

        val token = authRepository.getValidAccessToken() ?: return@withContext finalizeFallback(track)
        val tempFile = File(context.cacheDir, "id3_${track.id}.tmp")
        val retriever = MediaMetadataRetriever()

        try {
            // Fetch initial 384 KB via HTTP Range request
            val request = Request.Builder()
                .url(track.streamUrl)
                .header("Authorization", "Bearer $token")
                .header("Range", "bytes=0-${INITIAL_CHUNK_SIZE - 1}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful && response.code != 206) {
                Log.w(TAG, "Range request failed for '${track.title}', HTTP ${response.code}")
                response.close()
                return@withContext finalizeFallback(track)
            }

            val body = response.body ?: return@withContext finalizeFallback(track)
            val inputStream = body.byteStream()
            val buffer = ByteArray(INITIAL_CHUNK_SIZE.toInt())
            var totalRead = 0
            while (totalRead < buffer.size) {
                val read = inputStream.read(buffer, totalRead, buffer.size - totalRead)
                if (read == -1) break
                totalRead += read
            }
            response.close()

            if (totalRead <= 0) {
                return@withContext finalizeFallback(track)
            }

            // Write first chunk to temp file
            tempFile.outputStream().use { os ->
                os.write(buffer, 0, totalRead)
            }

            // Check if this is an MP3 with an ID3v2 tag that needs more bytes (e.g. large embedded artwork)
            if (totalRead >= 10 && buffer[0] == 0x49.toByte() && buffer[1] == 0x44.toByte() && buffer[2] == 0x33.toByte()) {
                val tagSize = ((buffer[6].toInt() and 0x7F) shl 21) or
                              ((buffer[7].toInt() and 0x7F) shl 14) or
                              ((buffer[8].toInt() and 0x7F) shl 7) or
                              (buffer[9].toInt() and 0x7F)
                val totalTagSize = tagSize + 10L
                if (totalTagSize > totalRead && totalTagSize <= MAX_TAG_SIZE) {
                    try {
                        val reqExtra = Request.Builder()
                            .url(track.streamUrl)
                            .header("Authorization", "Bearer $token")
                            .header("Range", "bytes=$totalRead-${totalTagSize - 1}")
                            .build()
                        val respExtra = httpClient.newCall(reqExtra).execute()
                        if (respExtra.isSuccessful || respExtra.code == 206) {
                            respExtra.body?.byteStream()?.use { extraStream ->
                                FileOutputStream(tempFile, true).use { os ->
                                    extraStream.copyTo(os)
                                }
                            }
                        }
                        respExtra.close()
                    } catch (e: Exception) {
                        Log.d(TAG, "Optional extra ID3 fetch ignored: ${e.message}")
                    }
                }
            }

            var rawTitle: String? = null
            var rawArtist: String? = null
            var rawAlbum: String? = null
            var durationMs = track.durationMs
            var picture: ByteArray? = null

            try {
                // Parse locally with MediaMetadataRetriever
                retriever.setDataSource(tempFile.absolutePath)
                rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
                rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
                rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull()?.takeIf { it > 0 } ?: track.durationMs
                picture = retriever.embeddedPicture
            } catch (e: Exception) {
                Log.d(TAG, "MediaMetadataRetriever failed on '${track.title}', falling back to Id3Parser: ${e.message}")
            }

            // Pure Kotlin fallback if MediaMetadataRetriever failed (status = 0x80000000) or missed fields
            if (rawTitle.isNullOrBlank() || picture == null || picture.isEmpty()) {
                try {
                    val parsed = Id3Parser.parse(tempFile.readBytes())
                    if (parsed != null) {
                        if (rawTitle.isNullOrBlank() && !parsed.title.isNullOrBlank()) {
                            rawTitle = parsed.title
                        }
                        if (rawArtist.isNullOrBlank() && !parsed.artist.isNullOrBlank()) {
                            rawArtist = parsed.artist
                        }
                        if (rawAlbum.isNullOrBlank() && !parsed.album.isNullOrBlank()) {
                            rawAlbum = parsed.album
                        }
                        if ((picture == null || picture.isEmpty()) && parsed.artworkBytes != null && parsed.artworkBytes.isNotEmpty()) {
                            picture = parsed.artworkBytes
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Id3Parser exception: ${e.message}")
                }
            }

            val cleanId3Title = rawTitle?.let { AudioTagSanitizer.clean(it) }?.takeIf { it.isNotBlank() }
            val cleanId3Artist = rawArtist?.let { AudioTagSanitizer.clean(it) }?.takeIf { it.isNotBlank() }
            val cleanId3Album = rawAlbum?.let { AudioTagSanitizer.clean(it) }?.takeIf { it.isNotBlank() }

            var coverPath = track.coverArtPath
            if (picture != null && picture.isNotEmpty()) {
                val coverFile = File(coversDir, "${track.id}.jpg")
                coverFile.writeBytes(picture)
                coverPath = coverFile.absolutePath
                Log.d(TAG, "Saved ID3 cover art for '${track.title}' (${picture.size} bytes)")
            }

            val finalTitle = cleanId3Title ?: AudioTagSanitizer.clean(track.title)
            val finalArtist = cleanId3Artist ?: AudioTagSanitizer.clean(track.artist)
            val finalAlbum = cleanId3Album ?: track.album

            val updated = track.copy(
                title = finalTitle,
                artist = finalArtist,
                album = finalAlbum,
                durationMs = durationMs,
                coverArtPath = coverPath,
                isMetadataParsed = true
            )

            trackDao.updateTrack(updated)
            return@withContext updated
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract ID3 tags for '${track.title}': ${e.message}")
            return@withContext finalizeFallback(track)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
            try { tempFile.delete() } catch (_: Exception) {}
        }
    }

    private suspend fun finalizeFallback(track: TrackEntity): TrackEntity {
        val sanitized = track.copy(
            title = AudioTagSanitizer.clean(track.title),
            artist = AudioTagSanitizer.clean(track.artist),
            isMetadataParsed = true
        )
        trackDao.updateTrack(sanitized)
        return sanitized
    }

    /**
     * Saves raw artwork bytes (e.g. from ExoPlayer onMediaMetadataChanged) to disk cache.
     */
    fun saveArtworkBytes(trackId: String, artworkBytes: ByteArray): String? {
        return try {
            val coverFile = File(coversDir, "$trackId.jpg")
            coverFile.writeBytes(artworkBytes)
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving artwork bytes: ${e.message}")
            null
        }
    }

    /**
     * Sequentially extracts metadata for all tracks needing enrichment in the background.
     */
    suspend fun enrichLibrary(onProgress: ((current: Int, total: Int) -> Unit)? = null) = withContext(Dispatchers.IO) {
        val needingEnrichment = trackDao.getTracksNeedingEnrichment()
        if (needingEnrichment.isEmpty()) return@withContext

        val total = needingEnrichment.size
        var done = 0

        for (track in needingEnrichment) {
            try {
                extractMetadata(track)
            } catch (e: Exception) {
                Log.w(TAG, "Error enriching track ${track.id}: ${e.message}")
            }
            done++
            onProgress?.invoke(done, total)
        }
    }
}
