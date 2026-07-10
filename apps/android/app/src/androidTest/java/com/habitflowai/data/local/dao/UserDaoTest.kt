package com.habitflowai.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.habitflowai.data.local.HabitFlowDatabase
import com.habitflowai.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: HabitFlowDatabase
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HabitFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.userDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun createUser(
        id: String = "user-1",
        email: String = "test@example.com",
        personaType: String? = "Achiever"
    ) = UserEntity(
        id = id,
        email = email,
        goal = "Run a marathon",
        personaType = personaType,
        portfolioSummary = "Summary",
        tips = "tip1,tip2",
        failurePatterns = "pat1",
        confidenceScore = 0.85
    )

    @Test
    fun insertAndGetUserById() = runBlocking {
        val user = createUser()
        dao.insert(user)

        val retrieved = dao.getUserById(user.id)
        assertNotNull(retrieved)
        assertEquals(user.id, retrieved?.id)
        assertEquals(user.email, retrieved?.email)
        assertEquals("Achiever", retrieved?.personaType)
    }

    @Test
    fun insertReplacesOnConflict() = runBlocking {
        val user = createUser(personaType = "Original")
        dao.insert(user)

        val updated = user.copy(personaType = "Grower")
        dao.insert(updated)

        val retrieved = dao.getUserById(user.id)
        assertEquals("Grower", retrieved?.personaType)
    }

    @Test
    fun getUserByEmail_returnsFlow() = runBlocking {
        val user = createUser()
        dao.insert(user)

        val flowValue = dao.getUserByEmail("test@example.com").first()
        assertNotNull(flowValue)
        assertEquals("user-1", flowValue?.id)
    }

    @Test
    fun getUserByEmail_returnsNullForUnknown() = runBlocking {
        val result = dao.getUserByEmail("unknown@example.com").first()
        assertNull(result)
    }

    @Test
    fun getFirstUser_returnsOnlyUser() = runBlocking {
        val user = createUser()
        dao.insert(user)

        val first = dao.getFirstUser()
        assertNotNull(first)
        assertEquals("user-1", first?.id)
    }

    @Test
    fun getFirstUser_returnsNullWhenEmpty() = runBlocking {
        val first = dao.getFirstUser()
        assertNull(first)
    }

    @Test
    fun getFirstUser_returnsFirstWhenMultiple() = runBlocking {
        val u1 = createUser(id = "u1", email = "a@example.com")
        val u2 = createUser(id = "u2", email = "b@example.com")
        dao.insert(u1)
        dao.insert(u2)

        // With REPLACE conflict, the order is not guaranteed,
        // but there should always be one returned when rows exist
        val first = dao.getFirstUser()
        assertNotNull(first)
    }

    @Test
    fun storesAndRetrievesAllFields() = runBlocking {
        val user = UserEntity(
            id = "detailed",
            email = "detailed@example.com",
            goal = "Learn piano",
            personaType = "Explorer",
            portfolioSummary = "Portfolio summary text",
            tips = "tip-a,tip-b,tip-c",
            failurePatterns = "fp1,fp2",
            confidenceScore = 0.92
        )
        dao.insert(user)

        val retrieved = dao.getUserById("detailed")!!
        assertEquals("Learn piano", retrieved.goal)
        assertEquals("Explorer", retrieved.personaType)
        assertEquals("Portfolio summary text", retrieved.portfolioSummary)
        assertEquals("tip-a,tip-b,tip-c", retrieved.tips)
        assertEquals("fp1,fp2", retrieved.failurePatterns)
        assertEquals(0.92, retrieved.confidenceScore, 0.001)
    }
}
