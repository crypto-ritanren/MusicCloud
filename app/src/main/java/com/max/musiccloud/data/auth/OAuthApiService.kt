package com.max.musiccloud.data.auth

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OAuthApiService {

    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeAuthorizationCode(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String? = null
    ): OAuthTokenResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): OAuthTokenResponse

    @GET("oauth2/v3/userinfo")
    suspend fun fetchUserInfo(
        @Header("Authorization") authorization: String
    ): GoogleUserInfoResponse
}
