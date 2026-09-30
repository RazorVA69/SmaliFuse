package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MergeDao {
    @Query("SELECT * FROM merge_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<MergeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MergeRecord): Long

    @Delete
    suspend fun deleteRecord(record: MergeRecord)

    @Query("DELETE FROM merge_records")
    suspend fun clearAll()
}
