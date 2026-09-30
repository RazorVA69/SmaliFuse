package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merge_records")
data class MergeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val fileName: String,
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val previewText: String,
    val fullText: String
)
