package com.spaceexplorer.presentation.viewmodel

import android.content.Context
import com.spaceexplorer.R
import com.spaceexplorer.domain.usecase.GetFavoritesUseCase
import com.spaceexplorer.domain.usecase.RemoveFavoriteUseCase
import com.spaceexplorer.fake.FakeFavoriteRepository
import com.spaceexplorer.fake.testApod
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeFavoriteRepository
    private lateinit var viewModel: FavoritesViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk<Context>(relaxed = true).also {
            every { it.getString(R.string.error_favorites_load) } returns "Favoriten konnten nicht geladen werden"
            every { it.getString(R.string.error_favorite_remove) } returns "Favorit konnte nicht entfernt werden"
        }
        repository = FakeFavoriteRepository()
        viewModel = FavoritesViewModel(
            getFavoritesUseCase = GetFavoritesUseCase(repository),
            removeFavoriteUseCase = RemoveFavoriteUseCase(repository),
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- favorites StateFlow ---

    @Test
    fun `initial favorites is empty list`() = runTest {
        advanceUntilIdle()

        assertTrue(viewModel.favorites.value.isEmpty())
    }

    @Test
    fun `favorites emits stored apods from repository`() = runTest {
        val apod1 = testApod(date = "2024-01-01")
        val apod2 = testApod(date = "2024-01-02")
        repository.storedFavorites.addAll(listOf(apod1, apod2))

        // Re-create ViewModel so it picks up the pre-populated repository
        viewModel = FavoritesViewModel(
            getFavoritesUseCase = GetFavoritesUseCase(repository),
            removeFavoriteUseCase = RemoveFavoriteUseCase(repository),
            context = context
        )
        advanceUntilIdle()

        val favorites = viewModel.favorites.value
        assertEquals(2, favorites.size)
        assertTrue(favorites.contains(apod1))
        assertTrue(favorites.contains(apod2))
    }

    @Test
    fun `favorites emits empty list and snackbar when flow throws`() = runTest {
        repository.flowException = RuntimeException("DB error")

        viewModel = FavoritesViewModel(
            getFavoritesUseCase = GetFavoritesUseCase(repository),
            removeFavoriteUseCase = RemoveFavoriteUseCase(repository),
            context = context
        )

        val events = mutableListOf<UiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        advanceUntilIdle()

        assertTrue(viewModel.favorites.value.isEmpty())
        assertTrue(events.any {
            it is UiEvent.ShowSnackbar && it.message == "Favoriten konnten nicht geladen werden"
        })
        job.cancel()
    }

    // --- removeFavorite ---

    @Test
    fun `removeFavorite deletes apod from repository`() = runTest {
        val apod = testApod()
        repository.storedFavorites.add(apod)

        viewModel.removeFavorite(apod)
        advanceUntilIdle()

        assertEquals(1, repository.removeFavoriteCallCount)
        assertEquals(apod.date, repository.lastRemovedDate)
    }

    @Test
    fun `removeFavorite passes correct date to repository`() = runTest {
        val apod = testApod(date = "2024-06-01")
        repository.storedFavorites.add(apod)

        viewModel.removeFavorite(apod)
        advanceUntilIdle()

        assertEquals("2024-06-01", repository.lastRemovedDate)
    }

    @Test
    fun `removeFavorite does not emit snackbar on success`() = runTest {
        val apod = testApod()
        repository.storedFavorites.add(apod)

        val events = mutableListOf<UiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.removeFavorite(apod)
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test
    fun `removeFavorite emits snackbar on failure`() = runTest {
        // Simulate RemoveFavoriteUseCase failure by making removeFavorite throw
        val failingRepository = object : FakeFavoriteRepository() {
            override suspend fun removeFavorite(date: String) {
                throw RuntimeException("DB locked")
            }
        }
        viewModel = FavoritesViewModel(
            getFavoritesUseCase = GetFavoritesUseCase(failingRepository),
            removeFavoriteUseCase = RemoveFavoriteUseCase(failingRepository),
            context = context
        )

        val events = mutableListOf<UiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.removeFavorite(testApod())
        advanceUntilIdle()

        assertTrue(events.any {
            it is UiEvent.ShowSnackbar && it.message == "Favorit konnte nicht entfernt werden"
        })
        job.cancel()
    }

    @Test
    fun `removeFavorite does not affect other stored favorites`() = runTest {
        val apod1 = testApod(date = "2024-01-01")
        val apod2 = testApod(date = "2024-01-02")
        repository.storedFavorites.addAll(listOf(apod1, apod2))

        viewModel.removeFavorite(apod1)
        advanceUntilIdle()

        assertFalse(repository.storedFavorites.contains(apod1))
        assertTrue(repository.storedFavorites.contains(apod2))
    }
}
