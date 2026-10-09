package com.max.musiccloud

import android.app.Application
import com.max.musiccloud.data.auth.AuthRepository
import com.max.musiccloud.data.local.AppDatabase
import com.max.musiccloud.data.metadata.MetadataExtractor
import com.max.musiccloud.data.remote.DriveRepository
import com.max.musiccloud.playback.PlaybackConnection

class MusicCloudApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var driveRepository: DriveRepository
        private set

    lateinit var metadataExtractor: MetadataExtractor
        private set

    lateinit var playbackConnection: PlaybackConnection
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        authRepository = AuthRepository(this)
        driveRepository = DriveRepository(authRepository, database.trackDao())
        metadataExtractor = MetadataExtractor(this, authRepository, database.trackDao())
        playbackConnection = PlaybackConnection(this)
    }

    companion object {
        lateinit var instance: MusicCloudApplication
            private set
    }
}
