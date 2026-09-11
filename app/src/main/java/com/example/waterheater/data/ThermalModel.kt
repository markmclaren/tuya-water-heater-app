package com.example.waterheater.data

import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.PI
import java.util.Calendar

// ---------------------------------------------------------------------------
// Region profiles for the seasonal mains water temperature model.
//
// Each profile encodes three parameters for the sine-wave formula:
//   T_mains = annualMeanC + amplitudeC × sin(2π × (day_of_year − phaseDay) / 365)
//
// • annualMeanC  — average cold-water temperature at pipe depth (°C)
// • amplitudeC   — seasonal swing (°C peak-to-mean)
// • phaseDay     — day-of-year where the sine curve crosses its mean ascending
//                  (warmest = phaseDay + 91, coldest = phaseDay − 91 ≈ phaseDay + 274)
//
// Sources:
//   UKWIR   — "Cold water temperatures in UK distribution systems" (09/WM/03/14)
//   DVGW    — W551 "Trinkwassererwärmungsanlagen" (German hot-water standard)
//   BRGM    — French geological survey ground temperature atlas
//   ASHRAE  — 2009 Handbook of Fundamentals, Chapter 18 (Service Water Heating)
//   CSIRO   — Australian soil temperature studies (Bureau of Meteorology ground data)
// ---------------------------------------------------------------------------

data class RegionProfile(
    val id: String,
    val flag: String,               // Unicode flag emoji
    val name: String,               // Display name shown in Settings
    val annualMeanC: Double,        // Annual mean mains temperature (°C)
    val amplitudeC: Double,         // Seasonal swing ± (°C)
    val phaseDay: Int,              // Day-of-year at ascending zero-crossing
    val pipeDepthMm: Int,           // Typical pipe burial depth (informational)
    val source: String,             // Reference / data source
    val isCustom: Boolean = false   // True only for the editable Custom entry
)

// ---------------------------------------------------------------------------
// Pre-defined region profiles.  UK_REGION is the default.
// ---------------------------------------------------------------------------
val UK_REGION = RegionProfile(
    id = "uk", flag = "🇬🇧", name = "United Kingdom",
    annualMeanC = 11.5, amplitudeC = 7.5, phaseDay = 60, pipeDepthMm = 750,
    source = "UKWIR Report 09/WM/03/14"
)

val PREDEFINED_REGIONS: List<RegionProfile> = listOf(
    UK_REGION,
    RegionProfile(
        id = "ie", flag = "🇮🇪", name = "Ireland",
        annualMeanC = 11.0, amplitudeC = 6.5, phaseDay = 60, pipeDepthMm = 600,
        source = "Met Éireann / Geological Survey Ireland soil temp data"
    ),
    RegionProfile(
        id = "fr", flag = "🇫🇷", name = "France (North)",
        annualMeanC = 12.5, amplitudeC = 8.0, phaseDay = 58, pipeDepthMm = 800,
        source = "BRGM French ground temperature atlas"
    ),
    RegionProfile(
        id = "de", flag = "🇩🇪", name = "Germany",
        annualMeanC = 10.0, amplitudeC = 9.5, phaseDay = 55, pipeDepthMm = 800,
        source = "DVGW W551 / DIN 4708 (German water heating standard)"
    ),
    RegionProfile(
        id = "nl", flag = "🇳🇱", name = "Netherlands",
        annualMeanC = 11.0, amplitudeC = 7.0, phaseDay = 60, pipeDepthMm = 700,
        source = "RIVM / KWR water research ground temperature data"
    ),
    RegionProfile(
        id = "es", flag = "🇪🇸", name = "Spain (North)",
        annualMeanC = 14.0, amplitudeC = 7.0, phaseDay = 55, pipeDepthMm = 600,
        source = "IGN Spanish ground temperature atlas"
    ),
    RegionProfile(
        id = "us_ne", flag = "🇺🇸", name = "USA (North-East)",
        annualMeanC = 10.0, amplitudeC = 10.0, phaseDay = 60, pipeDepthMm = 900,
        source = "ASHRAE 2009 HOF Ch. 18 / EPA WaterSense data"
    ),
    RegionProfile(
        id = "us_pnw", flag = "🇺🇸", name = "USA (Pacific NW)",
        annualMeanC = 11.0, amplitudeC = 6.0, phaseDay = 60, pipeDepthMm = 750,
        source = "ASHRAE 2009 HOF Ch. 18 / EPA WaterSense data"
    ),
    RegionProfile(
        id = "au_se", flag = "🇦🇺", name = "Australia (South-East)",
        annualMeanC = 16.0, amplitudeC = 6.0, phaseDay = 240, pipeDepthMm = 500,
        source = "CSIRO / Bureau of Meteorology soil temp data (inverted season)"
    ),
    RegionProfile(
        id = "custom", flag = "🌍", name = "Custom",
        annualMeanC = 11.5, amplitudeC = 7.5, phaseDay = 60, pipeDepthMm = 750,
        source = "User-defined",
        isCustom = true
    )
)

