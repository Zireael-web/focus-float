package com.focusfloat.app.pause.controller

import android.content.Context
import com.focusfloat.app.core.time.endOfToday
import com.focusfloat.app.pause.data.CategoryRepository
import com.focusfloat.app.pause.data.PauseSessionRepository
import com.focusfloat.app.pause.executor.PauseExecutor
import com.focusfloat.app.pause.executor.PauseExecutorSelector
import com.focusfloat.app.pause.model.NewPauseSession
import com.focusfloat.app.pause.model.PauseAction
import com.focusfloat.app.pause.model.PauseExecutorUnavailableException
import com.focusfloat.app.pause.model.PauseOperationResult
import com.focusfloat.app.pause.model.PausePackageFailure
import com.focusfloat.app.pause.model.PauseReconcileRetryException
import com.focusfloat.app.pause.model.PauseSession
import com.focusfloat.app.pause.model.PauseSessionStatus
import com.focusfloat.app.pause.notifications.PauseNotificationController
import com.focusfloat.app.pause.safety.ProtectedPackages
import com.focusfloat.app.pause.scheduler.PauseExpiryScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock

class PauseController(
    private val categoryRepository: CategoryRepository,
    private val sessionRepository: PauseSessionRepository,
    private val executorSelector: PauseExecutorSelector,
    private val scheduler: PauseExpiryScheduler,
    private val notificationController: PauseNotificationController,
    private val clock: Clock,
    private val context: Context,
) {
    private val lifecycleMutex = Mutex()

    suspend fun pauseCategoryUntilMidnight(categoryId: Long): PauseOperationResult = lifecycleMutex.withLock {
        val requestedRefs = categoryRepository.getPackageRefs(categoryId).distinct()
        val protectedRefs = requestedRefs.filter { ProtectedPackages.isProtected(it.packageName, context) }
        val refsToPause = requestedRefs.filterNot { it in protectedRefs }
        if (refsToPause.isEmpty()) {
            return PauseOperationResult(
                requested = requestedRefs.map { it.packageName },
                succeeded = emptyList(),
                failed = protectedRefs.map { PausePackageFailure(it.packageName, "Protected package") },
            )
        }

        val until = endOfToday(clock)
        val cleanup = unpauseActiveSessionsForCategory(categoryId)
        if (cleanup.failed.isNotEmpty()) return cleanup

        val executor = executorSelector.select()
        val availability = executor.availability()
        if (!availability.available) throw PauseExecutorUnavailableException(availability)

        val succeeded = mutableListOf<String>()
        val failed = protectedRefs.mapTo(mutableListOf()) { PausePackageFailure(it.packageName, "Protected package") }

        for ((userSerial, refs) in refsToPause.groupBy { it.userSerial }) {
            val packages = refs.map { it.packageName }.distinct()
            val sessionId = sessionRepository.createSession(
                NewPauseSession(
                    categoryId = categoryId,
                    packageNames = packages,
                    userSerial = userSerial,
                    startedAt = clock.instant(),
                    pausedUntil = until,
                    status = PauseSessionStatus.Pending,
                    executorType = executor.type,
                ),
            )

            val result = executor.pausePackages(
                packageNames = packages,
                userSerial = userSerial,
                dialogMessage = "Paused until midnight",
            )

            sessionRepository.savePackageResults(sessionId, userSerial, result, PauseAction.Pause)
            sessionRepository.updatePackages(sessionId, result.succeeded)

            val status = when {
                result.failed.isEmpty() -> PauseSessionStatus.Active
                result.succeeded.isNotEmpty() -> PauseSessionStatus.PartiallyFailed
                else -> PauseSessionStatus.Cancelled
            }
            sessionRepository.updateStatus(sessionId, status, failureSummary(result))
            succeeded += result.succeeded
            failed += result.failed
        }

        if (succeeded.isNotEmpty()) {
            val scheduleResult = scheduler.schedule(until)
            if (!scheduleResult.exactAlarmScheduled) {
                notificationController.showExactAlarmPermissionNeeded()
            }
            val warnings = if (!scheduleResult.exactAlarmScheduled) {
                listOf("Allow Alarms & reminders permission for exact 00:00 unpause.")
            } else {
                emptyList()
            }
            return@withLock PauseOperationResult(
                requested = requestedRefs.map { it.packageName },
                succeeded = succeeded,
                failed = failed,
                warnings = warnings,
            )
        }
        PauseOperationResult(
            requested = requestedRefs.map { it.packageName },
            succeeded = succeeded,
            failed = failed,
        )
    }

    suspend fun unpauseCategoryNow(categoryId: Long): PauseOperationResult = lifecycleMutex.withLock {
        val sessions = sessionRepository.getActiveSessionsForCategory(categoryId)
        if (sessions.isEmpty()) return@withLock PauseOperationResult(emptyList(), emptyList(), emptyList())
        val results = sessions.map { unpauseSession(it, manual = true) }
        results.combine()
    }

    suspend fun unpauseAppNow(packageName: String, userSerial: Long): PauseOperationResult = lifecycleMutex.withLock {
        if (ProtectedPackages.isProtected(packageName, context)) {
            return@withLock PauseOperationResult(
                requested = listOf(packageName),
                succeeded = emptyList(),
                failed = listOf(PausePackageFailure(packageName, "Protected package")),
            )
        }

        val matchingSessions = sessionRepository.getSessionsNeedingReconcile()
            .filter { it.userSerial == userSerial && packageName in it.packageNames }

        if (matchingSessions.isEmpty()) {
            val executor = executorSelector.select()
            val availability = executor.availability()
            if (!availability.available) throw PauseExecutorUnavailableException(availability)
            return@withLock executor.unpausePackages(listOf(packageName), userSerial)
        }

        val results = mutableListOf<PauseOperationResult>()
        for ((executorType, sessions) in matchingSessions.groupBy { it.executorType }) {
            val executor = executorSelector.select(executorType)
            val availability = executor.availability()
            if (!availability.available) {
                sessions.forEach { session ->
                    sessionRepository.updateStatus(session.id, PauseSessionStatus.WaitingForShizuku)
                    notificationController.showShizukuNeededForUnpause(session)
                }
                throw PauseExecutorUnavailableException(availability)
            }

            val result = executor.unpausePackages(listOf(packageName), userSerial)
            results += result
            for (session in sessions) {
                sessionRepository.savePackageResults(session.id, userSerial, result, PauseAction.Unpause)
                val succeeded = result.succeeded.toSet()
                val remainingPackages = session.packageNames.filterNot { it in succeeded }
                if (remainingPackages.size != session.packageNames.size) {
                    sessionRepository.updatePackages(session.id, remainingPackages)
                }
                if (remainingPackages.isEmpty()) {
                    sessionRepository.updateStatus(
                        session.id,
                        PauseSessionStatus.Cancelled,
                        failureSummary(result),
                    )
                } else if (result.failed.isEmpty()) {
                    sessionRepository.updateStatus(session.id, PauseSessionStatus.Active)
                } else {
                    sessionRepository.updateStatus(session.id, PauseSessionStatus.FailedToUnpause, failureSummary(result))
                    notificationController.showUnpauseFailed(session, result.failed)
                }
            }
        }

        results.combine()
    }

    suspend fun reconcile(reason: String): Unit = lifecycleMutex.withLock {
        val now = clock.instant()
        val sessions = sessionRepository.getSessionsNeedingReconcile()
        val timezoneChanged = reason == android.content.Intent.ACTION_TIMEZONE_CHANGED
        var retryRequired = false
        var nextExpiry: java.time.Instant? = null
        for (session in sessions) {
            if (session.pausedUntil <= now) {
                val result = unpauseSession(session, manual = false)
                if (result.failed.isNotEmpty()) {
                    retryRequired = true
                }
            } else {
                val pausedUntil = if (timezoneChanged) {
                    endOfToday(clock).also { adjusted -> sessionRepository.updatePausedUntil(session.id, adjusted) }
                } else {
                    session.pausedUntil
                }
                if (nextExpiry == null || pausedUntil < nextExpiry) {
                    nextExpiry = pausedUntil
                }
            }
        }
        nextExpiry?.let { scheduler.schedule(it) }
        if (retryRequired) {
            throw PauseReconcileRetryException("Unpause failed during $reason reconcile")
        }
    }

    private suspend fun unpauseActiveSessionsForCategory(categoryId: Long): PauseOperationResult {
        val sessions = sessionRepository.getActiveSessionsForCategory(categoryId)
        if (sessions.isEmpty()) return PauseOperationResult(emptyList(), emptyList(), emptyList())
        return sessions.map { unpauseSession(it, manual = true) }.combine()
    }

    private suspend fun unpauseSession(
        session: PauseSession,
        manual: Boolean,
        executor: PauseExecutor? = null,
    ): PauseOperationResult {
        if (session.packageListCorrupt || session.packageNames.isEmpty()) {
            val failure = PausePackageFailure(
                packageName = "<stored session>",
                reason = "Stored package list is empty or corrupted; manual recovery may be required.",
            )
            sessionRepository.updateStatus(session.id, PauseSessionStatus.FailedToUnpause, failure.reason)
            notificationController.showUnpauseFailed(session, listOf(failure))
            return PauseOperationResult(emptyList(), emptyList(), listOf(failure))
        }

        val selectedExecutor = executor ?: executorSelector.select(session.executorType)
        val availability = selectedExecutor.availability()
        if (!availability.available) {
            sessionRepository.updateStatus(session.id, PauseSessionStatus.WaitingForShizuku)
            notificationController.showShizukuNeededForUnpause(session)
            throw PauseExecutorUnavailableException(availability)
        }

        val result = selectedExecutor.unpausePackages(session.packageNames, session.userSerial)
        sessionRepository.savePackageResults(session.id, session.userSerial, result, PauseAction.Unpause)
        val remainingPackages = session.packageNames.filterNot { it in result.succeeded.toSet() }
        if (remainingPackages.size != session.packageNames.size) {
            sessionRepository.updatePackages(session.id, remainingPackages)
        }

        if (result.failed.isEmpty()) {
            sessionRepository.updateStatus(
                session.id,
                if (manual) PauseSessionStatus.Cancelled else PauseSessionStatus.Expired,
            )
        } else if (remainingPackages.isEmpty()) {
            sessionRepository.updateStatus(
                session.id,
                if (manual) PauseSessionStatus.Cancelled else PauseSessionStatus.Expired,
                failureSummary(result),
            )
        } else {
            sessionRepository.updateStatus(session.id, PauseSessionStatus.FailedToUnpause, failureSummary(result))
            notificationController.showUnpauseFailed(session, result.failed)
        }
        return result
    }

    private fun failureSummary(result: PauseOperationResult): String? {
        val failed = result.failed.takeIf { it.isNotEmpty() } ?: return null
        val visibleFailures = failed.take(MAX_FAILURE_SUMMARY_ITEMS)
            .joinToString("\n") { "${it.packageName}: ${it.reason}" }
        val remaining = failed.size - MAX_FAILURE_SUMMARY_ITEMS
        return if (remaining > 0) {
            "$visibleFailures\n+ $remaining more"
        } else {
            visibleFailures
        }
    }

    private fun List<PauseOperationResult>.combine(): PauseOperationResult {
        return PauseOperationResult(
            requested = flatMap { it.requested },
            succeeded = flatMap { it.succeeded },
            failed = flatMap { it.failed },
            warnings = flatMap { it.warnings },
        )
    }

    private companion object {
        const val MAX_FAILURE_SUMMARY_ITEMS = 4
    }
}
