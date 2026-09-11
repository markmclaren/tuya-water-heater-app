package com.example.waterheater.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WaterHeaterRepository(
    private val apiClient: TuyaApiClient = TuyaApiClient()
) {
    private val _status = MutableStateFlow(
        WaterHeaterStatus(
            isConnected = false,
            isSwitchOn = false,
            countdownSeconds = 0,
            relayStatus = "power_off"
        )
    )
    val status: StateFlow<WaterHeaterStatus> = _status.asStateFlow()

    private val _tankConfig = MutableStateFlow(TankConfig())
    val tankConfig: StateFlow<TankConfig> = _tankConfig.asStateFlow()

    private val _tuyaCredentials = MutableStateFlow(apiClient.credentials)
    val tuyaCredentials: StateFlow<TuyaCredentials> = _tuyaCredentials.asStateFlow()

    fun updateTuyaCredentials(newCreds: TuyaCredentials) {
        apiClient.updateCredentials(newCreds)
        _tuyaCredentials.value = newCreds
    }

    suspend fun refreshStatus(): WaterHeaterStatus {
        val newStatus = apiClient.getDeviceStatus()
        _status.value = newStatus
        return newStatus
    }

    suspend fun fetchSchedules(): List<TuyaScheduleTimer> {
        return apiClient.getDeviceSchedules()
    }

    suspend fun startBoost(durationSeconds: Int): Boolean {
        val success = apiClient.sendBoostCommand(turnOn = true, countdownSeconds = durationSeconds)
        if (success) {
            refreshStatus()
        }
        return success
    }

    suspend fun stopHeating(): Boolean {
        val success = apiClient.sendBoostCommand(turnOn = false, countdownSeconds = 0)
        if (success) {
            refreshStatus()
        }
        return success
    }

    suspend fun updateRelayStatus(mode: String): Boolean {
        val success = apiClient.setRelayStatus(mode)
        if (success) {
            refreshStatus()
        }
        return success
    }

    fun updateTankConfig(newConfig: TankConfig) {
        _tankConfig.value = newConfig
    }
}
