package com.test.propsid4sakura.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PropDao {

    /**
     * Insert or update props from remote.
     * Uses IGNORE on conflict so that existing [isUnlocked] and [isFavorite] flags are preserved.
     * We then do a selective update for title/category only.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(props: List<PropEntity>)

    /**
     * Update only metadata (title, category) without touching unlock/favorite status.
     */
    @Query("""
        UPDATE props
        SET title = :title, category = :category, cachedAt = :cachedAt
        WHERE propId = :propId
    """)
    suspend fun updateMetadata(propId: String, title: String, category: String, cachedAt: Long)

    @Query("SELECT * FROM props ORDER BY category ASC, title ASC")
    fun getAllProps(): Flow<List<PropEntity>>

    @Query("SELECT * FROM props WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<PropEntity>>

    @Query("SELECT * FROM props WHERE propId = :propId")
    suspend fun getPropById(propId: String): PropEntity?

    @Query("UPDATE props SET isUnlocked = 1 WHERE propId = :propId")
    suspend fun unlockProp(propId: String)

    @Query("UPDATE props SET isFavorite = :isFavorite WHERE propId = :propId")
    suspend fun setFavorite(propId: String, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM props")
    suspend fun count(): Int
}
