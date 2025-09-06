package com.focusfloat.app.pause.data

import com.focusfloat.app.core.model.AppCategory
import com.focusfloat.app.core.model.AppRef
import com.focusfloat.app.core.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

interface CategoryRepository {
    fun observeCategories(): Flow<List<AppCategory>>
    fun observeCategory(categoryId: Long): Flow<AppCategory?>
    fun observeCategoryItems(categoryId: Long): Flow<List<AppRef>>
    suspend fun getPackageRefs(categoryId: Long): List<AppRef>
    suspend fun ensureDefaultPauseCategory(): Long
    suspend fun ensureDefaultDistractingCategory(): Long
    suspend fun createCategory(name: String, type: CategoryType): Long
    suspend fun renameCategory(categoryId: Long, name: String)
    suspend fun deleteCategory(categoryId: Long)
    suspend fun addPackage(categoryId: Long, packageName: String, userSerial: Long = 0)
    suspend fun removePackage(categoryId: Long, packageName: String, userSerial: Long = 0)
}

class RoomCategoryRepository(
    private val dao: CategoryDao,
    private val clock: Clock,
) : CategoryRepository {
    override fun observeCategories(): Flow<List<AppCategory>> {
        return dao.observeCategories().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeCategory(categoryId: Long): Flow<AppCategory?> {
        return dao.observeCategory(categoryId).map { it?.toDomain() }
    }

    override fun observeCategoryItems(categoryId: Long): Flow<List<AppRef>> {
        return dao.observeCategoryPackages(categoryId)
    }

    override suspend fun getPackageRefs(categoryId: Long): List<AppRef> {
        return dao.getCategoryPackages(categoryId)
    }

    override suspend fun ensureDefaultPauseCategory(): Long {
        return dao.ensureDefaultPauseCategory(clock.millis())
    }

    override suspend fun ensureDefaultDistractingCategory(): Long {
        return dao.ensureDefaultDistractingCategory(clock.millis())
    }

    override suspend fun createCategory(name: String, type: CategoryType): Long {
        val now = clock.millis()
        return dao.insertCategory(
            CategoryEntity(
                name = name.trim().ifBlank { defaultName(type) },
                type = type.name,
                showOnHome = type == CategoryType.PauseGroup,
                sortOrder = 0,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    override suspend fun renameCategory(categoryId: Long, name: String) {
        val existing = dao.getCategory(categoryId) ?: return
        dao.updateCategory(existing.copy(name = name.trim().ifBlank { existing.name }, updatedAtEpochMs = clock.millis()))
    }

    override suspend fun deleteCategory(categoryId: Long) {
        dao.deleteCategory(categoryId)
    }

    override suspend fun addPackage(categoryId: Long, packageName: String, userSerial: Long) {
        dao.insertItem(
            CategoryItemEntity(
                categoryId = categoryId,
                packageName = packageName,
                userSerial = userSerial,
                addedAtEpochMs = clock.millis(),
            ),
        )
    }

    override suspend fun removePackage(categoryId: Long, packageName: String, userSerial: Long) {
        dao.removeItem(categoryId, packageName, userSerial)
    }
}

private fun CategoryEntity.toDomain() = AppCategory(
    id = id,
    name = name,
    type = runCatching { enumValueOf<CategoryType>(type) }.getOrDefault(CategoryType.Folder),
    showOnHome = showOnHome,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
)

private fun defaultName(type: CategoryType): String {
    return when (type) {
        CategoryType.PauseGroup -> "Paused apps"
        CategoryType.DistractingGroup -> "Distracting apps"
        CategoryType.Folder -> "Folder"
    }
}
