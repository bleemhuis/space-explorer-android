package com.spaceexplorer.domain.usecase

import com.spaceexplorer.fake.FakeApodRepository
import com.spaceexplorer.fake.testApod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetApodRangeUseCaseTest {

    private lateinit var repository: FakeApodRepository
    private lateinit var useCase: GetApodRangeUseCase

    @Before
    fun setUp() {
        repository = FakeApodRepository()
        useCase = GetApodRangeUseCase(repository)
    }

    @Test
    fun `invoke forwards startDate and endDate to repository`() = runTest {
        useCase("2024-01-01", "2024-01-07")

        assertEquals("2024-01-01", repository.lastRangeStart)
        assertEquals("2024-01-07", repository.lastRangeEnd)
    }

    @Test
    fun `invoke returns success list from repository`() = runTest {
        val apods = listOf(testApod(date = "2024-01-03"), testApod(date = "2024-01-01"))
        repository.apodRangeResult = Result.success(apods)

        val result = useCase("2024-01-01", "2024-01-03")

        assertTrue(result.isSuccess)
        assertEquals(apods, result.getOrNull())
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val error = RuntimeException("Network error")
        repository.apodRangeResult = Result.failure(error)

        val result = useCase("2024-01-01", "2024-01-07")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke returns empty list when repository returns empty`() = runTest {
        repository.apodRangeResult = Result.success(emptyList())

        val result = useCase("2024-01-01", "2024-01-07")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `invoke does not throw exception on failure`() = runTest {
        repository.apodRangeResult = Result.failure(IllegalStateException("Server error"))

        val result = runCatching { useCase("2024-01-01", "2024-01-07") }

        assertFalse(result.isFailure)
        assertTrue(result.getOrThrow().isFailure)
    }
}
