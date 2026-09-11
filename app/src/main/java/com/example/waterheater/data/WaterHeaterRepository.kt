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

    private val _readyBySchedule = MutableStateFlow(ReadyBySchedule())
    val readyBySchedule: StateFlow<ReadyBySchedule> = _readyBySchedule.asStateFlow()

    // Tracks the Tuya group IDs of the active "Ready by" timer pair
    var programmedOnGroupId: String? = null
        private set
    var programmedOffGroupId: String? = null
        private set
    var lastSyncedCalc: CalculatedScheduleTimes? = null
        private set

    fun updateReadyBySchedule(schedule: ReadyBySchedule) {
        _readyBySchedule.value = schedule
    }

    suspend fun syncReadyByScheduleToTuya(calc: CalculatedScheduleTimes, schedule: ReadyBySchedule): Boolean {
        // 1. Remove previous programmed timers if known
        programmedOnGroupId?.let { apiClient.deleteDeviceTimerGroup(it) }
        programmedOffGroupId?.let { apiClient.deleteDeviceTimerGroup(it) }

        // 2. Program switch ON timer at calculated start time
        val onId = apiClient.createDeviceTimer(
            time = calc.formattedStartTime,
            turnOn = true,
            loops = schedule.repeatDays
        )

        // 3. Program switch OFF timer at target ready time
        val offId = apiClient.createDeviceTimer(
            time = calc.formattedReadyTime,
            turnOn = false,
            loops = schedule.repeatDays
        )

        programmedOnGroupId = onId
        programmedOffGroupId = offId
        val success = onId != null && offId != null
        if (success) {
            lastSyncedCalc = calc
        }
        return success
    }

    suspend fun deleteReadyByScheduleFromTuya(): Boolean {
        var ok = true
        programmedOnGroupId?.let { ok = apiClient.deleteDeviceTimerGroup(it) && ok }
        programmedOffGroupId?.let { ok = apiClient.deleteDeviceTimerGroup(it) && ok }
        programmedOnGroupId = null
        programmedOffGroupId = null
        lastSyncedCalc = null
        return ok
    }
}
