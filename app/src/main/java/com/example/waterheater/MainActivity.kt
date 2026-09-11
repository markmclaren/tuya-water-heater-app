package com.example.waterheater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.waterheater.ui.WaterHeaterViewModel
import com.example.waterheater.ui.screens.HomeScreen
import com.example.waterheater.ui.screens.RelayStatusDialog
import com.example.waterheater.ui.screens.SchedulesScreen
import com.example.waterheater.ui.screens.SettingsDialog
import com.example.waterheater.ui.theme.AmberFirePrimary
import com.example.waterheater.ui.theme.DeepBackground
import com.example.waterheater.ui.theme.SurfaceCard
import com.example.waterheater.ui.theme.TextMuted
import com.example.waterheater.ui.theme.TextPrimary
import com.example.waterheater.ui.theme.WaterHeaterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WaterHeaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaterHeaterTheme {
                val state by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedNavTab by remember { mutableStateOf(0) } // 0: Boost (Home), 1: Schedules

                LaunchedEffect(state.userNotification) {
                    state.userNotification?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearNotification()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceCard,
                            contentColor = AmberFirePrimary
                        ) {
                            NavigationBarItem(
                                selected = selectedNavTab == 0,
                                onClick = { selectedNavTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Boost"
                                    )
                                },
                                label = { Text("Boost") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmberFirePrimary,
                                    selectedTextColor = AmberFirePrimary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = DeepBackground
                                )
                            )
                            NavigationBarItem(
                                selected = selectedNavTab == 1,
                                onClick = { selectedNavTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "Schedules"
                                    )
                                },
                                label = { Text("Schedules") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmberFirePrimary,
                                    selectedTextColor = AmberFirePrimary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = DeepBackground
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = DeepBackground
                    ) {
                        if (selectedNavTab == 0) {
                            HomeScreen(
                                state = state,
                                onRefresh = { viewModel.refreshStatus() },
                                onSliderMinutesChanged = { viewModel.onSliderMinutesChanged(it) },
                                onApplyPreset = { viewModel.applyPreset(it) },
                                onStartCustomBoost = { viewModel.startCustomBoost() },
                                onAddExtraMinutes = { viewModel.addExtraBoostMinutes(it) },
                                onStopHeating = { viewModel.stopHeating() },
                                onOpenRelayDialog = { viewModel.showRelayDialog(true) },
                                onOpenSettingsDialog = { viewModel.showSettingsDialog(true) }
                            )
                        } else {
                            SchedulesScreen(
                                state = state,
                                onRefresh = { viewModel.refreshStatus() }
                            )
                        }
                    }

                    if (state.isRelayDialogVisible) {
                        RelayStatusDialog(
                            currentMode = state.status.relayStatus,
                            onSelectMode = { viewModel.setRelayMode(it) },
                            onDismiss = { viewModel.showRelayDialog(false) }
                        )
                    }

                    if (state.isSettingsDialogVisible) {
                        SettingsDialog(
                            config = state.tankConfig,
                            credentials = state.tuyaCredentials,
                            selectedRegion = state.selectedRegion,
                            onSaveTankConfig = { vol, kw, region ->
                                viewModel.updateTankConfig(vol, kw, region)
                            },
                            onSaveCredentials = { clientId, secret, deviceId, regionUrl ->
                                viewModel.updateTuyaCredentials(clientId, secret, deviceId, regionUrl)
                            },
                            onDismiss = { viewModel.showSettingsDialog(false) }
                        )
                    }
                }
            }
        }
    }
}
