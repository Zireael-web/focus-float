package com.focusfloat.app.pause.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.focusfloat.app.FocusFloatApplication
import com.focusfloat.app.pause.model.PauseExecutorUnavailableException
import com.focusfloat.app.pause.model.PauseReconcileRetryException

class PauseReconcileWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as FocusFloatApplication
        val reason = inputData.getString(KEY_REASON) ?: "worker"
        return try {
            app.container.pauseController.reconcile(reason)
            Result.success()
        } catch (_: PauseExecutorUnavailableException) {
            Result.retry()
        } catch (_: PauseReconcileRetryException) {
            Result.retry()
        } catch (_: Throwable) {
            Result.failure()
        }
    }

    companion object {
        private const val KEY_REASON = "reason"

        fun inputData(reason: String): Data {
            return Data.Builder().putString(KEY_REASON, reason).build()
        }
    }
}
