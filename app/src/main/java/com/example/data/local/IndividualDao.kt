package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Individual
import kotlinx.coroutines.flow.Flow

@Dao
interface IndividualDao {
    @Query("SELECT * FROM individuals ORDER BY boost DESC, strikeCount DESC, id DESC")
    fun getAllIndividuals(): Flow<List<Individual>>

    @Query("SELECT * FROM individuals WHERE id = :id")
    suspend fun getIndividualById(id: Long): Individual?

    @Query("SELECT * FROM individuals WHERE id = :id")
    fun getIndividualFlow(id: Long): Flow<Individual?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(individual: Individual): Long

    @Update
    suspend fun update(individual: Individual)

    @Delete
    suspend fun delete(individual: Individual)

    @Query("DELETE FROM strike_events WHERE individualId = :individualId")
    suspend fun deleteStrikesForIndividual(individualId: Long)

    @Transaction
    suspend fun deleteWithCascade(individual: Individual) {
        deleteStrikesForIndividual(individual.id)
        delete(individual)
    }

    @Query("UPDATE individuals SET strikeCount = CASE WHEN (strikeCount + :delta) < 0 THEN 0 ELSE (strikeCount + :delta) END, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementStrike(id: Long, delta: Int = 1, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE individuals SET strikeCount = :count, updatedAt = :timestamp WHERE id = :id")
    suspend fun setStrikeCount(id: Long, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM individuals")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(individuals: List<Individual>)

    @Query("SELECT * FROM individuals")
    suspend fun getAllIndividualsList(): List<Individual>

    @Query("SELECT * FROM individuals WHERE phoneNumber IS NOT NULL AND phoneNumber != ''")
    suspend fun getIndividualsWithPhone(): List<Individual>

    @Query("DELETE FROM individuals")
    suspend fun clearAll()

    @Query("UPDATE individuals SET strikeCount = 0, updatedAt = :timestamp")
    suspend fun resetAllStrikeCounts(timestamp: Long = System.currentTimeMillis())
}
