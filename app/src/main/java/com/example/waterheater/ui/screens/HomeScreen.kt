package com.example.waterheater.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterheater.data.BoostPreset
import com.example.waterheater.data.SeasonalModel
import com.example.waterheater.data.ThermalEstimate
import com.example.waterheater.data.ThermalModel
import com.example.waterheater.ui.UiState
import com.example.waterheater.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onSliderMinutesChanged: (Int) -> Unit,
    onApplyPreset: (BoostPreset) -> Unit,
    onStartCustomBoost: () -> Unit,
    onAddExtraMinutes: (Int) -> Unit,
    onStopHeating: () -> Unit,
    onOpenRelayDialog: () -> Unit,
    onOpenSettingsDialog: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = CyanWaterPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Water Heater Boost",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettingsDialog) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AmberFirePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        },
        containerColor = DeepBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status & Connection Header Card
            ConnectionStatusHeader(
                isConnected = state.status.isConnected,
                isSwitchOn = state.status.isSwitchOn,
                relayStatus = state.status.relayStatus,
                onOpenRelayDialog = onOpenRelayDialog
            )

            // Active Countdown Card (when switch_1 is ON)
            AnimatedVisibility(visible = state.status.isSwitchOn) {
                ActiveHeatingCard(
                    countdownSeconds = state.status.countdownSeconds,
                    onAddExtraMinutes = onAddExtraMinutes,
                    onStopHeating = onStopHeating,
                    isLoading = state.isLoading
                )
            }

            // Quick Boost Presets Grid
            Text(
                text = "⚡ Quick Boost Presets",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp
            )

            // Mains water temperature seasonal info chip
            MainsWaterTempChip(mainsTempC = state.mainsTempC)
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val chunkedPresets = state.presets.chunked(2)
                for (row in chunkedPresets) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (preset in row) {
                            Box(modifier = Modifier.weight(1f)) {
                                PresetCardTile(
                                    preset = preset,
                                    onClick = { onApplyPreset(preset) },
                                    isEnabled = !state.isLoading
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Thermal Calculator & Custom Duration Slider
            Text(
                text = "🎛️ Custom Boost Calculator",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp
            )

            CustomBoostSliderCard(
                selectedMinutes = state.selectedBoostMinutes,
                estimate = state.thermalEstimate,
                onSliderChanged = onSliderMinutesChanged,
                onStartBoost = onStartCustomBoost,
                isLoading = state.isLoading
            )
        }
    }
}

@Composable
fun ConnectionStatusHeader(
    isConnected: Boolean,
    isSwitchOn: Boolean,
    relayStatus: String,
    onOpenRelayDialog: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) EmeraldSuccess else RoseError)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isConnected) "Connected to Tuya" else "Offline / Reconnecting",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isSwitchOn) "Status: HEATING ON" else "Status: IDLE / OFF",
                        color = if (isSwitchOn) AmberFirePrimary else CyanWaterPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Power-Restore Badge Button
            Surface(
                color = SurfaceVariantCard,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onOpenRelayDialog() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AmberFirePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Power: $relayStatus",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveHeatingCard(
    countdownSeconds: Int,
    onAddExtraMinutes: (Int) -> Unit,
    onStopHeating: () -> Unit,
    isLoading: Boolean
) {
    val formattedTimer = ThermalModel.formatSeconds(countdownSeconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF7C2D12), Color(0xFF451A03))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, AmberFirePrimary, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = AmberGlow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HEATER IS ACTIVE",
                    fontWeight = FontWeight.Bold,
                    color = AmberGlow,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formattedTimer,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Text(
                text = "Remaining boost countdown",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAddExtraMinutes(10) },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+10 Mins", color = AmberGlow)
                }

                Button(
                    onClick = onStopHeating,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCEL / OFF", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PresetCardTile(
    preset: BoostPreset,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    val icon = when (preset.iconName) {
        "bolt" -> Icons.Default.FlashOn
        "shower" -> Icons.Default.Shower
        "bathtub" -> Icons.Default.Bathtub
        else -> Icons.Default.LocalFireDepartment
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberFirePrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = preset.subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun CustomBoostSliderCard(
    selectedMinutes: Int,
    estimate: ThermalEstimate,
    onSliderChanged: (Int) -> Unit,
    onStartBoost: () -> Unit,
    isLoading: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Boost Duration:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Text(
                    text = ThermalModel.formatSeconds(selectedMinutes * 60),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberFirePrimary
                )
            }

            Slider(
                value = selectedMinutes.toFloat(),
                onValueChange = { onSliderChanged(it.toInt()) },
                valueRange = 5f..240f,
                steps = 46, // 5 min increments up to 4h
                colors = SliderDefaults.colors(
                    thumbColor = AmberFirePrimary,
                    activeTrackColor = AmberFirePrimary,
                    inactiveTrackColor = SurfaceVariantCard
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Thermal Output Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThermalMetricBox(
                    title = "Target Temp",
                    value = "${String.format("%.1f", estimate.estimatedTempC)}°C",
                    subtext = "+${String.format("%.1f", estimate.tempRiseC)}°C gain",
                    icon = Icons.Default.Thermostat,
                    tint = AmberGlow,
                    modifier = Modifier.weight(1f)
                )

                ThermalMetricBox(
                    title = "Shower Water",
                    value = "~${estimate.usableShowerLiters.toInt()} L",
                    subtext = "at 40°C shower",
                    icon = Icons.Default.Shower,
                    tint = CyanWaterPrimary,
                    modifier = Modifier.weight(1f)
                )

                ThermalMetricBox(
                    title = "Energy",
                    value = "${String.format("%.2f", estimate.energyKwh)} kWh",
                    subtext = "grid power",
                    icon = Icons.Default.FlashOn,
                    tint = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartBoost,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AmberFirePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DeepBackground)
                } else {
                    Text(
                        text = "START ${ThermalModel.formatSeconds(selectedMinutes * 60).uppercase()} BOOST",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBackground
                    )
                }
            }
        }
    }
}

@Composable
fun ThermalMetricBox(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantCard)
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtext, fontSize = 10.sp, color = TextMuted)
        }
    }
}

/**
 * Small informational chip displaying today's estimated mains cold-water temperature
 * derived from the UKWIR seasonal formula. Not interactive.
 */
@Composable
fun MainsWaterTempChip(mainsTempC: Double) {
    Surface(
        color = SurfaceVariantCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = CyanWaterPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mains water today: ",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "~${String.format("%.1f", mainsTempC)} °C",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanWaterPrimary
                    )
                }
                Text(
                    text = "seasonal formula · heating times adjusted",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun ScheduledTimersSection(schedules: List<com.example.waterheater.data.TuyaScheduleTimer>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "📅 Scheduled Timers (Tuya App)",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 18.sp
        )

        if (schedules.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "No Active Cloud Schedules",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Weekly timers created in the Tuya app will automatically appear here.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (schedule in schedules) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = AmberFirePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${schedule.time} — ${schedule.actionText}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Repeats: ${schedule.daysFormatted}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                color = if (schedule.isEnabled) SurfaceVariantCard else DeepBackground,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (schedule.isEnabled) "ACTIVE" else "DISABLED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (schedule.isEnabled) EmeraldSuccess else TextMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
