package com.test.propsid4sakura.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing one prop in the local database.
 *
 * [isUnlocked]: once true, the prop is unlocked permanently without needing another ad.
 * [isFavorite]: user-toggled favorite flag.
 */
@Entity(tableName = "props")
data class PropEntity(
    @PrimaryKey val propId: String,
    val title: String,
    val category: String,
    val isUnlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)
