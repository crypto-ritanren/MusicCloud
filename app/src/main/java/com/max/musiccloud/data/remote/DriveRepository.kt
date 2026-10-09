package com.max.musiccloud.data.remote

import android.util.Log
import com.max.musiccloud.data.auth.AuthRepository
import com.max.musiccloud.data.local.TrackDao
import com.max.musiccloud.data.local.TrackEntity
import com.max.musiccloud.data.metadata.AudioTagSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val currentCount: Int, val message: String = "Scanning Google Drive...") : SyncState
    data class Success(val totalTracks: Int) : SyncState
    data class Error(val message: String) : SyncState
}

class DriveRepository(
    private val authRepository: AuthRepository,
    private val trackDao: TrackDao
) {

    companion object {
        private const val TAG = "DriveRepository"
        private const val BASE_URL = "https://www.googleapis.com/"
    }

    private val apiService: DriveApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .dns(ResilientDns)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriveApiService::class.java)
    }

    fun syncTracks(): Flow<SyncState> = flow {
        emit(SyncState.Syncing(currentCount = 0, message = "Authenticating with Google Drive..."))

        var token = authRepository.getValidAccessToken()
        if (token == null) {
            emit(SyncState.Error("Not signed in or authorization expired. Please sign in."))
            return@flow
        }

        try {
            val targetFolderName = authRepository.targetFolderName.value.trim()
            emit(
                SyncState.Syncing(
                    currentCount = 0,
                    message = if (targetFolderName.isNotBlank()) "Searching for '$targetFolderName'..." else "Loading folders..."
                )
            )

            val allFolders = mutableListOf<DriveFileItem>()
            var folderPageToken: String? = null
            do {
                try {
                    val folderResponse = apiService.listFolders(
                        authorization = "Bearer $token",
                        pageToken = folderPageToken
                    )
                    allFolders.addAll(folderResponse.files)
                    folderPageToken = folderResponse.nextPageToken
                } catch (e: Exception) {
                    Log.w(TAG, "Optional folder listing error: ${e.message}")
                    break
                }
            } while (!folderPageToken.isNullOrBlank())

            val folderMap = allFolders.associate { it.id to it.name }
            val targetFolderIds = mutableSetOf<String>()

            if (targetFolderName.isNotBlank()) {
                val matchingRoots = allFolders.filter {
                    it.name.trim().equals(targetFolderName, ignoreCase = true)
                }

                if (matchingRoots.isEmpty()) {
                    Log.w(TAG, "Target folder '$targetFolderName' not found among ${allFolders.size} folders")
                    emit(SyncState.Error("Папка '$targetFolderName' не найдена на Google Диске! Пожалуйста, создайте папку '$targetFolderName' и добавьте музыку туда."))
                    return@flow
                }

                targetFolderIds.addAll(matchingRoots.map { it.id })
                // Collect all nested subfolders inside target folder recursively
                var expanded = true
                while (expanded) {
                    expanded = false
                    for (folder in allFolders) {
                        if (folder.parents?.any { it in targetFolderIds } == true) {
                            if (targetFolderIds.add(folder.id)) {
                                expanded = true
                            }
                        }
                    }
                }
                Log.d(TAG, "Target folder '$targetFolderName' resolved with ${targetFolderIds.size} total folders (including subfolders)")
            }

            emit(SyncState.Syncing(currentCount = 0, message = "Scanning audio files..."))

            val discoveredTracks = mutableListOf<TrackEntity>()
            var nextPageToken: String? = null
            var page = 1

            // Build query if target folders are specified and small enough for URL length limit
            val audioQuery = if (targetFolderIds.isNotEmpty() && targetFolderIds.size <= 25) {
                val parentsFilter = targetFolderIds.joinToString(" or ") { "'$it' in parents" }
                "($parentsFilter) and mimeType contains 'audio/' and trashed = false"
            } else {
                "mimeType contains 'audio/' and trashed = false"
            }

            do {
                val response = try {
                    apiService.listAudioFiles(
                        authorization = "Bearer $token",
                        query = audioQuery,
                        pageToken = nextPageToken
                    )
                } catch (httpEx: retrofit2.HttpException) {
                    if (httpEx.code() == 401) {
                        // Attempt token refresh once
                        token = authRepository.getValidAccessToken(forceRefresh = true)
                        if (token == null) throw httpEx
                        apiService.listAudioFiles(
                            authorization = "Bearer $token",
                            query = audioQuery,
                            pageToken = nextPageToken
                        )
                    } else {
                        throw httpEx
                    }
                }

                for (fileItem in response.files) {
                    // Check if file is inside target folder or any of its subfolders
                    if (targetFolderIds.isNotEmpty()) {
                        val isInsideTarget = fileItem.parents?.any { it in targetFolderIds } == true
                        if (!isInsideTarget) continue
                    }

                    // Skip tiny sound effects (< 150 KB, e.g. system notification clicks like woodblock)
                    val sizeBytes = fileItem.size?.toLongOrNull() ?: 0L
                    if (sizeBytes in 1..150_000L && fileItem.name.contains("woodblock", ignoreCase = true)) {
                        continue
                    }

                    val track = parseDriveFileToTrack(fileItem, folderMap)
                    discoveredTracks.add(track)
                }

                emit(SyncState.Syncing(currentCount = discoveredTracks.size, message = "Found ${discoveredTracks.size} tracks..."))
                nextPageToken = response.nextPageToken
                page++
            } while (!nextPageToken.isNullOrEmpty())

            // Save to database
            if (discoveredTracks.isNotEmpty()) {
                val existingTracks = trackDao.getAllTracksList().associateBy { it.id }
                val mergedTracks = discoveredTracks.map { fresh ->
                    val existing = existingTracks[fresh.id]
                    if (existing != null) {
                        val existingHasWatermark = existing.title.contains("audiovk", ignoreCase = true) ||
                                                  existing.artist.contains("audiovk", ignoreCase = true) ||
                                                  existing.title.contains("{") ||
                                                  existing.artist.contains("{")
                        val sanitizedExistingTitle = AudioTagSanitizer.clean(existing.title)
                        val sanitizedExistingArtist = AudioTagSanitizer.clean(existing.artist)

                        val keepExisting = existing.isMetadataParsed && !existingHasWatermark

                        fresh.copy(
                            title = if (keepExisting) sanitizedExistingTitle else fresh.title,
                            artist = if (keepExisting) sanitizedExistingArtist else fresh.artist,
                            album = if (keepExisting) existing.album else fresh.album,
                            durationMs = if (existing.durationMs > 0) existing.durationMs else fresh.durationMs,
                            coverArtPath = existing.coverArtPath,
                            thumbnailUrl = fresh.thumbnailUrl ?: existing.thumbnailUrl,
                            isFavorite = existing.isFavorite,
                            isMetadataParsed = keepExisting
                        )
                    } else {
                        fresh
                    }
                }
                trackDao.insertAll(mergedTracks)
                // Remove tracks that were deleted on Drive
                val validIds = discoveredTracks.map { it.id }
                trackDao.deleteMissing(validIds)
            } else {
                trackDao.clearAll()
            }

            emit(SyncState.Success(discoveredTracks.size))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tracks from Google Drive", e)
            emit(SyncState.Error(e.localizedMessage ?: "Failed to scan Google Drive"))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseDriveFileToTrack(
        fileItem: DriveFileItem,
        folderMap: Map<String, String>
    ): TrackEntity {
        val rawName = fileItem.name
        val rawWithoutExt = rawName.substringBeforeLast('.') // remove extension
        val cleanName = AudioTagSanitizer.clean(rawWithoutExt)

        var artist = "Unknown Artist"
        var title = cleanName
        var album = "Google Drive"

        // Check if parent folder gives album / artist context
        val parentFolderId = fileItem.parents?.firstOrNull()
        val parentFolderName = parentFolderId?.let { folderMap[it] }
        if (!parentFolderName.isNullOrBlank()) {
            album = parentFolderName
        }

        // Parse "Artist - Title" format
        if (cleanName.contains(" - ")) {
            val parts = cleanName.split(" - ", limit = 2)
            if (parts.size == 2) {
                artist = AudioTagSanitizer.clean(parts[0])
                title = AudioTagSanitizer.clean(parts[1])
            }
        } else if (cleanName.contains(" – ")) { // en-dash
            val parts = cleanName.split(" – ", limit = 2)
            if (parts.size == 2) {
                artist = AudioTagSanitizer.clean(parts[0])
                title = AudioTagSanitizer.clean(parts[1])
            }
        } else if (rawWithoutExt.contains(" - ")) {
            val parts = rawWithoutExt.split(" - ", limit = 2)
            if (parts.size == 2) {
                artist = AudioTagSanitizer.clean(parts[0])
                title = AudioTagSanitizer.clean(parts[1])
            }
        }

        title = AudioTagSanitizer.clean(title)
        if (title.isBlank()) {
            title = cleanName.ifBlank { rawWithoutExt }
        }
        if (artist.isBlank()) {
            artist = "Unknown Artist"
        }

        val sizeBytes = fileItem.size?.toLongOrNull() ?: 0L
        val modifiedTime = parseIsoDate(fileItem.modifiedTime)

        return TrackEntity(
            id = fileItem.id,
            title = title,
            artist = artist,
            album = album,
            mimeType = fileItem.mimeType,
            sizeBytes = sizeBytes,
            modifiedTime = modifiedTime,
            folderId = parentFolderId,
            folderName = parentFolderName,
            thumbnailUrl = fileItem.thumbnailLink,
            isMetadataParsed = false
        )
    }

    private fun parseIsoDate(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.parse(isoString.substringBefore('.'))?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
