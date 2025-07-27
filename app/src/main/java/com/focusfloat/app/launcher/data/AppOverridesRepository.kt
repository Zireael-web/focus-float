package com.focusfloat.app.launcher.data

import com.focusfloat.app.pause.data.AppOverrideDao
import com.focusfloat.app.pause.data.AppOverrideEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock

class AppOverridesRepository(
    private val dao: AppOverrideDao,
    private val clock: Clock,
) {
    private val mutationLock = Mutex()

    suspend fun setFavorite(packageName: String, userSerial: Long, favorite: Boolean) = mutationLock.withLock {
        val existing = dao.get(packageName, userSerial)
        val now = clock.millis()
        val favoriteOrder = if (favorite) existing?.favoriteOrder ?: nextFavoriteOrder() else null
        dao.upsert((existing ?: defaultOverride(packageName, userSerial, now)).copy(favoriteOrder = favoriteOrder, updatedAtEpochMs = now))
    }

    suspend fun setHidden(packageName: String, userSerial: Long, hidden: Boolean) = mutationLock.withLock {
        val existing = dao.get(packageName, userSerial)
        val now = clock.millis()
        dao.upsert((existing ?: defaultOverride(packageName, userSerial, now)).copy(hidden = hidden, updatedAtEpochMs = now))
    }

    suspend fun rename(packageName: String, userSerial: Long, customLabel: String?) = mutationLock.withLock {
        val existing = dao.get(packageName, userSerial)
        val now = clock.millis()
        dao.upsert(
            (existing ?: defaultOverride(packageName, userSerial, now))
                .copy(customLabel = customLabel?.trim()?.takeIf { it.isNotBlank() }, updatedAtEpochMs = now),
        )
    }

    private suspend fun nextFavoriteOrder(): Int {
        return (dao.getAll().mapNotNull { it.favoriteOrder }.maxOrNull() ?: -1) + 1
    }

    private fun defaultOverride(packageName: String, userSerial: Long, now: Long): AppOverrideEntity {
        return AppOverrideEntity(
            packageName = packageName,
            userSerial = userSerial,
            customLabel = null,
            hidden = false,
            favoriteOrder = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
    }
}
