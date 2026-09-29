package com.example.gemini.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores non-sensitive, persistent user context across conversations.
 * (e.g. user working on physics problems, preferred calculation format, unit preference)
 */
@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factKey: String,
    val factValue: String,
    val category: String, // "preference", "context", "math_topic"
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val accessCount: Int = 1
)
