package com.example.waterheater.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterheater.data.CalculatedScheduleTimes
import com.example.waterheater.data.ReadyBySchedule
import com.example.waterheater.data.TuyaScheduleTimer
import com.example.waterheater.ui.UiState
import com.example.waterheater.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onUpdateTime: (hour: Int, minute: Int) -> Unit,
    onUpdateRepeatDays: (String) -> Unit,
    onSyncSchedule: () -> Unit,
    onDeleteSchedule: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = AmberFirePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Heating Schedules",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ===================================================================
            // 1. Ready by [Time] Automated Morning Heating Card
            // ===================================================================
            ReadyByMorningCard(
                schedule = state.readyBySchedule,
                calc = state.calculatedSchedule,
                mainsTempC = state.mainsTempC,
                flag = state.selectedRegion.flag,
                regionName = state.selectedRegion.name,
                isSyncing = state.isSyncingSchedule,
                isSynced = state.isScheduleSynced,
                onUpdateTime = onUpdateTime,
                onUpdateRepeatDays = onUpdateRepeatDays,
                onSyncSchedule = onSyncSchedule,
                onDeleteSchedule = onDeleteSchedule
            )

            HorizontalDivider(color = CardBorder)

            // ===================================================================
            // 2. Detected Cloud Schedules on Hardware
            // ===================================================================
            Text(
                text = "Cloud Timers on Device",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
                color = TextPrimary
            )

            Text(
                text = "Timers currently programmed into the Tuya switch (execute autonomously in the cloud even when offline):",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (state.schedules.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Cloud Timers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use the 'Ready by Time' scheduler above to program autonomous morning heating, or set timers in the Tuya app.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (schedule in state.schedules) {
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
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = AmberFirePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${schedule.time} — ${schedule.actionText}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Repeats: ${schedule.daysFormatted}",
                                            fontSize = 13.sp,
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (schedule.isEnabled) EmeraldSuccess else TextMuted,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReadyByMorningCard(
    schedule: ReadyBySchedule,
    calc: CalculatedScheduleTimes,
    mainsTempC: Double,
    flag: String,
    regionName: String,
    isSyncing: Boolean,
    isSynced: Boolean,
    onUpdateTime: (hour: Int, minute: Int) -> Unit,
    onUpdateRepeatDays: (String) -> Unit,
    onSyncSchedule: () -> Unit,
    onDeleteSchedule: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberFirePrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌅", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ready by Time",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Reverse-calculated full tank (60 °C)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = if (isSynced) EmeraldSuccess.copy(alpha = 0.15f) else AmberFirePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isSynced) "SYNCED" else "READY TO SYNC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSynced) EmeraldSuccess else AmberFirePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Target Ready Time Stepper & Display
            Surface(
                color = SurfaceVariantCard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HOT WATER READY BY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                var total = schedule.targetHour * 60 + schedule.targetMinute - 15
                                if (total < 0) total += 1440
                                onUpdateTime(total / 60, total % 60)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = DeepBackground, contentColor = TextPrimary
                            )
                        ) {
                            Text("-15m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = calc.formattedReadyTime,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberFirePrimary
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledTonalIconButton(
                            onClick = {
                                val total = (schedule.targetHour * 60 + schedule.targetMinute + 15) % 1440
                                onUpdateTime(total / 60, total % 60)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = DeepBackground, contentColor = TextPrimary
                            )
                        ) {
                            Text("+15m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick Preset Times
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickPresets = listOf(6 to 0, 6 to 30, 7 to 0, 7 to 30, 8 to 0)
                        for ((h, m) in quickPresets) {
                            val label = "%02d:%02d".format(h, m)
                            val isSelected = schedule.targetHour == h && schedule.targetMinute == m
                            Surface(
                                color = if (isSelected) AmberFirePrimary else DeepBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onUpdateTime(h, m) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DeepBackground else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Repeat Days Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Repeat Days",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                    val loops = schedule.repeatDays.padEnd(7, '0')

                    for (i in 0..6) {
                        val isActive = loops.getOrNull(i) == '1'
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isActive) AmberFirePrimary else SurfaceVariantCard)
                                .clickable {
                                    val chars = loops.toCharArray()
                                    chars[i] = if (isActive) '0' else '1'
                                    onUpdateRepeatDays(String(chars))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNames[i],
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) DeepBackground else TextSecondary
                            )
                        }
                    }
                }
            }

            // Calculated Thermal Details Card
            Surface(
                color = SurfaceVariantCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$flag Mains water today:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "~${"%.1f".format(mainsTempC)} °C ($regionName)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanWaterPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Heating duration:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${calc.formattedDuration} (+${"%.1f".format(calc.tempRiseC)} °C rise)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("⚡ Switch ON", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = calc.formattedStartTime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberFirePrimary
                            )
                        }

                        Text("➔", fontSize = 18.sp, color = TextMuted)

                        Column(horizontalAlignment = Alignment.End) {
                            Text("🛑 Switch OFF", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = calc.formattedReadyTime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    Text(
                        text = "💡 Finishes right at ${calc.formattedReadyTime}, saving standby heat loss and taking advantage of off-peak electricity.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            // Action Button: Program Tuya Cloud Timers
            Button(
                onClick = onSyncSchedule,
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberFirePrimary,
                    contentColor = DeepBackground
                )
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DeepBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Programming Tuya Cloud Timers...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Program Timers to Relay", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Note: Ground temperature shifts very slowly (~1–2 min heating shift per week), so you only need to re-sync this schedule seasonally.",
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
