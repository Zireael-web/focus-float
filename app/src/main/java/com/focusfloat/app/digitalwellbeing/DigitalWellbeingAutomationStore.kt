package com.focusfloat.app.digitalwellbeing

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class DigitalWellbeingAutomationMode {
    EnableFocusMode,
    DisableFocusMode,
}

enum class DigitalWellbeingAutomationState {
    Idle,
    Running,
    Completed,
    Failed,
}

data class DigitalWellbeingAutomationTarget(
    val packageName: String,
    val label: String,
    val userSerial: Long,
)

data class DigitalWellbeingAutomationRequest(
    val id: String,
    val mode: DigitalWellbeingAutomationMode,
    val targets: List<DigitalWellbeingAutomationTarget>,
    val createdAtEpochMs: Long,
)

data class DigitalWellbeingAutomationStatus(
    val state: DigitalWellbeingAutomationState,
    val message: String?,
    val completed: Int,
    val total: Int,
)

class DigitalWellbeingAutomationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun start(mode: DigitalWellbeingAutomationMode, targets: List<DigitalWellbeingAutomationTarget>): DigitalWellbeingAutomationRequest {
        val request = DigitalWellbeingAutomationRequest(
            id = UUID.randomUUID().toString(),
            mode = mode,
            targets = targets.distinctBy { it.packageName to it.userSerial },
            createdAtEpochMs = System.currentTimeMillis(),
        )
        val message = when (mode) {
            DigitalWellbeingAutomationMode.EnableFocusMode -> "Opening Pixel Focus Mode to select apps"
            DigitalWellbeingAutomationMode.DisableFocusMode -> "Opening Pixel Focus Mode to check selected apps"
        }
        val total = if (mode == DigitalWellbeingAutomationMode.EnableFocusMode) request.targets.size else 0
        prefs.edit()
            .putString(KEY_REQUEST, request.toJson().toString())
            .putString(KEY_STATE, DigitalWellbeingAutomationState.Running.name)
            .putString(KEY_MESSAGE, message)
            .putInt(KEY_COMPLETED, 0)
            .putInt(KEY_TOTAL, total)
            .apply()
        return request
    }

    fun activeRequest(): DigitalWellbeingAutomationRequest? {
        val state = status().state
        if (state != DigitalWellbeingAutomationState.Running) return null
        val request = prefs.getString(KEY_REQUEST, null)?.let { raw ->
            runCatching { JSONObject(raw).toRequest() }.getOrElse {
                fail("Focus Mode assistant request could not be read")
                return null
            }
        } ?: run {
            fail("Focus Mode assistant request is missing")
            return null
        }
        if (System.currentTimeMillis() - request.createdAtEpochMs > REQUEST_TTL_MS) {
            fail(request.id, "Focus Mode assistant timed out")
            return null
        }
        return request
    }

    fun status(): DigitalWellbeingAutomationStatus {
        return DigitalWellbeingAutomationStatus(
            state = prefs.getString(KEY_STATE, null)
                ?.let { runCatching { enumValueOf<DigitalWellbeingAutomationState>(it) }.getOrNull() }
                ?: DigitalWellbeingAutomationState.Idle,
            message = prefs.getString(KEY_MESSAGE, null),
            completed = prefs.getInt(KEY_COMPLETED, 0),
            total = prefs.getInt(KEY_TOTAL, 0),
        )
    }

    fun updateProgress(message: String, completed: Int, total: Int) {
        prefs.edit()
            .putString(KEY_STATE, DigitalWellbeingAutomationState.Running.name)
            .putString(KEY_MESSAGE, message)
            .putInt(KEY_COMPLETED, completed)
            .putInt(KEY_TOTAL, total)
            .apply()
    }

    fun updateProgress(requestId: String, message: String, completed: Int, total: Int) {
        if (!isActiveRequest(requestId)) return
        updateProgress(message, completed, total)
    }

    fun complete(message: String) {
        prefs.edit()
            .remove(KEY_REQUEST)
            .putString(KEY_STATE, DigitalWellbeingAutomationState.Completed.name)
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    fun complete(requestId: String, message: String) {
        if (!isActiveRequest(requestId)) return
        complete(message)
    }

    fun fail(message: String) {
        prefs.edit()
            .remove(KEY_REQUEST)
            .putString(KEY_STATE, DigitalWellbeingAutomationState.Failed.name)
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    fun fail(requestId: String, message: String) {
        if (!isActiveRequest(requestId)) return
        fail(message)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun isActiveRequest(requestId: String): Boolean {
        if (status().state != DigitalWellbeingAutomationState.Running) return false
        val raw = prefs.getString(KEY_REQUEST, null) ?: return false
        return runCatching { JSONObject(raw).optString("id") == requestId }.getOrDefault(false)
    }

    private fun DigitalWellbeingAutomationRequest.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("mode", mode.name)
            .put("createdAtEpochMs", createdAtEpochMs)
            .put(
                "targets",
                JSONArray().also { array ->
                    targets.forEach { target ->
                        array.put(
                            JSONObject()
                                .put("packageName", target.packageName)
                                .put("label", target.label)
                                .put("userSerial", target.userSerial),
                        )
                    }
                },
            )
    }

    private fun JSONObject.toRequest(): DigitalWellbeingAutomationRequest {
        val targetsJson = getJSONArray("targets")
        val targets = buildList {
            for (index in 0 until targetsJson.length()) {
                val item = targetsJson.getJSONObject(index)
                add(
                    DigitalWellbeingAutomationTarget(
                        packageName = item.getString("packageName"),
                        label = item.getString("label"),
                        userSerial = item.optLong("userSerial", 0L),
                    ),
                )
            }
        }
        return DigitalWellbeingAutomationRequest(
            id = getString("id"),
            mode = enumValueOf(getString("mode")),
            targets = targets,
            createdAtEpochMs = getLong("createdAtEpochMs"),
        )
    }

    private companion object {
        const val PREFS_NAME = "digital_wellbeing_automation"
        const val KEY_REQUEST = "request"
        const val KEY_STATE = "state"
        const val KEY_MESSAGE = "message"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
        const val REQUEST_TTL_MS = 5 * 60 * 1_000L
    }
}
