package com.example.waterheater.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.waterheater.data.BoostPreset
import com.example.waterheater.data.SeasonalModel
import com.example.waterheater.data.TankConfig
import com.example.waterheater.data.ThermalEstimate
import com.example.waterheater.data.ThermalModel
import com.example.waterheater.data.WaterHeaterRepository
import com.example.waterheater.data.WaterHeaterStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.waterheater.data.TuyaCredentials
import com.example.waterheater.data.TuyaScheduleTimer

data class UiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val status: WaterHeaterStatus = WaterHeaterStatus(isConnected = false, isSwitchOn = false, countdownSeconds = 0, relayStatus = "power_off"),
    val tankConfig: TankConfig = TankConfig(),
    val tuyaCredentials: TuyaCredentials = TuyaCredentials(),
    val schedules: List<TuyaScheduleTimer> = emptyList(),
    val selectedBoostMinutes: Int = 30,
    val thermalEstimate: ThermalEstimate = ThermalModel.calculateEstimate(30 * 60, TankConfig()),
    val presets: List<BoostPreset> = ThermalModel.getPresets(TankConfig()),
    val isRelayDialogVisible: Boolean = false,
    val isSettingsDialogVisible: Boolean = false,
    val userNotification: String? = null,
    // Seasonal mains water temperature estimate (UKWIR formula, no external API)
    val mainsTempC: Double = SeasonalModel.mainsWaterTempC()
)

class WaterHeaterViewModel(
    private val repository: WaterHeaterRepository = WaterHeaterRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                refreshStatus()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val status = repository.refreshStatus()
            val schedules = repository.fetchSchedules()
            _uiState.value = _uiState.value.copy(
                status = status,
                schedules = schedules,
                isRefreshing = false
            )
        }
    }

    fun onSliderMinutesChanged(minutes: Int) {
        val seconds = minutes * 60
        val estimate = ThermalModel.calculateEstimate(seconds, _uiState.value.tankConfig)
        _uiState.value = _uiState.value.copy(
            selectedBoostMinutes = minutes,
            thermalEstimate = estimate
        )
    }

    fun applyPreset(preset: BoostPreset) {
        onSliderMinutesChanged(preset.durationMinutes)
        startBoost(preset.durationMinutes * 60)
    }

    fun startCustomBoost() {
        val seconds = _uiState.value.selectedBoostMinutes * 60
        startBoost(seconds)
    }

    fun addExtraBoostMinutes(extraMinutes: Int) {
        val currentCountdown = _uiState.value.status.countdownSeconds
        val newSeconds = currentCountdown + (extraMinutes * 60)
        startBoost(newSeconds)
    }

    private fun startBoost(durationSeconds: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.startBoost(durationSeconds)
            val msg = if (success) {
                "Boost started! Heating for ${ThermalModel.formatSeconds(durationSeconds)}."
            } else {
                "Failed to send boost command to Tuya."
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userNotification = msg
            )
        }
    }

    fun stopHeating() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.stopHeating()
            val msg = if (success) "Water tank turned OFF." else "Failed to stop heating."
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userNotification = msg
            )
        }
    }

    fun setRelayMode(mode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isRelayDialogVisible = false)
            val success = repository.updateRelayStatus(mode)
            val msg = if (success) "Power restore state set to '$mode'." else "Failed to update relay setting."
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userNotification = msg
            )
        }
    }

    fun updateTankConfig(volumeLiters: Double, elementKw: Double) {
        // coldInletTempC is always driven by the seasonal model — not user-editable
        val newConfig = _uiState.value.tankConfig.copy(
            volumeLiters = volumeLiters,
            elementKw = elementKw,
            coldInletTempC = SeasonalModel.mainsWaterTempC()
        )
        repository.updateTankConfig(newConfig)
        val newEstimate = ThermalModel.calculateEstimate(_uiState.value.selectedBoostMinutes * 60, newConfig)
        val newPresets = ThermalModel.getPresets(newConfig)

        _uiState.value = _uiState.value.copy(
            tankConfig = newConfig,
            thermalEstimate = newEstimate,
            presets = newPresets,
            isSettingsDialogVisible = false,
            userNotification = "Tank parameters updated."
        )
    }

    fun updateTuyaCredentials(clientId: String, secret: String, deviceId: String, regionUrl: String) {
        val newCreds = TuyaCredentials(
            clientId = clientId.trim(),
            secret = secret.trim(),
            deviceId = deviceId.trim(),
            regionUrl = regionUrl.trim()
        )
        repository.updateTuyaCredentials(newCreds)
        _uiState.value = _uiState.value.copy(
            tuyaCredentials = newCreds,
            isSettingsDialogVisible = false,
            userNotification = "Tuya credentials updated! Reconnecting..."
        )
        refreshStatus()
    }

    fun showRelayDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isRelayDialogVisible = show)
    }

    fun showSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsDialogVisible = show)
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
