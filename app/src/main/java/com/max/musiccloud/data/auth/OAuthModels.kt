package com.max.musiccloud.data.auth

import com.google.gson.annotations.SerializedName

data class OAuthTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("scope")
    val scope: String? = null,
    @SerializedName("token_type")
    val tokenType: String = "Bearer"
)

data class GoogleUserInfoResponse(
    @SerializedName("sub")
    val sub: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("given_name")
    val givenName: String? = null,
    @SerializedName("picture")
    val picture: String? = null,
    @SerializedName("email")
    val email: String? = null
)
