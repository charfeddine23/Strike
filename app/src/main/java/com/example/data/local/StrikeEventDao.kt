package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StrikeEvent
import kotlinx.coroutines.flow.Flow

data class YearlyPersonAggregate(
    val year: Int,
    val individualName: String,
    val totalStrikes: Int,
    val eventCount: Int
)

data class IndividualLatestDateTuple(
    val individualId: Long,
    val latestDate: String?
)

@Dao
interface StrikeEventDao {
    @Query("SELECT * FROM strike_events ORDER BY timestamp DESC, id DESC LIMIT 10000")
    fun getAllStrikeEvents(): Flow<List<StrikeEvent>>

    @Query("SELECT COALESCE(SUM(count), 0) FROM strike_events")
    fun getAllTimeStrikesCountFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM strike_events")
    suspend fun getAllTimeStrikesCount(): Int

    @Query(
        """
        SELECT year, individualName,
               COALESCE(SUM(count), 0) AS totalStrikes,
               COUNT(*) AS eventCount
        FROM strike_events
        GROUP BY year, individualName
        ORDER BY year DESC, totalStrikes DESC
        """
    )
    fun getYearlyPersonAggregatesFlow(): Flow<List<YearlyPersonAggregate>>

    @Query(
        """
        SELECT year, individualName,
               COALESCE(SUM(count), 0) AS totalStrikes,
               COUNT(*) AS eventCount
        FROM strike_events
        GROUP BY year, individualName
        ORDER BY year DESC, totalStrikes DESC
        """
    )
    suspend fun getYearlyPersonAggregates(): List<YearlyPersonAggregate>

    @Query("SELECT individualId, MAX(date) AS latestDate FROM strike_events GROUP BY individualId")
    fun getLatestStrikeDateByIndividualFlow(): Flow<List<IndividualLatestDateTuple>>

    @Query("SELECT * FROM strike_events WHERE individualId = :id ORDER BY timestamp DESC, id DESC")
    fun getEventsForIndividual(id: Long): Flow<List<StrikeEvent>>

    @Query("SELECT * FROM strike_events WHERE year = :year ORDER BY timestamp DESC, id DESC")
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

    @Query("SELECT * FROM strike_events ORDER BY timestamp DESC, id DESC")
    suspend fun getAllStrikeEventsList(): List<StrikeEvent>

    @Query("SELECT MAX(date) FROM strike_events")
    suspend fun getLatestStrikeDate(): String?

    @Query("SELECT * FROM strike_events WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getEventsForDate(date: String): List<StrikeEvent>

    @Query("UPDATE strike_events SET notes = :notes WHERE id = :id")
    suspend fun updateEventNotes(id: Long, notes: String)

    @Query("DELETE FROM strike_events")
    suspend fun clearAll()
}
