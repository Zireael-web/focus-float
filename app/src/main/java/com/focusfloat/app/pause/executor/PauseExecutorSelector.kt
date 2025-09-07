package com.focusfloat.app.pause.executor

import com.focusfloat.app.pause.model.PauseExecutorPreference
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.settings.SettingsRepository

class PauseExecutorSelector(
    private val wellbeingRoot: PauseExecutor,
    private val shizuku: PauseExecutor,
    private val deviceOwner: PauseExecutor,
    private val manual: PauseExecutor,
    private val settings: SettingsRepository,
) {
    suspend fun select(): PauseExecutor {
        return when (settings.pauseExecutorPreference()) {
            PauseExecutorPreference.WellbeingRoot,
            PauseExecutorPreference.Shizuku,
            PauseExecutorPreference.DeviceOwner,
            PauseExecutorPreference.Manual,
            PauseExecutorPreference.Auto -> wellbeingRoot
        }
    }

    fun select(type: PauseExecutorType): PauseExecutor {
        return when (type) {
            PauseExecutorType.WellbeingRoot -> wellbeingRoot
            PauseExecutorType.ShizukuShell -> shizuku
            PauseExecutorType.DeviceOwner -> deviceOwner
            PauseExecutorType.ManualOnly -> manual
        }
    }
}