// ---------------------------------------------------------------------------
// Seasonal mains-water temperature model.
//
// Call SeasonalModel.setRegion(profile) from the ViewModel when the user
// changes their region in Settings.  All subsequent mainsWaterTempC() calls
// will use the new profile.
// ---------------------------------------------------------------------------
object SeasonalModel {

    /** Currently active region — defaults to UK on first launch. */
    var currentRegion: RegionProfile = UK_REGION
        private set

    fun setRegion(profile: RegionProfile) {
        currentRegion = profile
    }

    /** Estimated mains cold-water temperature for today using the active region. */
    fun mainsWaterTempC(): Double = mainsWaterTempC(currentRegion)

    /** Estimated mains cold-water temperature for today using a specific region. */
    fun mainsWaterTempC(region: RegionProfile): Double {
        val cal = Calendar.getInstance()
        val doy = cal.get(Calendar.DAY_OF_YEAR)  // 1–365
        val angle = 2.0 * PI * (doy - region.phaseDay) / 365.0
        return region.annualMeanC + region.amplitudeC * sin(angle)
    }

    /** Formatted label for display, e.g. "16.2 °C" */
    fun formattedTemp(): String = String.format("%.1f °C", mainsWaterTempC())
}


/**
 * Data class representing configuration parameters for a water heating tank.
 */
data class TankConfig(
    val volumeLiters: Double = 266.0,       // Tank volume in liters (default: 266L)
    val elementKw: Double = 3.0,            // Heating element rating in kW (default: 3kW)
    val efficiency: Double = 0.95,          // Heating efficiency factor (95%)
    // Defaults to today's seasonal estimate — recalculated on every app launch
    val coldInletTempC: Double = SeasonalModel.mainsWaterTempC(),
    val targetFullTempC: Double = 60.0,     // Target full heating temperature in °C
    val maxSafeTempC: Double = 65.0         // Max safety limit in °C
)

/**
 * Calculated thermal estimation results for a given boost duration.
 */
data class ThermalEstimate(
    val durationSeconds: Int,
    val tempRiseC: Double,
    val estimatedTempC: Double,
    val energyKwh: Double,
    val usableShowerLiters: Double,
    val durationFormatted: String
)

/**
 * Quick boost preset definition.
 */
data class BoostPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationMinutes: Int,
    val iconName: String
)

object ThermalModel {
    private const val WATER_HEAT_CAPACITY = 4184.0 // J / (kg * °C)
    private const val WATER_DENSITY = 1.0          // kg / Liter

