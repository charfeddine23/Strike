package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.IndividualDao
import com.example.data.local.StrikeEventDao
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.repository.IndividualRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CascadeAndSwipeDeleteTest {

    private lateinit var database: AppDatabase
    private lateinit var individualDao: IndividualDao
    private lateinit var strikeEventDao: StrikeEventDao
    private lateinit var repository: IndividualRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        individualDao = database.individualDao()
        strikeEventDao = database.strikeEventDao()
        repository = IndividualRepository(individualDao, strikeEventDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCascadeDeletionRemovesAllAssociatedStrikes() = runBlocking {
        // 1. Create and insert a Person
        val person = Individual(
            id = 1L,
            fullName = "John Test",
            type = "M",
            length = 20.0,
            width = 10.0,
            size = 200.0,
            sScore = 80.0,
            fScore = 80.0,
            cScore = 80.0,
            boost = 242.0,
            strikeCount = 15
        )
        individualDao.insert(person)

        // 2. Insert associated strike events
        val event1 = StrikeEvent(
            id = 101L,
            individualId = 1L,
            individualName = "John Test",
            year = 2026,
            date = "2026-11-02",
            time = "12:00:00",
            count = 10,
            strikeType = "Auto",
            performanceValue = 242.0
        )
        val event2 = StrikeEvent(
            id = 102L,
            individualId = 1L,
            individualName = "John Test",
            year = 2025,
            date = "2025-05-01",
            time = "10:30:00",
            count = 5,
            strikeType = "Historical",
            performanceValue = 242.0
        )
        strikeEventDao.insertEvent(event1)
        strikeEventDao.insertEvent(event2)

        // Verify inserted
        val beforeIndividuals = individualDao.getAllIndividuals().first()
        val beforeEvents = strikeEventDao.getAllStrikeEvents().first()
        assertEquals(1, beforeIndividuals.size)
        assertEquals(2, beforeEvents.size)

        // 3. Delete person using repository (which calls deleteWithCascade)
        repository.delete(person)

        // 4. Verify BOTH the individual and all associated strike events are completely removed
        val afterIndividuals = individualDao.getAllIndividuals().first()
        val afterEvents = strikeEventDao.getAllStrikeEvents().first()
        assertTrue(afterIndividuals.isEmpty())
        assertTrue(afterEvents.isEmpty())
    }

    @Test
    fun testDeleteStrikeEventDecrementsCountAndCanBeRestored() = runBlocking {
        // 1. Create and insert person with 20 strikes
        val person = Individual(
            id = 2L,
            fullName = "Jane Striker",
            type = "TS",
            length = 25.0,
            width = 12.0,
            size = 300.0,
            sScore = 90.0,
            fScore = 85.0,
            cScore = 90.0,
            boost = 268.0,
            strikeCount = 20
        )
        individualDao.insert(person)

        val strikeEntry = StrikeEvent(
            id = 201L,
            individualId = 2L,
            individualName = "Jane Striker",
            year = 2026,
            date = "2026-11-02",
            time = "14:15:00",
            count = 5,
            strikeType = "Quick +5",
            performanceValue = 268.0
        )
        strikeEventDao.insertEvent(strikeEntry)

        // 2. Perform swipe-to-delete on the strike event
        repository.deleteStrikeEvent(strikeEntry)

        // Verify strike event is removed
        assertNull(strikeEventDao.getEventById(201L))
        // Verify strike count decremented from 20 to 15
        val updatedPerson = individualDao.getIndividualById(2L)
        assertNotNull(updatedPerson)
        assertEquals(15, updatedPerson!!.strikeCount)

        // 3. Perform Undo: restore the deleted strike event
        repository.restoreStrikeEvent(strikeEntry)

        // Verify strike event is back
        val restoredEvent = strikeEventDao.getEventById(201L)
        assertNotNull(restoredEvent)
        assertEquals(201L, restoredEvent!!.id)

        // Verify strike count restored back to 20
        val restoredPerson = individualDao.getIndividualById(2L)
        assertNotNull(restoredPerson)
        assertEquals(20, restoredPerson!!.strikeCount)
    }
}
