package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StrikeEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface StrikeEventDao {
    @Query("SELECT * FROM strike_events ORDER BY timestamp DESC")
    fun getAllStrikeEvents(): Flow<List<StrikeEvent>>

    @Query("SELECT * FROM strike_events WHERE individualId = :id ORDER BY timestamp DESC")
    fun getEventsForIndividual(id: Long): Flow<List<StrikeEvent>>

    @Query("SELECT * FROM strike_events WHERE year = :year ORDER BY timestamp DESC")
    fun getEventsForYear(year: Int): Flow<List<StrikeEvent>>

    @Query("SELECT * FROM strike_events WHERE id = :id")
    suspend fun getEventById(id: Long): StrikeEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: StrikeEvent): Long

    @Delete
    suspend fun deleteEvent(event: StrikeEvent)

    @Query("DELETE FROM strike_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM strike_events WHERE individualId = :id")
    suspend fun deleteEventsForIndividual(id: Long)

    @Query("SELECT COUNT(*) FROM strike_events")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<StrikeEvent>)

    @Query("DELETE FROM strike_events")
    suspend fun clearAll()
}
