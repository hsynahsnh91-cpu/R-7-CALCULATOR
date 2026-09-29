package com.example.gemini.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfImprovementDao {

    // --- Memory Facts ---
    @Query("SELECT * FROM memory_facts ORDER BY accessCount DESC, timestamp DESC LIMIT 20")
    fun getAllFactsFlow(): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts ORDER BY accessCount DESC, timestamp DESC LIMIT 20")
    suspend fun getAllFacts(): List<MemoryFactEntity>

    @Query("SELECT * FROM memory_facts WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getFactsByCategory(category: String): List<MemoryFactEntity>

    @Query("SELECT * FROM memory_facts WHERE factKey = :key LIMIT 1")
    suspend fun getFactByKey(key: String): MemoryFactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: MemoryFactEntity): Long

    @Update
    suspend fun updateFact(fact: MemoryFactEntity)

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteFact(id: Long)

    @Query("DELETE FROM memory_facts")
    suspend fun clearAllFacts()

    // --- Learned Patterns ---
    @Query("SELECT * FROM learned_patterns ORDER BY (successCount + failureCount) DESC")
    fun getAllPatternsFlow(): Flow<List<LearnedPatternEntity>>

    @Query("SELECT * FROM learned_patterns ORDER BY (successCount + failureCount) DESC")
    suspend fun getAllPatterns(): List<LearnedPatternEntity>

    @Query("SELECT * FROM learned_patterns WHERE patternKey = :key LIMIT 1")
    suspend fun getPattern(key: String): LearnedPatternEntity?

    @Query("SELECT * FROM learned_patterns WHERE domain = :domain")
    suspend fun getPatternsByDomain(domain: String): List<LearnedPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePattern(pattern: LearnedPatternEntity)

    @Query("DELETE FROM learned_patterns")
    suspend fun clearAllPatterns()

    // --- Feedback Signals ---
    @Query("SELECT * FROM feedback_signals ORDER BY timestamp DESC LIMIT 50")
    fun getAllFeedbackFlow(): Flow<List<FeedbackSignalEntity>>

    @Query("SELECT * FROM feedback_signals ORDER BY timestamp DESC LIMIT 50")
    suspend fun getAllFeedback(): List<FeedbackSignalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(signal: FeedbackSignalEntity): Long

    @Query("SELECT COUNT(*) FROM feedback_signals WHERE feedbackType = 'THUMBS_UP'")
    suspend fun getPositiveFeedbackCount(): Int

    @Query("SELECT COUNT(*) FROM feedback_signals WHERE feedbackType IN ('THUMBS_DOWN', 'REPORT_INCORRECT')")
    suspend fun getNegativeFeedbackCount(): Int

    @Query("SELECT COUNT(*) FROM feedback_signals")
    suspend fun getTotalFeedbackCount(): Int

    @Query("DELETE FROM feedback_signals")
    suspend fun clearAllFeedback()
}
