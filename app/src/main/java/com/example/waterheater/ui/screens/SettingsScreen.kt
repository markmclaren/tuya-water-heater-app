package com.example.waterheater.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterheater.data.PREDEFINED_REGIONS
import com.example.waterheater.data.RegionProfile
import com.example.waterheater.data.SeasonalModel
import com.example.waterheater.data.TankConfig
import com.example.waterheater.data.TuyaCredentials
import com.example.waterheater.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    config: TankConfig,
    credentials: TuyaCredentials,
    selectedRegion: RegionProfile,
    onSaveTankConfig: (volumeLiters: Double, elementKw: Double, region: RegionProfile) -> Unit,
    onSaveCredentials: (clientId: String, secret: String, deviceId: String, regionUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Tuya API Credentials, 1: Tank Physics

    // Tuya Credentials fields
    var clientIdText  by remember { mutableStateOf(credentials.clientId) }
    var secretText    by remember { mutableStateOf(credentials.secret) }
    var deviceIdText  by remember { mutableStateOf(credentials.deviceId) }
    var regionUrlText by remember { mutableStateOf(credentials.regionUrl) }

    // Tank Config fields
    var volumeText by remember { mutableStateOf(config.volumeLiters.toString()) }
    var kwText     by remember { mutableStateOf(config.elementKw.toString()) }

    // Region selection
    var pickedRegion by remember { mutableStateOf(selectedRegion) }
    // For the "Custom" entry: editable copies of the three constants
    var customMean      by remember { mutableStateOf(selectedRegion.annualMeanC.toString()) }
    var customAmplitude by remember { mutableStateOf(selectedRegion.amplitudeC.toString()) }
    var customPhaseDay  by remember { mutableStateOf(selectedRegion.phaseDay.toString()) }

    var regionDropdownExpanded by remember { mutableStateOf(false) }

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
                    // ----------------------------------------------------------
                    // Tab 0: Tuya API Credentials
                    // ----------------------------------------------------------
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
                    // ----------------------------------------------------------
                    // Tab 1: Tank Physics
                    // ----------------------------------------------------------
                    Text(
                        text = "Configure your hot water cylinder and select your region to calibrate the seasonal mains temperature model.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // --- Region selector ---
                    Text(
                        text = "Mains Water Region",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = regionDropdownExpanded,
                        onExpandedChange = { regionDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${pickedRegion.flag}  ${pickedRegion.name}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Region") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AmberFirePrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberFirePrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedLabelColor = AmberFirePrimary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = regionDropdownExpanded,
                            onDismissRequest = { regionDropdownExpanded = false },
                            containerColor = SurfaceCard
                        ) {
                            PREDEFINED_REGIONS.forEach { region ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(region.flag, fontSize = 20.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    region.name,
                                                    color = TextPrimary,
                                                    fontWeight = if (region.id == pickedRegion.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (!region.isCustom) {
                                                    Text(
                                                        "~${SeasonalModel.mainsWaterTempC(region).let { "%.1f".format(it) }} °C today",
                                                        fontSize = 11.sp,
                                                        color = CyanWaterPrimary
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        pickedRegion = if (region.isCustom) {
                                            // Pre-populate custom fields from current pickedRegion values
                                            customMean = pickedRegion.annualMeanC.toString()
                                            customAmplitude = pickedRegion.amplitudeC.toString()
                                            customPhaseDay = pickedRegion.phaseDay.toString()
                                            region.copy(
                                                annualMeanC = pickedRegion.annualMeanC,
                                                amplitudeC = pickedRegion.amplitudeC,
                                                phaseDay = pickedRegion.phaseDay
                                            )
                                        } else region
                                        regionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // --- Region info card ---
                    Surface(
                        color = SurfaceVariantCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (pickedRegion.isCustom) {
                                // Editable custom fields
                                Text("Custom Region Parameters", fontSize = 12.sp, color = AmberFirePrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customMean,
                                    onValueChange = {
                                        customMean = it
                                        pickedRegion = pickedRegion.copy(annualMeanC = it.toDoubleOrNull() ?: pickedRegion.annualMeanC)
                                    },
                                    label = { Text("Annual mean temp (°C)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberFirePrimary, unfocusedBorderColor = CardBorder,
                                        focusedLabelColor = AmberFirePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                    )
                                )
                                OutlinedTextField(
                                    value = customAmplitude,
                                    onValueChange = {
                                        customAmplitude = it
                                        pickedRegion = pickedRegion.copy(amplitudeC = it.toDoubleOrNull() ?: pickedRegion.amplitudeC)
                                    },
                                    label = { Text("Seasonal swing ± (°C)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberFirePrimary, unfocusedBorderColor = CardBorder,
                                        focusedLabelColor = AmberFirePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                    )
                                )
                                OutlinedTextField(
                                    value = customPhaseDay,
                                    onValueChange = {
                                        customPhaseDay = it
                                        pickedRegion = pickedRegion.copy(phaseDay = it.toIntOrNull() ?: pickedRegion.phaseDay)
                                    },
                                    label = { Text("Phase day (1–365, ascending zero)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberFirePrimary, unfocusedBorderColor = CardBorder,
                                        focusedLabelColor = AmberFirePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                    )
                                )
                                Text(
                                    "Formula: T = mean + swing × sin(2π × (day − phaseDay) / 365)",
                                    fontSize = 10.sp, color = TextMuted
                                )
                            } else {
                                // Read-only info for a pre-defined region
                                Text("📐 Seasonal Model Parameters", fontSize = 12.sp, color = AmberFirePrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                RegionInfoRow("Annual mean", "${pickedRegion.annualMeanC} °C")
                                RegionInfoRow("Seasonal swing", "±${pickedRegion.amplitudeC} °C")
                                RegionInfoRow("Phase day", "${pickedRegion.phaseDay}  (warmest ≈ day ${pickedRegion.phaseDay + 91})")
                                RegionInfoRow("Pipe depth", "~${pickedRegion.pipeDepthMm} mm")
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "📚 Source: ${pickedRegion.source}",
                                    fontSize = 10.sp, color = TextMuted
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CardBorder)

                    // --- Tank parameters ---
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cold Water Inlet Temp",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "~${SeasonalModel.mainsWaterTempC(pickedRegion).let { "%.1f".format(it) }} °C (seasonal estimate)",
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
                        val kw  = kwText.toDoubleOrNull()     ?: config.elementKw
                        onSaveTankConfig(vol, kw, pickedRegion)
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

@Composable
private fun RegionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
