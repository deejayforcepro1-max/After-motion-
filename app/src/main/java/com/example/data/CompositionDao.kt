package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompositionDao {
    @Query("SELECT * FROM compositions ORDER BY updatedAtMs DESC")
    fun observeAllCompositions(): Flow<List<CompositionEntity>>

    @Query("SELECT * FROM compositions WHERE id = :id LIMIT 1")
    suspend fun getCompositionById(id: String): CompositionEntity?

    @Query("SELECT COUNT(*) FROM compositions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComposition(entity: CompositionEntity)

    @Query("DELETE FROM compositions WHERE id = :id")
    suspend fun deleteById(id: String)
}
