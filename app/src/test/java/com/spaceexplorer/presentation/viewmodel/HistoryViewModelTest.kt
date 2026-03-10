package com.spaceexplorer.presentation.viewmodel

import android.content.Context
import com.spaceexplorer.R
import com.spaceexplorer.domain.usecase.GetApodRangeUseCase
import com.spaceexplorer.fake.FakeApodRepository
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
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeApodRepository
    private lateinit var useCase: GetApodRangeUseCase
    private lateinit var viewModel: HistoryViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk<Context>(relaxed = true).also {
            every { it.getString(R.string.error_unknown) } returns "Unbekannter Fehler"
            every { it.getString(R.string.error_loading) } returns "Fehler beim Laden"
        }
        repository = FakeApodRepository()
        useCase = GetApodRangeUseCase(repository)
        viewModel = HistoryViewModel(useCase, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertTrue(viewModel.uiState.value is HistoryUiState.Idle)
    }

    @Test
    fun `initial showDatePicker is false`() = runTest {
        assertFalse(viewModel.showDatePicker.value)
    }

    @Test
    fun `openDatePicker sets showDatePicker to true`() = runTest {
        viewModel.openDatePicker()

        assertTrue(viewModel.showDatePicker.value)
    }

    @Test
    fun `dismissDatePicker sets showDatePicker to false`() = runTest {
        viewModel.openDatePicker()
        viewModel.dismissDatePicker()

        assertFalse(viewModel.showDatePicker.value)
    }

    @Test
    fun `loadRange results in Success state with returned items`() = runTest {
        val apods = listOf(testApod(date = "2024-01-03"), testApod(date = "2024-01-01"))
        repository.apodRangeResult = Result.success(apods)

        viewModel.loadRange("2024-01-01", "2024-01-03")
        advanceUntilIdle()

        val successState = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(apods, successState.items)
    }

    @Test
    fun `loadRange transitions through Loading to Error on failure`() = runTest {
        repository.apodRangeResult = Result.failure(RuntimeException("Network error"))

        viewModel.loadRange("2024-01-01", "2024-01-07")
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as HistoryUiState.Error
        assertEquals("Network error", errorState.message)
    }

    @Test
    fun `loadRange dismisses date picker immediately`() = runTest {
        viewModel.openDatePicker()
        viewModel.loadRange("2024-01-01", "2024-01-07")

        assertFalse(viewModel.showDatePicker.value)
    }

    @Test
    fun `loadRange emits ShowSnackbar event on failure`() = runTest {
        repository.apodRangeResult = Result.failure(RuntimeException("Server error"))

        val events = mutableListOf<UiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.loadRange("2024-01-01", "2024-01-07")
        advanceUntilIdle()

        assertTrue(events.any { it is UiEvent.ShowSnackbar && it.message == "Server error" })
        job.cancel()
    }

    @Test
    fun `loadRange with empty result shows Success with empty list`() = runTest {
        repository.apodRangeResult = Result.success(emptyList())

        viewModel.loadRange("2024-01-01", "2024-01-07")
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Success
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `loadRange forwards dates to repository`() = runTest {
        viewModel.loadRange("2024-03-01", "2024-03-31")
        advanceUntilIdle()

        assertEquals("2024-03-01", repository.lastRangeStart)
        assertEquals("2024-03-31", repository.lastRangeEnd)
    }

    @Test
    fun `error state uses fallback message when exception has no message`() = runTest {
        repository.apodRangeResult = Result.failure(RuntimeException())

        viewModel.loadRange("2024-01-01", "2024-01-07")
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as HistoryUiState.Error
        assertEquals("Unbekannter Fehler", errorState.message)
    }
}
