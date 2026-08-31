package com.habitflowai.presentation.viewmodel

import com.habitflowai.data.model.AppUser
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.SocialRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FollowingViewModelTest {

    private val socialRepository: SocialRepository = mockk()
    private val authManager: AuthManager = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { authManager.currentUserId } returns MutableStateFlow("me")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads only the users that are followed, joined with their full profiles`() {
        coEvery { socialRepository.getFollowing("me") } returns listOf("user-1", "user-3")
        coEvery { socialRepository.getAllUsers() } returns listOf(
            AppUser(id = "user-1", email = "one@example.com", firstName = "One"),
            AppUser(id = "user-2", email = "two@example.com", firstName = "Two"),
            AppUser(id = "user-3", email = "three@example.com", firstName = "Three")
        )

        val viewModel = FollowingViewModel(socialRepository, authManager)

        val state = viewModel.uiState.value
        assertEquals(listOf("user-1", "user-3"), state.users.map { it.id })
        assertTrue(!state.isLoading)
    }

    @Test
    fun `surfaces an error message when loading fails`() {
        coEvery { socialRepository.getFollowing("me") } throws RuntimeException("network down")

        val viewModel = FollowingViewModel(socialRepository, authManager)

        val state = viewModel.uiState.value
        assertTrue(state.users.isEmpty())
        assertTrue(state.errorMessage != null)
    }
}
