package com.habitflowai.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.habitflowai.data.local.HabitFlowDatabase
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: HabitFlowDatabase
    private lateinit var dao: HabitDao
    private val userId = "test-user"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HabitFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.habitDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun createHabit(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Habit",
        syncStatus: SyncStatus = SyncStatus.SYNCED,
        userId: String = this.userId
    ) = HabitEntity(
        id = id,
        title = title,
        description = "Description",
        frequency = "DAILY",
        userId = userId,
        completed = false,
        syncStatus = syncStatus
    )

    @Test
    fun insertAndGetHabitById() = runBlocking {
        val habit = createHabit()
        dao.insert(habit)

        val retrieved = dao.getHabitById(habit.id)
        assertNotNull(retrieved)
        assertEquals(habit.id, retrieved?.id)
        assertEquals(habit.title, retrieved?.title)
    }

    @Test
    fun getHabitsByUserId_returnsFlow() = runBlocking {
        val h1 = createHabit(id = "h1", title = "Habit A")
        val h2 = createHabit(id = "h2", title = "Habit B")
        dao.insert(h1)
        dao.insert(h2)

        val habits = dao.getHabitsByUserId(userId).first()
        assertEquals(2, habits.size)
        assertTrue(habits.any { it.id == "h1" })
        assertTrue(habits.any { it.id == "h2" })
    }

    @Test
    fun getHabitsByUserId_filtersByUser() = runBlocking {
        val h1 = createHabit(id = "h1", userId = "user-a")
        val h2 = createHabit(id = "h2", userId = "user-b")
        dao.insert(h1)
        dao.insert(h2)

        val userAHabits = dao.getHabitsByUserId("user-a").first()
        assertEquals(1, userAHabits.size)
        assertEquals("h1", userAHabits.first().id)
    }

    @Test
    fun upsertAll_replacesExisting() = runBlocking {
        val habit = createHabit(id = "h1", title = "Original")
        dao.insert(habit)

        val updated = habit.copy(title = "Updated")
        dao.upsertAll(listOf(updated))

        val retrieved = dao.getHabitById("h1")
        assertEquals("Updated", retrieved?.title)
    }

    @Test
    fun delete_removesHabit() = runBlocking {
        val habit = createHabit()
        dao.insert(habit)
        dao.delete(habit)

        val retrieved = dao.getHabitById(habit.id)
        assertNull(retrieved)
    }

    @Test
    fun update_modifiesFields() = runBlocking {
        val habit = createHabit()
        dao.insert(habit)

        val updated = habit.copy(title = "Modified", completed = true)
        dao.update(updated)

        val retrieved = dao.getHabitById(habit.id)
        assertEquals("Modified", retrieved?.title)
        assertEquals(true, retrieved?.completed)
    }

    @Test
    fun getUnsyncedHabits_excludesSynced() = runBlocking {
        val synced = createHabit(id = "s1", syncStatus = SyncStatus.SYNCED)
        val pending = createHabit(id = "p1", syncStatus = SyncStatus.PENDING_CREATE)
        dao.insert(synced)
        dao.insert(pending)

        val unsynced = dao.getUnsyncedHabits(userId)
        assertEquals(1, unsynced.size)
        assertEquals("p1", unsynced.first().id)
    }

    @Test
    fun getUnsyncedHabits_excludesOtherUsersPendingHabits() = runBlocking {
        val ownPending = createHabit(id = "p1", syncStatus = SyncStatus.PENDING_CREATE)
        val otherUsersPending = createHabit(id = "p2", syncStatus = SyncStatus.PENDING_CREATE, userId = "other-user")
        dao.insert(ownPending)
        dao.insert(otherUsersPending)

        val unsynced = dao.getUnsyncedHabits(userId)
        assertEquals(1, unsynced.size)
        assertEquals("p1", unsynced.first().id)
    }

    @Test
    fun markSynced_updatesStatus() = runBlocking {
        val habit = createHabit(syncStatus = SyncStatus.PENDING_CREATE)
        dao.insert(habit)

        dao.markSynced(habit.id)

        val retrieved = dao.getHabitById(habit.id)
        assertEquals(SyncStatus.SYNCED, retrieved?.syncStatus)
    }

    @Test
    fun deleteBySyncStatus_removesOnlyMatching() = runBlocking {
        val toDelete = createHabit(id = "d1", syncStatus = SyncStatus.PENDING_DELETE)
        val keep = createHabit(id = "k1", syncStatus = SyncStatus.SYNCED)
        dao.insert(toDelete)
        dao.insert(keep)

        dao.deleteBySyncStatus(userId, SyncStatus.PENDING_DELETE)

        val all = dao.getHabitsByUserId(userId).first()
        assertEquals(1, all.size)
        assertEquals("k1", all.first().id)
    }

    @Test
    fun deleteBySyncStatus_leavesOtherUsersMatchingHabitsAlone() = runBlocking {
        val ownPendingDelete = createHabit(id = "d1", syncStatus = SyncStatus.PENDING_DELETE)
        val othersPendingDelete = createHabit(id = "d2", syncStatus = SyncStatus.PENDING_DELETE, userId = "other-user")
        dao.insert(ownPendingDelete)
        dao.insert(othersPendingDelete)

        dao.deleteBySyncStatus(userId, SyncStatus.PENDING_DELETE)

        assertNull(dao.getHabitById("d1"))
        assertNotNull(dao.getHabitById("d2"))
    }

    @Test
    fun updateSyncStatus_changesStatusAndTimestamp() = runBlocking {
        val habit = createHabit()
        dao.insert(habit)

        val newTime = System.currentTimeMillis() + 1000
        dao.updateSyncStatus(habit.id, SyncStatus.PENDING_UPDATE, newTime)

        val retrieved = dao.getHabitById(habit.id)
        assertEquals(SyncStatus.PENDING_UPDATE, retrieved?.syncStatus)
        assertEquals(newTime, retrieved?.updatedAt)
    }

    @Test
    fun getAllForUser_returnsOnlyThatUsersHabits() = runBlocking {
        val h1 = createHabit(id = "h1")
        val h2 = createHabit(id = "h2")
        val otherUsersHabit = createHabit(id = "h3", userId = "other-user")
        dao.insert(h1)
        dao.insert(h2)
        dao.insert(otherUsersHabit)

        val all = dao.getAllForUser(userId)
        assertEquals(2, all.size)
    }
}
