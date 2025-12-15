package com.focusfloat.app.pause.executor

import com.focusfloat.app.pause.model.PauseExecutorType

class PauseExecutorSelector(
    private val wellbeingRoot: PauseExecutor,
    private val shizuku: PauseExecutor,
    private val deviceOwner: PauseExecutor,
    private val manual: PauseExecutor,
) {
    fun select(): PauseExecutor = wellbeingRoot

    fun select(type: PauseExecutorType): PauseExecutor {
        return when (type) {
            PauseExecutorType.WellbeingRoot -> wellbeingRoot
            PauseExecutorType.ShizukuShell -> shizuku
            PauseExecutorType.DeviceOwner -> deviceOwner
            PauseExecutorType.ManualOnly -> manual
        }
    }
}
