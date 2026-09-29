package com.example.gemini.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks explicit user feedback to calibrate response strategies and measure accuracy.
 */
@Entity(tableName = "feedback_signals")
data class FeedbackSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val userQuery: String,
    val aiAnswerSnippet: String,
    val feedbackType: String, // "THUMBS_UP", "THUMBS_DOWN", "REPORT_INCORRECT", "REQUEST_BETTER"
    val userCorrection: String? = null,
    val wasValidated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
