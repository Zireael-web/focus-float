package com.focusfloat.app.pause.data

import com.focusfloat.app.pause.model.NewPauseSession
import com.focusfloat.app.pause.model.PauseAction
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.pause.model.PauseOperationResult
import com.focusfloat.app.pause.model.PauseSession
import com.focusfloat.app.pause.model.PauseSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

interface PauseSessionRepository {
    fun observeActiveSessionForCategory(categoryId: Long): Flow<PauseSession?>
    fun observeActiveSessions(): Flow<List<PauseSession>>
    suspend fun getActiveSessionForCategory(categoryId: Long): PauseSession?
    suspend fun getActiveSessionsForCategory(categoryId: Long): List<PauseSession>
    suspend fun getSessionsNeedingReconcile(): List<PauseSession>
    suspend fun createSession(session: NewPauseSession): Long
    suspend fun updateStatus(sessionId: Long, status: PauseSessionStatus, failureSummary: String? = null)
    suspend fun updatePackages(sessionId: Long, packageNames: List<String>)
    suspend fun updatePausedUntil(sessionId: Long, pausedUntil: Instant)
    suspend fun savePackageResults(sessionId: Long, userSerial: Long, results: PauseOperationResult, action: PauseAction)
}

class RoomPauseSessionRepository(
    private val dao: PauseSessionDao,
    private val clock: Clock,
) : PauseSessionRepository {
    override fun observeActiveSessionForCategory(categoryId: Long): Flow<PauseSession?> {
        return dao.observeActiveSessionForCategory(categoryId).map { it?.toDomain() }
    }

    override fun observeActiveSessions(): Flow<List<PauseSession>> {
        return dao.observeActiveSessions().map { sessions -> sessions.map { it.toDomain() } }
    }

    override suspend fun getActiveSessionForCategory(categoryId: Long): PauseSession? {
        return dao.getActiveSessionForCategory(categoryId)?.toDomain()
    }

    override suspend fun getActiveSessionsForCategory(categoryId: Long): List<PauseSession> {
        return dao.getActiveSessionsForCategory(categoryId).map { it.toDomain() }
    }

    override suspend fun getSessionsNeedingReconcile(): List<PauseSession> {
        return dao.getSessionsNeedingReconcile().map { it.toDomain() }
    }

    override suspend fun createSession(session: NewPauseSession): Long {
        return dao.insertSession(
            PauseSessionEntity(
                categoryId = session.categoryId,
                packageNamesJson = session.packageNames.toJsonArrayString(),
                userSerial = session.userSerial,
                startedAtEpochMs = session.startedAt.toEpochMilli(),
                pausedUntilEpochMs = session.pausedUntil.toEpochMilli(),
                status = session.status.name,
                executorType = session.executorType.name,
                failureSummary = null,
            ),
        )
    }

    override suspend fun updateStatus(sessionId: Long, status: PauseSessionStatus, failureSummary: String?) {
        dao.updateStatus(sessionId, status.name, failureSummary)
    }

    override suspend fun updatePackages(sessionId: Long, packageNames: List<String>) {
        dao.updatePackages(sessionId, packageNames.distinct().toJsonArrayString())
    }

    override suspend fun updatePausedUntil(sessionId: Long, pausedUntil: Instant) {
        dao.updatePausedUntil(sessionId, pausedUntil.toEpochMilli())
    }

    override suspend fun savePackageResults(
        sessionId: Long,
        userSerial: Long,
        results: PauseOperationResult,
        action: PauseAction,
    ) {
        val now = clock.millis()
        val succeeded = results.succeeded.map {
            PausePackageResultEntity(
                sessionId = sessionId,
                packageName = it,
                userSerial = userSerial,
                action = action.name,
                success = true,
                exitCode = 0,
                stdout = null,
                stderr = null,
                createdAtEpochMs = now,
            )
        }
        val failed = results.failed.map {
            PausePackageResultEntity(
                sessionId = sessionId,
                packageName = it.packageName,
                userSerial = userSerial,
                action = action.name,
                success = false,
                exitCode = it.exitCode,
                stdout = null,
                stderr = null,
                createdAtEpochMs = now,
            )
        }
        dao.insertResults(succeeded + failed)
    }
}

private fun PauseSessionEntity.toDomain(): PauseSession {
    val parsedPackages = packageNamesJson.toStringListFromJsonArrayOrNull()
    return PauseSession(
        id = id,
        categoryId = categoryId,
        packageNames = parsedPackages.orEmpty(),
        userSerial = userSerial,
        startedAt = Instant.ofEpochMilli(startedAtEpochMs),
        pausedUntil = Instant.ofEpochMilli(pausedUntilEpochMs),
        status = runCatching { enumValueOf<PauseSessionStatus>(status) }
            .getOrDefault(PauseSessionStatus.FailedToUnpause),
        executorType = executorType.toPauseExecutorType(),
        failureSummary = failureSummary,
        packageListCorrupt = parsedPackages == null,
    )
}

private fun String.toPauseExecutorType(): PauseExecutorType {
    return when (this) {
        "WellbeingRoot" -> PauseExecutorType.WellbeingRoot
        "Shizuku",
        "ShizukuShell" -> PauseExecutorType.ShizukuShell
        "DeviceOwner" -> PauseExecutorType.DeviceOwner
        "Manual",
        "ManualOnly" -> PauseExecutorType.ManualOnly
        else -> PauseExecutorType.WellbeingRoot
    }
}
