package com.bestboy.sounds

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bestboy.sounds.ads.AdMobManager
import com.bestboy.sounds.service.BestboyMediaService
import com.bestboy.sounds.ui.components.MiniPlayer
import com.bestboy.sounds.ui.components.BannerAd
import com.bestboy.sounds.ui.screens.HomeScreen
import com.bestboy.sounds.ui.screens.NowPlayingScreen
import com.bestboy.sounds.ui.screens.PlaylistsScreen
import com.bestboy.sounds.ui.screens.SettingsScreen
import com.bestboy.sounds.ui.theme.BestboySoundsTheme
import com.bestboy.sounds.ui.theme.CardPurple
import com.bestboy.sounds.ui.theme.DeepBlack
import com.bestboy.sounds.ui.theme.ElectricViolet
import com.bestboy.sounds.ui.theme.TextMuted
import com.bestboy.sounds.ui.theme.TextPrimary
import com.bestboy.sounds.ui.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicPlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (audioGranted) {
            viewModel.loadTracks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        val serviceIntent = Intent(this, BestboyMediaService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        AdMobManager.loadInterstitialAd(this)
        AdMobManager.loadRewardedAd(this)

        setContent {
            BestboySoundsTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showNowPlaying by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepBlack,
                    bottomBar = {
                        Column {
                            if (uiState.currentTrack != null) {
                                val progress = if (uiState.durationMs > 0) {
                                    (uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                MiniPlayer(
                                    currentTrack = uiState.currentTrack,
                                    isPlaying = uiState.isPlaying,
                                    progress = progress,
                                    onPlayPauseClick = { viewModel.togglePlayPause() },
                                    onNextClick = { viewModel.skipNext(this@MainActivity) },
                                    onClick = { showNowPlaying = true }
                                )
                            }

                            BannerAd(modifier = Modifier.fillMaxWidth())
                            NavigationBar(
                                containerColor = CardPurple,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = uiState.activeTab == 0,
                                    onClick = { viewModel.setActiveTab(0) },
                                    icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Tracks") },
                                    label = { Text("Tracks") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepBlack,
                                        selectedTextColor = ElectricViolet,
                                        indicatorColor = ElectricViolet,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = uiState.activeTab == 1,
                                    onClick = { viewModel.setActiveTab(1) },
                                    icon = { Icon(Icons.Rounded.PlaylistPlay, contentDescription = "Playlists") },
                                    label = { Text("Playlists") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepBlack,
                                        selectedTextColor = ElectricViolet,
                                        indicatorColor = ElectricViolet,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = uiState.activeTab == 2,
                                    onClick = { viewModel.setActiveTab(2) },
                                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepBlack,
                                        selectedTextColor = ElectricViolet,
                                        indicatorColor = ElectricViolet,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (uiState.activeTab) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onTrackClick = { showNowPlaying = true }
                            )
                            1 -> PlaylistsScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                            2 -> SettingsScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                        }

                        AnimatedVisibility(
                            visible = showNowPlaying && uiState.currentTrack != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            NowPlayingScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onBackClick = { showNowPlaying = false }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
