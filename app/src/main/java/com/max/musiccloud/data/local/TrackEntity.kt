package com.max.musiccloud.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String, // Google Drive file ID
    val title: String,
    val artist: String,
    val album: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val durationMs: Long = 0L,
    val folderId: String? = null,
    val folderName: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val coverArtPath: String? = null,
    val thumbnailUrl: String? = null,
    val isMetadataParsed: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return ""
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return "%.1f MB".format(mb)
        }

    val streamUrl: String
        get() = "https://www.googleapis.com/drive/v3/files/$id?alt=media"
}