    /**
     * Calculates thermal metrics for a specific heating duration in seconds.
     */
    fun calculateEstimate(durationSeconds: Int, config: TankConfig): ThermalEstimate {
        val seconds = max(0, durationSeconds)
        val powerWatts = config.elementKw * 1000.0
        val massKg = config.volumeLiters * WATER_DENSITY

        // Thermal energy delivered to water Q = P * t * efficiency
        val energyJoules = powerWatts * seconds * config.efficiency
        
        // Temperature rise deltaT = Q / (m * c)
        val tempRise = energyJoules / (massKg * WATER_HEAT_CAPACITY)
        
        // Estimated final tank temperature (capped at maxSafeTempC)
        val finalTemp = min(config.maxSafeTempC, config.coldInletTempC + tempRise)
        val actualRise = finalTemp - config.coldInletTempC

        // Energy consumed from grid in kWh
        val energyKwh = (powerWatts * seconds) / (3600.0 * 1000.0)

        // Usable shower water volume at 40°C mixed temperature
        // V_mix * (40 - T_cold) = V_tank * (T_tank - T_cold)
        val targetShowerTemp = 40.0
        val usableLiters = if (finalTemp > config.coldInletTempC && targetShowerTemp > config.coldInletTempC) {
            val ratio = (finalTemp - config.coldInletTempC) / (targetShowerTemp - config.coldInletTempC)
            config.volumeLiters * ratio
        } else {
            0.0
        }

        return ThermalEstimate(
            durationSeconds = seconds,
            tempRiseC = actualRise,
            estimatedTempC = finalTemp,
            energyKwh = energyKwh,
            usableShowerLiters = usableLiters,
            durationFormatted = formatSeconds(seconds)
        )
    }

    /**
     * Calculates the exact duration in seconds required to heat tank from cold inlet to target full temperature.
     */
    fun calculateFullHeatSeconds(config: TankConfig): Int {
        val tempDelta = max(0.0, config.targetFullTempC - config.coldInletTempC)
        val massKg = config.volumeLiters * WATER_DENSITY
        val requiredJoules = massKg * WATER_HEAT_CAPACITY * tempDelta
        val powerWatts = config.elementKw * 1000.0 * config.efficiency

        if (powerWatts <= 0) return 0
        return (requiredJoules / powerWatts).toInt()
    }

    /**
     * Returns standard recommended boost presets.
     *
     * Shower duration labels (e.g. "4 min Quick Shower") assume a power shower
     * flow rate of approximately 12 L/min at 40°C mixed temperature.
     * A gravity-fed or standard mixer shower (~8 L/min) will give longer
     * shower times from the same hot water volume.
     */
    fun getPresets(config: TankConfig): List<BoostPreset> {
        val fullHeatMins = (calculateFullHeatSeconds(config) / 60)
        val fullHeatHours = fullHeatMins / 60
        val fullHeatRemMins = fullHeatMins % 60
        val fullHeatTitle = if (fullHeatHours > 0) "Full Tank (${fullHeatHours}h ${fullHeatRemMins}m)" else "Full Tank (${fullHeatMins}m)"

        return listOf(
            BoostPreset(
                id = "shower_30",
                title = "30 Min Boost",
                subtitle = "4 min Quick Shower (~49L)",
                durationMinutes = 30,
                iconName = "shower"
            ),
            BoostPreset(
                id = "shower_60",
                title = "1 Hour Boost",
                subtitle = "8 min Full Shower (~98L)",
                durationMinutes = 60,
                iconName = "shower"
            ),
            BoostPreset(
                id = "bath_90",
                title = "1.5 Hour Boost",
                subtitle = "Full Deep Bath (~147L)",
                durationMinutes = 90,
                iconName = "bathtub"
            ),
            BoostPreset(
                id = "showers_120",
                title = "2 Hour Boost",
                subtitle = "2 Full Showers (~196L)",
                durationMinutes = 120,
                iconName = "bolt"
            ),
            BoostPreset(
                id = "full_heat",
                title = fullHeatTitle,
                subtitle = "40 min showering (~478L)",
                durationMinutes = fullHeatMins,
                iconName = "local_fire_department"
            )
        )
    }

    fun formatSeconds(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0 mins"
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
            mins > 0 -> "${mins}m"
            else -> "${secs}s"
        }
    }
}
