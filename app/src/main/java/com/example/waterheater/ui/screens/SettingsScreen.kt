package com.example.waterheater.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterheater.data.SeasonalModel
import com.example.waterheater.data.TankConfig
import com.example.waterheater.data.TuyaCredentials
import com.example.waterheater.ui.theme.*

@Composable
fun SettingsDialog(
    config: TankConfig,
    credentials: TuyaCredentials,
    onSaveTankConfig: (volumeLiters: Double, elementKw: Double) -> Unit,
    onSaveCredentials: (clientId: String, secret: String, deviceId: String, regionUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Tuya API Credentials, 1: Tank Parameters

    // Tuya Credentials fields
    var clientIdText by remember { mutableStateOf(credentials.clientId) }
    var secretText by remember { mutableStateOf(credentials.secret) }
    var deviceIdText by remember { mutableStateOf(credentials.deviceId) }
    var regionUrlText by remember { mutableStateOf(credentials.regionUrl) }

    // Tank Config fields
    var volumeText by remember { mutableStateOf(config.volumeLiters.toString()) }
    var kwText by remember { mutableStateOf(config.elementKw.toString()) }
    // coldInletTempC is read-only — always from the seasonal model

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "⚙️ Application Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceVariantCard,
                    contentColor = AmberFirePrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tuya API Key", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tank Physics", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    Text(
                        text = "Parameterize your Tuya OpenAPI credentials and Device ID below. You can change these anytime to target another Tuya device or account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = clientIdText,
                        onValueChange = { clientIdText = it },
                        label = { Text("Client ID (apiKey)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberFirePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = AmberFirePrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = secretText,
                        onValueChange = { secretText = it },
                        label = { Text("Client Secret (apiSecret)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberFirePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = AmberFirePrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = deviceIdText,
                        onValueChange = { deviceIdText = it },
                        label = { Text("Water Tank Device ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberFirePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = AmberFirePrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = regionUrlText,
                        onValueChange = { regionUrlText = it },
                        label = { Text("Tuya Region Endpoint URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberFirePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = AmberFirePrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                } else {
                    Text(
                        text = "Configure your hot water tank parameters to calculate exact heating duration and usable shower water estimates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = volumeText,
                        onValueChange = { volumeText = it },
                        label = { Text("Tank Capacity (Liters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberFirePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = AmberFirePrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = kwText,
                        onValueChange = { kwText = it },
                        label = { Text("Heater Element Power (kW)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberFirePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = AmberFirePrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Read-only mains water temperature from seasonal model
                    Surface(
                        color = SurfaceVariantCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = CyanWaterPrimary,
                                modifier = androidx.compose.ui.Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cold Water Inlet Temp",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "~${SeasonalModel.formattedTemp()} (seasonal estimate)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanWaterPrimary
                                )
                                Text(
                                    text = "Automatically adjusted by day of year — not editable",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        onSaveCredentials(clientIdText, secretText, deviceIdText, regionUrlText)
                    } else {
                        val vol = volumeText.toDoubleOrNull() ?: config.volumeLiters
                        val kw = kwText.toDoubleOrNull() ?: config.elementKw
                        onSaveTankConfig(vol, kw)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberFirePrimary)
            ) {
                Text(if (selectedTab == 0) "Save API Credentials" else "Save Tank Parameters", color = DeepBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}
