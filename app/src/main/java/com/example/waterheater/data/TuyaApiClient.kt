package com.example.waterheater.data

import com.example.waterheater.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Credentials are injected at build time from local.properties via buildConfigField.
// Never hardcode secrets here — copy local.properties.example → local.properties.
data class TuyaCredentials(
    val clientId: String = BuildConfig.TUYA_CLIENT_ID,
    val secret: String = BuildConfig.TUYA_CLIENT_SECRET,
    val deviceId: String = BuildConfig.TUYA_WATER_TANK_DEVICE_ID,
    val regionUrl: String = BuildConfig.TUYA_REGION_URL
)

data class WaterHeaterStatus(
    val isConnected: Boolean,
    val isSwitchOn: Boolean,
    val countdownSeconds: Int,
    val relayStatus: String, // "power_off", "power_on", "last"
    val errorMessage: String? = null
)

data class TuyaScheduleTimer(
    val timerId: String,
    val groupId: String = "",
    val time: String,
    val daysFormatted: String,
    val actionText: String,
    val isEnabled: Boolean
)

class TuyaApiClient(
    var credentials: TuyaCredentials = TuyaCredentials()
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var cachedToken: String? = null
    private var tokenExpireTime: Long = 0

    fun updateCredentials(newCredentials: TuyaCredentials) {
        this.credentials = newCredentials
        this.cachedToken = null
        this.tokenExpireTime = 0
    }

    /**
     * Obtains or refreshes Tuya OpenAPI Access Token.
     */
    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpireTime - 60000) {
            return@withContext cachedToken!!
        }

        val timestamp = now.toString()
        val url = "${credentials.regionUrl}/v1.0/token?grant_type=1"
        
        // Build Tuya HMAC-SHA256 signature for token request
        val httpMethod = "GET"
        val bodyHash = sha256("")
        val headers = ""
        val urlPath = "/v1.0/token?grant_type=1"
        
        val stringToSign = "$httpMethod\n$bodyHash\n$headers\n$urlPath"
        val signPayload = "${credentials.clientId}$timestamp$stringToSign"
        val sign = hmacSha256(signPayload, credentials.secret).uppercase()

        val request = Request.Builder()
            .url(url)
            .addHeader("client_id", credentials.clientId)
            .addHeader("sign", sign)
            .addHeader("t", timestamp)
            .addHeader("sign_method", "HMAC-SHA256")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val jsonStr = response.body?.string() ?: throw Exception("Empty token response")
            val json = JSONObject(jsonStr)
            if (json.optBoolean("success")) {
                val result = json.getJSONObject("result")
                val token = result.getString("access_token")
                val expireIn = result.optLong("expire_time", 7200)
                cachedToken = token
                tokenExpireTime = now + (expireIn * 1000)
                return@withContext token
            } else {
                val msg = json.optString("msg", "Failed to get Tuya token")
                throw Exception("Tuya Auth Error: $msg")
            }
        }
    }

    /**
     * Fetches current device status (switch_1, countdown_1, relay_status).
     */
    suspend fun getDeviceStatus(): WaterHeaterStatus = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            val timestamp = System.currentTimeMillis().toString()
            val path = "/v1.0/devices/${credentials.deviceId}/status"
            val url = "${credentials.regionUrl}$path"

            val httpMethod = "GET"
            val bodyHash = sha256("")
            val headers = ""
            
            val stringToSign = "$httpMethod\n$bodyHash\n$headers\n$path"
            val signPayload = "${credentials.clientId}$token$timestamp$stringToSign"
            val sign = hmacSha256(signPayload, credentials.secret).uppercase()

            val request = Request.Builder()
                .url(url)
                .addHeader("client_id", credentials.clientId)
                .addHeader("access_token", token)
                .addHeader("sign", sign)
                .addHeader("t", timestamp)
                .addHeader("sign_method", "HMAC-SHA256")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val jsonStr = response.body?.string() ?: throw Exception("Empty status response")
                val json = JSONObject(jsonStr)
                if (json.optBoolean("success")) {
                    val result = json.getJSONArray("result")
                    var isSwitchOn = false
                    var countdownSecs = 0
                    var relayStatus = "power_off"

                    for (i in 0 until result.length()) {
                        val item = result.getJSONObject(i)
                        when (item.optString("code")) {
                            "switch_1" -> isSwitchOn = item.optBoolean("value")
                            "countdown_1" -> countdownSecs = item.optInt("value")
                            "relay_status" -> relayStatus = item.optString("value", "power_off")
                        }
                    }

                    WaterHeaterStatus(
                        isConnected = true,
                        isSwitchOn = isSwitchOn,
                        countdownSeconds = countdownSecs,
                        relayStatus = relayStatus
                    )
                } else {
                    WaterHeaterStatus(
                        isConnected = false,
                        isSwitchOn = false,
                        countdownSeconds = 0,
                        relayStatus = "power_off",
                        errorMessage = json.optString("msg", "Error fetching device status")
                    )
                }
            }
        } catch (e: Exception) {
            WaterHeaterStatus(
                isConnected = false,
                isSwitchOn = false,
                countdownSeconds = 0,
                relayStatus = "power_off",
                errorMessage = e.localizedMessage ?: e.message ?: "Network error"
            )
        }
    }

    /**
     * Sends commands to turn on/off the relay and set countdown timer.
     */
    suspend fun sendBoostCommand(turnOn: Boolean, countdownSeconds: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val commands = JSONArray()
        commands.put(JSONObject().apply {
            put("code", "switch_1")
            put("value", turnOn)
        })
        if (turnOn) {
            commands.put(JSONObject().apply {
                put("code", "countdown_1")
                put("value", countdownSeconds)
            })
        } else {
            commands.put(JSONObject().apply {
                put("code", "countdown_1")
                put("value", 0)
            })
        }

        val payload = JSONObject().apply {
            put("commands", commands)
        }
        return@withContext sendDeviceCommands(payload)
    }

    /**
     * Sets the relay power-on restore status behavior.
     */
    suspend fun setRelayStatus(relayMode: String): Boolean = withContext(Dispatchers.IO) {
        val commands = JSONArray().apply {
            put(JSONObject().apply {
                put("code", "relay_status")
                put("value", relayMode)
            })
        }
        val payload = JSONObject().apply {
            put("commands", commands)
        }
        return@withContext sendDeviceCommands(payload)
    }

    private suspend fun sendDeviceCommands(payloadJson: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            val timestamp = System.currentTimeMillis().toString()
            val path = "/v1.0/devices/${credentials.deviceId}/commands"
            val url = "${credentials.regionUrl}$path"

            val jsonBodyStr = payloadJson.toString()
            val bodyHash = sha256(jsonBodyStr)
            val httpMethod = "POST"
            val headers = ""

            val stringToSign = "$httpMethod\n$bodyHash\n$headers\n$path"
            val signPayload = "${credentials.clientId}$token$timestamp$stringToSign"
            val sign = hmacSha256(signPayload, credentials.secret).uppercase()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBodyStr.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("client_id", credentials.clientId)
                .addHeader("access_token", token)
                .addHeader("sign", sign)
                .addHeader("t", timestamp)
                .addHeader("sign_method", "HMAC-SHA256")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val jsonStr = response.body?.string() ?: return@withContext false
                val json = JSONObject(jsonStr)
                return@withContext json.optBoolean("success", false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Fetches cloud schedules / timers configured for this device.
     */
    suspend fun getDeviceSchedules(): List<TuyaScheduleTimer> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            val timestamp = System.currentTimeMillis().toString()
            val path = "/v1.0/devices/${credentials.deviceId}/timers"
            val url = "${credentials.regionUrl}$path"

            val httpMethod = "GET"
            val bodyHash = sha256("")
            val headers = ""

            val stringToSign = "$httpMethod\n$bodyHash\n$headers\n$path"
            val signPayload = "${credentials.clientId}$token$timestamp$stringToSign"
            val sign = hmacSha256(signPayload, credentials.secret).uppercase()

            val request = Request.Builder()
                .url(url)
                .addHeader("client_id", credentials.clientId)
                .addHeader("access_token", token)
                .addHeader("sign", sign)
                .addHeader("t", timestamp)
                .addHeader("sign_method", "HMAC-SHA256")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val jsonStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(jsonStr)
                if (json.optBoolean("success")) {
                    val list = mutableListOf<TuyaScheduleTimer>()
                    val resultJson = json.opt("result")

                    val processTimersArray = { timersArray: JSONArray, gId: String ->
                        for (j in 0 until timersArray.length()) {
                            val timerObj = timersArray.optJSONObject(j)
                            if (timerObj != null) {
                                val tId = timerObj.optString("timer_id", "t_${list.size}")
                                val timeStr = timerObj.optString("time", "00:00")
                                val loops = timerObj.optString("loops", "0000000")
                                val status = timerObj.optInt("status", 1) == 1

                                val functions = timerObj.optJSONArray("functions")
                                var action = "Switch Action"
                                if (functions != null && functions.length() > 0) {
                                    val func = functions.getJSONObject(0)
                                    if (func.optString("code") == "switch_1") {
                                        action = if (func.optBoolean("value")) "Turn ON" else "Turn OFF"
                                    }
                                }

                                list.add(
                                    TuyaScheduleTimer(
                                        timerId = tId,
                                        groupId = gId,
                                        time = timeStr,
                                        daysFormatted = formatLoops(loops),
                                        actionText = action,
                                        isEnabled = status
                                    )
                                )
                            }
                        }
                    }

                    val processGroupObject = { groupObj: JSONObject ->
                        val gId = groupObj.optString("id", "")
                        val timersArr = groupObj.optJSONArray("timers")
                        if (timersArr != null) {
                            processTimersArray(timersArr, gId)
                        }
                    }

                    if (resultJson is JSONArray) {
                        for (i in 0 until resultJson.length()) {
                            val elem = resultJson.optJSONObject(i) ?: continue
                            val groupsArr = elem.optJSONArray("groups")
                            if (groupsArr != null) {
                                for (g in 0 until groupsArr.length()) {
                                    val groupObj = groupsArr.optJSONObject(g) ?: continue
                                    processGroupObject(groupObj)
                                }
                            } else if (elem.has("timers")) {
                                processGroupObject(elem)
                            } else if (elem.has("timer_id") || elem.has("time")) {
                                val timerObj = elem
                                val tId = timerObj.optString("timer_id", "t_${list.size}")
                                val timeStr = timerObj.optString("time", "00:00")
                                val loops = timerObj.optString("loops", "0000000")
                                val status = timerObj.optInt("status", 1) == 1

                                val functions = timerObj.optJSONArray("functions")
                                var action = "Switch Action"
                                if (functions != null && functions.length() > 0) {
                                    val func = functions.getJSONObject(0)
                                    if (func.optString("code") == "switch_1") {
                                        action = if (func.optBoolean("value")) "Turn ON" else "Turn OFF"
                                    }
                                }

                                list.add(
                                    TuyaScheduleTimer(
                                        timerId = tId,
                                        groupId = "",
                                        time = timeStr,
                                        daysFormatted = formatLoops(loops),
                                        actionText = action,
                                        isEnabled = status
                                    )
                                )
                            }
                        }
                    } else if (resultJson is JSONObject) {
                        val groupsArr = resultJson.optJSONArray("groups")
                        if (groupsArr != null) {
                            for (g in 0 until groupsArr.length()) {
                                val groupObj = groupsArr.optJSONObject(g) ?: continue
                                processGroupObject(groupObj)
                            }
                        } else {
                            processGroupObject(resultJson)
                        }
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * Creates a scheduled cloud timer on the device using Tuya OpenAPI.
     * Endpoint: POST /v1.0/devices/{device_id}/timers
     * Returns the created timer group_id on success, or null on failure.
     */
    suspend fun createDeviceTimer(
        time: String,
        turnOn: Boolean,
        loops: String = "1111111",
        timezoneId: String = "Europe/London"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            val timestamp = System.currentTimeMillis().toString()
            val path = "/v1.0/devices/${credentials.deviceId}/timers"
            val url = "${credentials.regionUrl}$path"

            val funcArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("code", "switch_1")
                    put("value", turnOn)
                })
            }
            val instructItem = JSONObject().apply {
                put("date", "00000000")
                put("time", time)
                put("functions", funcArr)
            }
            val instructArr = JSONArray().apply {
                put(instructItem)
            }

            val payloadJson = JSONObject().apply {
                put("loops", loops)
                put("timezone_id", timezoneId)
                put("category", "category_socket")
                put("instruct", instructArr)
            }

            val jsonBodyStr = payloadJson.toString()
            val bodyHash = sha256(jsonBodyStr)
            val httpMethod = "POST"
            val headers = ""

            val stringToSign = "$httpMethod\n$bodyHash\n$headers\n$path"
            val signPayload = "${credentials.clientId}$token$timestamp$stringToSign"
            val sign = hmacSha256(signPayload, credentials.secret).uppercase()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBodyStr.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("client_id", credentials.clientId)
                .addHeader("access_token", token)
                .addHeader("sign", sign)
                .addHeader("t", timestamp)
                .addHeader("sign_method", "HMAC-SHA256")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val jsonStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(jsonStr)
                if (json.optBoolean("success", false)) {
                    val resultObj = json.optJSONObject("result")
                    return@withContext resultObj?.optString("group_id", "created") ?: "created"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    /**
     * Deletes a scheduled timer group from the device.
     * Endpoint: DELETE /v1.0/devices/{device_id}/timers?group_id={groupId}
     */
    suspend fun deleteDeviceTimerGroup(groupId: String): Boolean = withContext(Dispatchers.IO) {
        if (groupId.isBlank()) return@withContext false
        try {
            val token = getAccessToken()
            val timestamp = System.currentTimeMillis().toString()
            val path = "/v1.0/devices/${credentials.deviceId}/timers"
            val queryParam = "group_id=$groupId"
            val url = "${credentials.regionUrl}$path?$queryParam"

            val bodyHash = sha256("")
            val httpMethod = "DELETE"
            val headers = ""

            val stringToSign = "$httpMethod\n$bodyHash\n$headers\n$path?$queryParam"
            val signPayload = "${credentials.clientId}$token$timestamp$stringToSign"
            val sign = hmacSha256(signPayload, credentials.secret).uppercase()

            val request = Request.Builder()
                .url(url)
                .addHeader("client_id", credentials.clientId)
                .addHeader("access_token", token)
                .addHeader("sign", sign)
                .addHeader("t", timestamp)
                .addHeader("sign_method", "HMAC-SHA256")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                val jsonStr = response.body?.string() ?: return@withContext false
                val json = JSONObject(jsonStr)
                return@withContext json.optBoolean("success", false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    private fun formatLoops(loops: String): String {
        if (loops == "1111111") return "Everyday"
        if (loops == "1111100" || loops == "0111110") return "Mon-Fri"
        if (loops == "0000011") return "Weekends"
        if (loops == "0000000") return "Once"

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val activeDays = mutableListOf<String>()
        for (i in 0 until minOf(loops.length, 7)) {
            if (loops[i] == '1') activeDays.add(days[i])
        }
        return if (activeDays.isEmpty()) "Once" else activeDays.joinToString(", ")
    }

    companion object {
        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        }

        fun hmacSha256(data: String, key: String): String {
            val sha256HMAC = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
            sha256HMAC.init(secretKey)
            val hash = sha256HMAC.doFinal(data.toByteArray(Charsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
}
