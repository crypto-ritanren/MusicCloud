package com.max.musiccloud.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.max.musiccloud.data.auth.AuthState
import com.max.musiccloud.ui.screens.LibraryScreen
import com.max.musiccloud.ui.theme.MusicCloudTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        requestNotificationPermission()
        handleOAuthIntent(intent)

        setContent {
            MusicCloudTheme {
                val authState by viewModel.authState.collectAsStateWithLifecycle()

                LaunchedEffect(authState) {
                    if (authState is AuthState.SignedIn) {
                        if (viewModel.tracks.value.isEmpty()) {
                            viewModel.startSync()
                        } else {
                            viewModel.enrichUnparsedTracks()
                        }
                    }
                }

                LibraryScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        Log.d("MainActivity", "Handling deep link URI: $uri")
        if (uri.scheme == "com.max.musiccloud" && uri.host == "oauth2callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank() && viewModel.authState.value !is AuthState.SignedIn) {
                viewModel.handleAuthCode(code, "http://localhost:8888/callback")
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val permissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ -> }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
