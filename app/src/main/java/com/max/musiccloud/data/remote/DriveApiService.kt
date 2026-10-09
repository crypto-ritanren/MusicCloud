package com.max.musiccloud.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DriveApiService {

    @GET("drive/v3/files")
    suspend fun listAudioFiles(
        @Header("Authorization") authorization: String,
        @Query("q") query: String = "mimeType contains 'audio/' and trashed = false",
        @Query("fields") fields: String = "nextPageToken, files(id, name, mimeType, size, modifiedTime, parents, thumbnailLink)",
        @Query("pageSize") pageSize: Int = 100,
        @Query("pageToken") pageToken: String? = null
    ): DriveFileListResponse

    @GET("drive/v3/files")
    suspend fun listFolders(
        @Header("Authorization") authorization: String,
        @Query("q") query: String = "mimeType = 'application/vnd.google-apps.folder' and trashed = false",
        @Query("fields") fields: String = "nextPageToken, files(id, name, parents)",
        @Query("pageSize") pageSize: Int = 1000,
        @Query("pageToken") pageToken: String? = null
    ): DriveFileListResponse
}
