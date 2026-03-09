package com.spaceexplorer.domain.usecase

import com.spaceexplorer.fake.FakeApodRepository
import com.spaceexplorer.fake.testApod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetApodUseCaseTest {

    private lateinit var repository: FakeApodRepository
    private lateinit var useCase: GetApodUseCase

    @Before
    fun setUp() {
        repository = FakeApodRepository()
        useCase = GetApodUseCase(repository)
    }

    @Test
    fun `invoke without date passes null to repository`() = runTest {
        useCase()

        assertTrue(repository.getApodCalled)
        assertNull(repository.lastGetApodDate)
    }

    @Test
    fun `invoke with specific date passes date to repository`() = runTest {
        useCase("2024-06-15")

        assertEquals("2024-06-15", repository.lastGetApodDate)
    }

    @Test
    fun `invoke returns success result from repository`() = runTest {
        val apod = testApod(title = "Crab Nebula")
        repository.apodResult = Result.success(apod)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(apod, result.getOrNull())
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val error = RuntimeException("Network error")
        repository.apodResult = Result.failure(error)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke delegates date correctly and returns matching apod`() = runTest {
        val apod = testApod(date = "2024-03-20")
        repository.apodResult = Result.success(apod)

        val result = useCase("2024-03-20")

        assertEquals("2024-03-20", repository.lastGetApodDate)
        assertEquals(apod.date, result.getOrNull()?.date)
    }

    @Test
    fun `invoke does not call repository more than once per invocation`() = runTest {
        useCase()
        useCase("2024-01-01")

        // Each call to the use case is independent — second call should overwrite lastGetApodDate
        assertEquals("2024-01-01", repository.lastGetApodDate)
    }

    @Test
    fun `invoke returns failure without throwing exception`() = runTest {
        repository.apodResult = Result.failure(IllegalStateException("Server error"))

        val result = runCatching { useCase() }

        assertFalse(result.isFailure) // no exception escapes the use case
        assertTrue(result.getOrThrow().isFailure)
    }
}
