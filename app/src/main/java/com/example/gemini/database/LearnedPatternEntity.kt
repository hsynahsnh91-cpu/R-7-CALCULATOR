package com.example.gemini.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores learned error and response patterns so future responses
 * automatically adapt verification strategies and system instructions.
 */
@Entity(tableName = "learned_patterns")
data class LearnedPatternEntity(
    @PrimaryKey val patternKey: String, // e.g. "parallel_resistor", "percentage_addition", "javascript_async"
    val domain: String, // "MATH", "PROGRAMMING", "UNIT_CONV", etc.
    val successCount: Int = 1,
    val failureCount: Int = 0,
    val verificationStrategy: String, // e.g. "VERIFY_WITH_R7_ENGINE", "CHECK_INVERSE_SUM", "CONFIRM_UNITS"
    val adaptivePromptInstruction: String, // Instruction injected into prompt to prevent past mistake
    val reliability: String = "HIGH", // "HIGH", "MEDIUM", "LOW"
    val lastUpdated: Long = System.currentTimeMillis()
)
