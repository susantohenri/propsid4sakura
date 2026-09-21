package com.test.propsid4sakura.data.repository

import com.test.propsid4sakura.data.local.AppDatabase
import com.test.propsid4sakura.data.local.PropEntity
import com.test.propsid4sakura.data.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for props data.
 *
 * Strategy:
 * 1. UI observes Room DB via Flow (always up-to-date, offline-first).
 * 2. [syncFromRemote] is called in background to fetch fresh data and upsert into DB.
 *    - Uses INSERT IGNORE + UPDATE metadata to preserve unlocked/favorite flags.
 *    - Falls back to asset JSON if DB is empty on first launch.
 */
class PropsRepository(
    private val db: AppDatabase,
    private val remoteDataSource: RemoteDataSource
) {
    private val dao = db.propDao()

    /** Observe all props from local DB (Room Flow) */
    fun observeAllProps(): Flow<List<PropEntity>> = dao.getAllProps()

    /** Observe favorite props from local DB */
    fun observeFavorites(): Flow<List<PropEntity>> = dao.getFavorites()

    /** Get a single prop by ID */
    suspend fun getPropById(propId: String): PropEntity? = dao.getPropById(propId)

    /** Mark a prop as unlocked (persisted permanently) */
    suspend fun unlockProp(propId: String) = withContext(Dispatchers.IO) {
        dao.unlockProp(propId)
    }

    /** Toggle favorite status */
    suspend fun setFavorite(propId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(propId, isFavorite)
    }

    /**
     * Sync from remote. Called in background after app start.
     * Never throws — silently falls back on error.
     */
    suspend fun syncFromRemote() = withContext(Dispatchers.IO) {
        // If DB is empty, seed from assets first so UI is never blank
        if (dao.count() == 0) {
            seedFromAssets()
        }

        remoteDataSource.fetchProps()
            .onSuccess { items ->
                val now = System.currentTimeMillis()
                val entities = items.filter { it.propId.isNotBlank() }.map { item ->
                    PropEntity(
                        propId = item.propId,
                        title = item.title,
                        category = item.category,
                        cachedAt = now
                    )
                }
                // Insert new props (IGNORE if already exists, preserving unlock/fav)
                dao.insertAll(entities)
                // Update metadata for existing props
                entities.forEach { e ->
                    dao.updateMetadata(e.propId, e.title, e.category, now)
                }
            }
        // On failure: existing DB data continues to be served — no action needed
    }

    private suspend fun seedFromAssets() {
        val items = remoteDataSource.loadAssetProps()
        val entities = items.filter { it.propId.isNotBlank() }.map { item ->
            PropEntity(
                propId = item.propId,
                title = item.title,
                category = item.category
            )
        }
        dao.insertAll(entities)
    }
}
