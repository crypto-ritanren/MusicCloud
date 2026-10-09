package com.max.musiccloud.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.max.musiccloud.data.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import java.io.File

@OptIn(UnstableApi::class)
object DownloadCacheHolder {

    private const val MAX_CACHE_BYTES = 750L * 1024L * 1024L // 750 MB LRU cache
    private const val CACHE_DIR_NAME = "exoplayer_music_cache"

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    /**
     * Builds a CacheDataSource.Factory that automatically pulls a valid Google Drive Bearer token
     * and streams with HTTP Range requests (saving data & enabling instant playback).
     */
    fun createDataSourceFactory(
        context: Context,
        authRepository: AuthRepository
    ): DataSource.Factory {
        val cache = getCache(context)

        // Custom upstream factory injecting dynamic OAuth Bearer header
        val upstreamFactory = DataSource.Factory {
            val token = runBlocking { authRepository.getValidAccessToken() }
            val httpDataSource = DefaultHttpDataSource.Factory()
                .setUserAgent("MusicCloud-Android/1.0")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)
                .setAllowCrossProtocolRedirects(true)

            if (!token.isNullOrBlank()) {
                httpDataSource.setDefaultRequestProperties(
                    mapOf("Authorization" to "Bearer $token")
                )
            }
            httpDataSource.createDataSource()
        }

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
