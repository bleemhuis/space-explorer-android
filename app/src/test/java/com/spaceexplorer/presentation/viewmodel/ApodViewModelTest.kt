package com.spaceexplorer.presentation.viewmodel

import android.content.Context
import com.spaceexplorer.R
import com.spaceexplorer.domain.usecase.GetApodUseCase
import com.spaceexplorer.domain.usecase.ObserveIsFavoriteUseCase
import com.spaceexplorer.domain.usecase.ToggleFavoriteUseCase
import com.spaceexplorer.fake.FakeApodRepository
import com.spaceexplorer.fake.FakeFavoriteRepository
import com.spaceexplorer.fake.testApod
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApodViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apodRepository: FakeApodRepository
    private lateinit var favoriteRepository: FakeFavoriteRepository
    private lateinit var viewModel: ApodViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk<Context>(relaxed = true).also {
            every { it.getString(R.string.error_unknown) } returns "Unbekannter Fehler"
            every { it.getString(R.string.error_loading) } returns "Fehler beim Laden"
            every { it.getString(R.string.error_favorite_save) } returns "Favorit konnte nicht gespeichert werden"
        }
        apodRepository = FakeApodRepository()
        favoriteRepository = FakeFavoriteRepository()
        viewModel = ApodViewModel(
            getApodUseCase = GetApodUseCase(apodRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(favoriteRepository),
            observeIsFavoriteUseCase = ObserveIsFavoriteUseCase(favoriteRepository),
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial state ---

    @Test
    fun `initial uiState is Loading`() {
        assertTrue(viewModel.uiState.value is ApodUiState.Loading)
    }

    @Test
    fun `initial isFavorite is false`() = runTest {
        assertFalse(viewModel.isFavorite.value)
    }

    // --- loadApod success ---

    @Test
    fun `loadApod sets Success state on success`() = runTest {
        val apod = testApod(title = "Orion Nebula")
        apodRepository.apodResult = Result.success(apod)

        viewModel.loadApod()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ApodUiState.Success
        assertEquals(apod, state.apod)
    }

    @Test
    fun `loadApod with date forwards date to repository`() = runTest {
        viewModel.loadApod("2024-03-15")
        advanceUntilIdle()

        assertEquals("2024-03-15", apodRepository.lastGetApodDate)
    }

    @Test
    fun `loadApod without date passes null to repository`() = runTest {
        viewModel.loadApod()
        advanceUntilIdle()

        assertTrue(apodRepository.getApodCalled)
        assertEquals(null, apodRepository.lastGetApodDate)
    }

    // --- loadApod failure ---

    @Test
    fun `loadApod sets Error state on failure`() = runTest {
        apodRepository.apodResult = Result.failure(RuntimeException("Network error"))

        viewModel.loadApod()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ApodUiState.Error
        assertEquals("Network error", state.message)
    }

    @Test
    fun `loadApod emits ShowSnackbar on failure`() = runTest {
        apodRepository.apodResult = Result.failure(RuntimeException("Server error"))

        val events = mutableListOf<UiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.loadApod()
        advanceUntilIdle()

        assertTrue(events.any { it is UiEvent.ShowSnackbar && it.message == "Server error" })
        job.cancel()
    }

    @Test
    fun `loadApod uses fallback message when exception has no message`() = runTest {
        apodRepository.apodResult = Result.failure(RuntimeException())

        viewModel.loadApod()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ApodUiState.Error
        assertEquals("Unbekannter Fehler", state.message)
    }

    @Test
    fun `loadApod sets Loading state before result arrives`() = runTest {
        apodRepository.apodResult = Result.success(testApod())

        viewModel.loadApod()

        // Before advanceUntilIdle: coroutine is suspended, state should still be Loading
        assertTrue(viewModel.uiState.value is ApodUiState.Loading)
    }

    // --- toggleFavorite ---

    @Test
    fun `toggleFavorite adds to favorites when not yet favorited`() = runTest {
        val apod = testApod()
        apodRepository.apodResult = Result.success(apod)
        viewModel.loadApod()
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(1, favoriteRepository.addFavoriteCallCount)
        assertTrue(favoriteRepository.storedFavorites.contains(apod))
    }

    @Test
    fun `toggleFavorite removes from favorites when already favorited`() = runTest {
        val apod = testApod()
        favoriteRepository.storedFavorites.add(apod)
        apodRepository.apodResult = Result.success(apod)
        viewModel.loadApod()
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(1, favoriteRepository.removeFavoriteCallCount)
        assertFalse(favoriteRepository.storedFavorites.contains(apod))
    }

    @Test
    fun `toggleFavorite is no-op when state is not Success`() = runTest {
        // State is still Loading (no loadApod called)
        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(0, favoriteRepository.addFavoriteCallCount)
        assertEquals(0, favoriteRepository.removeFavoriteCallCount)
    }

    @Test
    fun `isFavorite reflects favoriteRepository after loadApod`() = runTest {
        val apod = testApod()
        favoriteRepository.storedFavorites.add(apod)
        apodRepository.apodResult = Result.success(apod)

        viewModel.loadApod()
        advanceUntilIdle()

        assertTrue(viewModel.isFavorite.value)
    }

    @Test
    fun `isFavorite is false when apod is not in favorites`() = runTest {
        apodRepository.apodResult = Result.success(testApod())

        viewModel.loadApod()
        advanceUntilIdle()

        assertFalse(viewModel.isFavorite.value)
    }
}
