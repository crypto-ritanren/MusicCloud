package com.max.musiccloud.data.remote

import com.google.gson.annotations.SerializedName

data class DriveFileListResponse(
    @SerializedName("nextPageToken")
    val nextPageToken: String? = null,
    @SerializedName("files")
    val files: List<DriveFileItem> = emptyList()
)

data class DriveFileItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mimeType")
    val mimeType: String,
    @SerializedName("size")
    val size: String? = null,
    @SerializedName("modifiedTime")
    val modifiedTime: String? = null,
    @SerializedName("parents")
    val parents: List<String>? = null,
    @SerializedName("thumbnailLink")
    val thumbnailLink: String? = null
)
