package com.max.musiccloud.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.max.musiccloud.BuildConfig
import com.max.musiccloud.data.remote.ResilientDns
import com.max.musiccloud.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class UserProfile(
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: UserProfile) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("musiccloud_byok_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AuthRepository"

        const val DRIVE_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        const val EMAIL_SCOPE = "email"
        const val PROFILE_SCOPE = "profile"

        const val DEFAULT_WEB_CLIENT_ID = ""
        const val DEFAULT_CLIENT_SECRET = ""
        const val DEFAULT_REDIRECT_URI = "http://localhost:8888/callback"
        const val CUSTOM_SCHEME_REDIRECT_URI = "com.max.musiccloud://oauth2callback"

        private const val KEY_CLIENT_ID = "byok_client_id"
        private const val KEY_CLIENT_SECRET = "byok_client_secret"
        private const val KEY_TARGET_FOLDER = "drive_target_folder"
        const val DEFAULT_TARGET_FOLDER = "User Music"
        private const val KEY_ACCESS_TOKEN = "oauth_access_token"
        private const val KEY_REFRESH_TOKEN = "oauth_refresh_token"
        private const val KEY_EXPIRES_AT = "oauth_expires_at"
        private const val KEY_USER_EMAIL = "oauth_user_email"
        private const val KEY_USER_NAME = "oauth_user_name"
        private const val KEY_USER_PHOTO = "oauth_user_photo"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _webClientId = MutableStateFlow(
        prefs.getString(KEY_CLIENT_ID, DEFAULT_WEB_CLIENT_ID) ?: DEFAULT_WEB_CLIENT_ID
    )
    val webClientId: StateFlow<String> = _webClientId.asStateFlow()

    private val _clientSecret = MutableStateFlow(
        prefs.getString(KEY_CLIENT_SECRET, DEFAULT_CLIENT_SECRET) ?: DEFAULT_CLIENT_SECRET
    )
    val clientSecret: StateFlow<String> = _clientSecret.asStateFlow()

    private val _targetFolderName = MutableStateFlow(
        prefs.getString(KEY_TARGET_FOLDER, DEFAULT_TARGET_FOLDER) ?: DEFAULT_TARGET_FOLDER
    )
    val targetFolderName: StateFlow<String> = _targetFolderName.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val exchangeMutex = Mutex()
    private var lastExchangedCode: String? = null
    private var localServer: OAuthLocalServer? = null
    private var pendingCodeVerifier: String? = null
    private var pendingRedirectUri: String = DEFAULT_REDIRECT_URI

    private val oauthApi: OAuthApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .dns(ResilientDns)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OAuthApiService::class.java)
    }

    private val userinfoApi: OAuthApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .dns(ResilientDns)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OAuthApiService::class.java)
    }

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val name = prefs.getString(KEY_USER_NAME, null)
        val photo = prefs.getString(KEY_USER_PHOTO, null)

        if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
            _authState.value = AuthState.SignedIn(
                UserProfile(
                    email = email,
                    displayName = name ?: email,
                    photoUrl = photo
                )
            )
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    fun sanitizeKey(raw: String): String {
        return raw
            .replace("\uFEFF", "") // BOM
            .replace("\u200B", "") // Zero width space
            .replace("\u200C", "") // Zero width non-joiner
            .replace("\u200D", "") // Zero width joiner
            .replace("\u00A0", "") // Non-breaking space
            .replace("\u202F", "") // Narrow no-break space
            .replace("\u205F", "") // Medium mathematical space
            .replace("\u3000", "") // Ideographic space
            .replace(Regex("\\s+"), "") // All standard whitespace
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
    }

    fun saveByokKeys(clientId: String, secret: String, targetFolder: String = _targetFolderName.value) {
        val cleanId = sanitizeKey(clientId)
        val cleanSecret = sanitizeKey(secret)
        val cleanFolder = targetFolder.trim()
        prefs.edit()
            .putString(KEY_CLIENT_ID, cleanId)
            .putString(KEY_CLIENT_SECRET, cleanSecret)
            .putString(KEY_TARGET_FOLDER, cleanFolder)
            .apply()
        _webClientId.value = cleanId
        _clientSecret.value = cleanSecret
        _targetFolderName.value = cleanFolder
        Log.d(TAG, "Saved BYOK keys & target folder '$cleanFolder'")
    }

    fun saveTargetFolder(targetFolder: String) {
        val clean = targetFolder.trim()
        prefs.edit().putString(KEY_TARGET_FOLDER, clean).apply()
        _targetFolderName.value = clean
        Log.d(TAG, "Saved target folder '$clean'")
    }

    fun resetByokKeys() {
        prefs.edit()
            .putString(KEY_CLIENT_ID, DEFAULT_WEB_CLIENT_ID)
            .putString(KEY_CLIENT_SECRET, DEFAULT_CLIENT_SECRET)
            .apply()
        _webClientId.value = DEFAULT_WEB_CLIENT_ID
        _clientSecret.value = DEFAULT_CLIENT_SECRET
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        Log.d(TAG, "Saved theme mode '$mode'")
    }

    /**
     * Launches standard OAuth 2.0 flow via Chrome Custom Tabs and local loopback server.
     */
    fun startOAuthFlow(context: Context) {
        val clientId = sanitizeKey(_webClientId.value.ifBlank { DEFAULT_WEB_CLIENT_ID })
        if (clientId.isBlank()) {
            _authState.value = AuthState.Error("Web Client ID is empty. Please configure it in settings.")
            return
        }

        // Generate PKCE code_verifier & code_challenge
        val verifier = generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)
        pendingRedirectUri = DEFAULT_REDIRECT_URI

        // Start local server to capture redirect
        localServer?.stop()
        localServer = OAuthLocalServer(
            port = 8888,
            onCodeReceived = { code, redirectUri ->
                scope.launch {
                    exchangeCode(code, redirectUri)
                }
            },
            onError = { error ->
                _authState.value = AuthState.Error("OAuth error: $error")
            }
        ).also { it.start(scope) }

        val scopeString = listOf(DRIVE_READONLY_SCOPE, EMAIL_SCOPE, PROFILE_SCOPE).joinToString(" ")

        val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", DEFAULT_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopeString)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, authUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Custom Tabs launch error, falling back to browser Intent", e)
            val intent = Intent(Intent.ACTION_VIEW, authUrl).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Exchange auth code (received from loopback server or custom scheme deep link)
     */
    suspend fun exchangeCode(code: String, redirectUri: String) = withContext(Dispatchers.IO) {
        if (_authState.value is AuthState.SignedIn) {
            Log.d(TAG, "Already signed in, ignoring redundant code exchange")
            return@withContext
        }
        if (code.isBlank() || code == lastExchangedCode) {
            Log.d(TAG, "Code already processed or blank, skipping duplicate exchange")
            return@withContext
        }

        exchangeMutex.withLock {
            if (_authState.value is AuthState.SignedIn) {
                Log.d(TAG, "Already signed in while waiting for lock, ignoring")
                return@withContext
            }
            if (code == lastExchangedCode) {
                Log.d(TAG, "Code was exchanged while waiting for lock, skipping")
                return@withContext
            }
            lastExchangedCode = code

            localServer?.stop()
            _authState.value = AuthState.Loading

            val clientId = _webClientId.value
            val secret = _clientSecret.value
            val verifier = pendingCodeVerifier

            var attempts = 0
            var lastError: Exception? = null

            while (attempts < 3) {
                attempts++
                try {
                    Log.d(TAG, "Exchanging auth code (attempt $attempts/3)...")
                    val tokenResponse = oauthApi.exchangeAuthorizationCode(
                        clientId = clientId,
                        clientSecret = secret,
                        code = code,
                        redirectUri = redirectUri,
                        codeVerifier = verifier
                    )

                    val accessToken = tokenResponse.accessToken
                    val refreshToken = tokenResponse.refreshToken
                    val expiresInMs = tokenResponse.expiresIn * 1000L
                    val expiresAt = System.currentTimeMillis() + expiresInMs

                    // Fetch user info
                    val userInfo = try {
                        userinfoApi.fetchUserInfo("Bearer $accessToken")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch user info: ${e.message}")
                        null
                    }

                    val email = userInfo?.email ?: "user@google.com"
                    val name = userInfo?.name ?: email.substringBefore('@')
                    val photo = userInfo?.picture

                    prefs.edit().apply {
                        putString(KEY_ACCESS_TOKEN, accessToken)
                        refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
                        putLong(KEY_EXPIRES_AT, expiresAt)
                        putString(KEY_USER_EMAIL, email)
                        putString(KEY_USER_NAME, name)
                        putString(KEY_USER_PHOTO, photo)
                        apply()
                    }

                    val user = UserProfile(
                        email = email,
                        displayName = name,
                        photoUrl = photo
                    )
                    _authState.value = AuthState.SignedIn(user)
                    Log.d(TAG, "OAuth authorization code exchange successful for $email")
                    return@withContext
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Token exchange attempt $attempts failed: ${e.message}")
                    // If HTTP 400 Bad Request, code is invalid or already redeemed; retrying won't help
                    if (e is retrofit2.HttpException && e.code() == 400) {
                        break
                    }
                    if (attempts < 3) {
                        delay(1000L)
                    }
                }
            }

            Log.e(TAG, "Token exchange failed after $attempts attempts", lastError)

            // CRITICAL: If app is already signed in or has a valid stored session, DO NOT overwrite with error!
            if (_authState.value is AuthState.SignedIn) {
                Log.w(TAG, "Exchange failed but app is already SignedIn; retaining signed-in state")
                return@withContext
            }
            val existingToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            val existingEmail = prefs.getString(KEY_USER_EMAIL, null)
            if (!existingToken.isNullOrBlank() && !existingEmail.isNullOrBlank()) {
                Log.w(TAG, "Exchange failed but valid session exists in SharedPreferences; restoring session instead of error")
                restoreSession()
                return@withContext
            }

            _authState.value = AuthState.Error("Sign-in failed: ${lastError?.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Returns a valid OAuth Bearer access token, automatically refreshing it if expired.
     */
    suspend fun getValidAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val currentAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val now = System.currentTimeMillis()

        val isExpired = (now + 60_000L) >= expiresAt // refresh if within 1 min of expiry

        if (!forceRefresh && !isExpired && !currentAccessToken.isNullOrBlank()) {
            return@withContext currentAccessToken
        }

        if (refreshToken.isNullOrBlank()) {
            Log.w(TAG, "No refresh token available to renew access token")
            return@withContext currentAccessToken
        }

        // Perform token refresh
        val clientId = _webClientId.value
        val secret = _clientSecret.value

        try {
            Log.d(TAG, "Refreshing expired OAuth access token...")
            val response = oauthApi.refreshAccessToken(
                clientId = clientId,
                clientSecret = secret,
                refreshToken = refreshToken
            )

            val newAccessToken = response.accessToken
            val newExpiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)

            prefs.edit().apply {
                putString(KEY_ACCESS_TOKEN, newAccessToken)
                putLong(KEY_EXPIRES_AT, newExpiresAt)
                apply()
            }

            Log.d(TAG, "Successfully refreshed access token!")
            newAccessToken
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing OAuth token", e)
            currentAccessToken
        }
    }

    suspend fun signOut(clearKeys: Boolean = false) = withContext(Dispatchers.IO) {
        lastExchangedCode = null
        localServer?.stop()
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_AT)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_PHOTO)
            if (clearKeys) {
                remove(KEY_CLIENT_ID)
                remove(KEY_CLIENT_SECRET)
            }
            apply()
        }

        if (clearKeys) {
            _webClientId.value = DEFAULT_WEB_CLIENT_ID
            _clientSecret.value = DEFAULT_CLIENT_SECRET
        }

        _authState.value = AuthState.SignedOut
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
