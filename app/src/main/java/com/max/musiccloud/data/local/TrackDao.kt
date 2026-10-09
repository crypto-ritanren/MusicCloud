package com.max.musiccloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY artist ASC, title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("""
        SELECT * FROM tracks 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%'
        ORDER BY artist ASC, title ASC
    """)
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksList(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE isMetadataParsed = 0 ORDER BY id ASC")
    suspend fun getUnparsedTracks(): List<TrackEntity>

    @Query("""
        SELECT * FROM tracks 
        WHERE isMetadataParsed = 0 
           OR coverArtPath IS NULL 
           OR title LIKE '%audiovk%' 
           OR artist LIKE '%audiovk%'
        ORDER BY id ASC
    """)
    suspend fun getTracksNeedingEnrichment(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id NOT IN (:validIds)")
    suspend fun deleteMissing(validIds: List<String>)

    @Query("DELETE FROM tracks")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getCount(): Int
}
