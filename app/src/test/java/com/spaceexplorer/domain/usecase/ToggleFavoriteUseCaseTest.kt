package com.spaceexplorer.domain.usecase

import com.spaceexplorer.fake.FakeFavoriteRepository
import com.spaceexplorer.fake.testApod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var repository: FakeFavoriteRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    @Before
    fun setUp() {
        repository = FakeFavoriteRepository()
        useCase = ToggleFavoriteUseCase(repository)
    }

    // --- Add branch ---

    @Test
    fun `invoke adds favorite when apod is not yet favorited`() = runTest {
        val apod = testApod()

        useCase(apod)

        assertEquals(1, repository.addFavoriteCallCount)
        assertEquals(0, repository.removeFavoriteCallCount)
    }

    @Test
    fun `invoke stores apod in repository when adding`() = runTest {
        val apod = testApod()

        useCase(apod)

        assertTrue(repository.storedFavorites.contains(apod))
    }

    @Test
    fun `invoke adds correct apod object to repository`() = runTest {
        val apod = testApod(date = "2024-07-04", title = "Eagle Nebula")

        useCase(apod)

        assertEquals(apod, repository.storedFavorites.first())
    }

    // --- Remove branch ---

    @Test
    fun `invoke removes favorite when apod is already favorited`() = runTest {
        val apod = testApod()
        repository.storedFavorites.add(apod)

        useCase(apod)

        assertEquals(0, repository.addFavoriteCallCount)
        assertEquals(1, repository.removeFavoriteCallCount)
    }

    @Test
    fun `invoke removes apod from repository when already favorited`() = runTest {
        val apod = testApod()
        repository.storedFavorites.add(apod)

        useCase(apod)

        assertFalse(repository.storedFavorites.contains(apod))
    }

    @Test
    fun `invoke passes correct date when removing favorite`() = runTest {
        val apod = testApod(date = "2024-07-04")
        repository.storedFavorites.add(apod)

        useCase(apod)

        assertEquals("2024-07-04", repository.lastRemovedDate)
    }

    // --- Toggle behavior ---

    @Test
    fun `invoke toggles from not-favorited to favorited`() = runTest {
        val apod = testApod()
        assertFalse(repository.storedFavorites.any { it.date == apod.date })

        useCase(apod)

        assertTrue(repository.storedFavorites.any { it.date == apod.date })
    }

    @Test
    fun `invoke toggles from favorited to not-favorited`() = runTest {
        val apod = testApod()
        repository.storedFavorites.add(apod)
        assertTrue(repository.storedFavorites.any { it.date == apod.date })

        useCase(apod)

        assertFalse(repository.storedFavorites.any { it.date == apod.date })
    }

    @Test
    fun `invoke called twice results in original state`() = runTest {
        val apod = testApod()

        useCase(apod) // add
        useCase(apod) // remove

        assertFalse(repository.storedFavorites.contains(apod))
        assertEquals(1, repository.addFavoriteCallCount)
        assertEquals(1, repository.removeFavoriteCallCount)
    }

    @Test
    fun `invoke only affects apod with matching date`() = runTest {
        val apod1 = testApod(date = "2024-01-01")
        val apod2 = testApod(date = "2024-01-02")
        repository.storedFavorites.add(apod1)
        repository.storedFavorites.add(apod2)

        useCase(apod1)

        assertFalse(repository.storedFavorites.contains(apod1))
        assertTrue(repository.storedFavorites.contains(apod2))
    }
}
