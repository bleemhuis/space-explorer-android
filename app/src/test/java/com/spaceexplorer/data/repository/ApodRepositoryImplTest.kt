package com.spaceexplorer.data.repository

import com.spaceexplorer.data.remote.api.NasaApiService
import com.spaceexplorer.data.remote.dto.ApodDto
import com.spaceexplorer.data.repository.ApodRepositoryImpl.Companion.CACHE_MAX_SIZE
import com.spaceexplorer.fake.FakeApodCacheDao
import com.spaceexplorer.fake.FakeApodDao
import com.spaceexplorer.fake.testApod
import com.spaceexplorer.fake.testCacheEntity
import com.spaceexplorer.fake.testEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApodRepositoryImplTest {

    private val apiService: NasaApiService = mockk()
    private lateinit var dao: FakeApodDao
    private lateinit var cacheDao: FakeApodCacheDao
    private lateinit var repository: ApodRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeApodDao()
        cacheDao = FakeApodCacheDao()
        repository = ApodRepositoryImpl(apiService, dao, cacheDao)
    }

    // --- getApod ---

    @Test
    fun `getApod returns success when api call succeeds`() = runTest {
        coEvery { apiService.getApod(any(), any()) } returns testDto()

        val result = repository.getApod()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `getApod passes null date to api service`() = runTest {
        coEvery { apiService.getApod(null, any()) } returns testDto()

        repository.getApod(null)

        coVerify { apiService.getApod(null, any()) }
    }

    @Test
    fun `getApod passes specific date to api service`() = runTest {
        coEvery { apiService.getApod("2024-06-15", any()) } returns testDto(date = "2024-06-15")

        repository.getApod("2024-06-15")

        coVerify { apiService.getApod("2024-06-15", any()) }
    }

    @Test
    fun `getApod maps dto fields to domain model correctly`() = runTest {
        val dto = testDto(
            date = "2024-03-20",
            title = "Crab Nebula",
            explanation = "Supernova remnant.",
            url = "https://example.com/crab.jpg",
            hdUrl = "https://example.com/crab_hd.jpg",
            mediaType = "image",
            copyright = "NASA",
            thumbnailUrl = null
        )
        coEvery { apiService.getApod(any(), any()) } returns dto

        val apod = repository.getApod().getOrThrow()

        assertEquals("2024-03-20", apod.date)
        assertEquals("Crab Nebula", apod.title)
        assertEquals("Supernova remnant.", apod.explanation)
        assertEquals("https://example.com/crab.jpg", apod.url)
        assertEquals("https://example.com/crab_hd.jpg", apod.hdUrl)
        assertEquals("image", apod.mediaType)
        assertEquals("NASA", apod.copyright)
    }

    @Test
    fun `getApod maps video dto with thumbnailUrl correctly`() = runTest {
        val dto = testDto(
            mediaType = "video",
            url = "https://youtube.com/embed/xyz",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        coEvery { apiService.getApod(any(), any()) } returns dto

        val apod = repository.getApod().getOrThrow()

        assertEquals("video", apod.mediaType)
        assertEquals("https://example.com/thumb.jpg", apod.thumbnailUrl)
        assertTrue(apod.isVideo)
    }

    @Test
    fun `getApod returns failure when api throws`() = runTest {
        coEvery { apiService.getApod(any(), any()) } throws RuntimeException("Network error")

        val result = repository.getApod()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getApod does not throw — wraps exception in Result`() = runTest {
        coEvery { apiService.getApod(any(), any()) } throws IllegalStateException("Server error")

        val result = runCatching { repository.getApod() }

        assertFalse(result.isFailure)
    }

    // --- getFavorites ---

    @Test
    fun `getFavorites returns empty list when dao has no entries`() = runTest {
        val favorites = repository.getFavorites().first()

        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `getFavorites maps entities to domain models`() = runTest {
        dao.seed(
            testEntity(date = "2024-01-01", title = "Galaxy A"),
            testEntity(date = "2024-01-02", title = "Galaxy B")
        )

        val favorites = repository.getFavorites().first()

        assertEquals(2, favorites.size)
        assertEquals("Galaxy A", favorites[0].title)
        assertEquals("Galaxy B", favorites[1].title)
    }

    @Test
    fun `getFavorites maps entity fields correctly`() = runTest {
        dao.seed(testEntity(
            date = "2024-05-10",
            title = "Eagle Nebula",
            explanation = "Star-forming region.",
            url = "https://example.com/eagle.jpg",
            mediaType = "image",
            copyright = "ESA"
        ))

        val apod = repository.getFavorites().first().first()

        assertEquals("2024-05-10", apod.date)
        assertEquals("Eagle Nebula", apod.title)
        assertEquals("Star-forming region.", apod.explanation)
        assertEquals("ESA", apod.copyright)
        assertNull(apod.thumbnailUrl) // not stored in Room
    }

    @Test
    fun `getFavorites emits updated list after insert`() = runTest {
        repository.addFavorite(testApod(date = "2024-06-01"))

        val favorites = repository.getFavorites().first()

        assertEquals(1, favorites.size)
        assertEquals("2024-06-01", favorites.first().date)
    }

    // --- isFavorite ---

    @Test
    fun `isFavorite returns false when apod is not in favorites`() = runTest {
        val result = repository.isFavorite("2024-01-15").first()

        assertFalse(result)
    }

    @Test
    fun `isFavorite returns true when apod is in favorites`() = runTest {
        dao.seed(testEntity(date = "2024-01-15"))

        val result = repository.isFavorite("2024-01-15").first()

        assertTrue(result)
    }

    @Test
    fun `isFavorite returns false for different date`() = runTest {
        dao.seed(testEntity(date = "2024-01-15"))

        val result = repository.isFavorite("2024-01-16").first()

        assertFalse(result)
    }

    // --- addFavorite ---

    @Test
    fun `addFavorite inserts entity into dao`() = runTest {
        repository.addFavorite(testApod())

        assertEquals(1, dao.insertCallCount)
    }

    @Test
    fun `addFavorite maps domain model to entity correctly`() = runTest {
        val apod = testApod(
            date = "2024-08-20",
            title = "Pillars of Creation",
            url = "https://example.com/pillars.jpg",
            hdUrl = "https://example.com/pillars_hd.jpg",
            copyright = "Hubble"
        )

        repository.addFavorite(apod)

        val entity = dao.lastInsertedEntity!!
        assertEquals("2024-08-20", entity.date)
        assertEquals("Pillars of Creation", entity.title)
        assertEquals("https://example.com/pillars.jpg", entity.url)
        assertEquals("https://example.com/pillars_hd.jpg", entity.hdUrl)
        assertEquals("Hubble", entity.copyright)
    }

    // --- removeFavorite ---

    @Test
    fun `removeFavorite deletes entry from dao by date`() = runTest {
        dao.seed(testEntity(date = "2024-01-15"))

        repository.removeFavorite("2024-01-15")

        assertEquals(1, dao.deleteByDateCallCount)
        assertEquals("2024-01-15", dao.lastDeletedDate)
    }

    @Test
    fun `removeFavorite does not affect other entries`() = runTest {
        dao.seed(
            testEntity(date = "2024-01-15"),
            testEntity(date = "2024-01-16")
        )

        repository.removeFavorite("2024-01-15")

        val remaining = repository.getFavorites().first()
        assertEquals(1, remaining.size)
        assertEquals("2024-01-16", remaining.first().date)
    }

    // --- getApod caching ---

    @Test
    fun `getApod on network success inserts result into cache`() = runTest {
        coEvery { apiService.getApod(any(), any()) } returns testDto(date = "2024-06-01")

        repository.getApod("2024-06-01")

        assertEquals(1, cacheDao.insertCallCount)
        assertNotNull(cacheDao.getByDate("2024-06-01"))
    }

    @Test
    fun `getApod on network success caches thumbnailUrl`() = runTest {
        coEvery { apiService.getApod(any(), any()) } returns testDto(
            date = "2024-06-01",
            mediaType = "video",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        repository.getApod("2024-06-01")

        val cached = cacheDao.getByDate("2024-06-01")
        assertEquals("https://example.com/thumb.jpg", cached?.thumbnailUrl)
    }

    @Test
    fun `getApod on network failure returns cached entry when available`() = runTest {
        cacheDao.seed(testCacheEntity(date = "2024-06-01", title = "Cached Nebula"))
        coEvery { apiService.getApod(any(), any()) } throws RuntimeException("Offline")

        val result = repository.getApod("2024-06-01")

        assertTrue(result.isSuccess)
        assertEquals("Cached Nebula", result.getOrThrow().title)
    }

    @Test
    fun `getApod on network failure returns network error when cache empty`() = runTest {
        coEvery { apiService.getApod(any(), any()) } throws RuntimeException("Offline")

        val result = repository.getApod("2024-06-01")

        assertTrue(result.isFailure)
        assertEquals("Offline", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getApod evicts oldest entries when cache exceeds max size`() = runTest {
        val oldEntries = (1..CACHE_MAX_SIZE).map { i ->
            testCacheEntity(date = "2023-01-%02d".format(i), cachedAt = i.toLong())
        }
        cacheDao.seed(*oldEntries.toTypedArray())
        coEvery { apiService.getApod(any(), any()) } returns testDto(date = "2024-06-01")

        repository.getApod("2024-06-01")

        assertEquals(CACHE_MAX_SIZE, cacheDao.count())
        assertEquals(1, cacheDao.deleteOldestCallCount)
    }

    @Test
    fun `getApod does not evict when cache is below max size`() = runTest {
        coEvery { apiService.getApod(any(), any()) } returns testDto(date = "2024-06-01")

        repository.getApod("2024-06-01")

        assertEquals(0, cacheDao.deleteOldestCallCount)
    }
}

private fun testDto(
    date: String = "2024-01-15",
    title: String = "Andromeda Galaxy",
    explanation: String = "A test explanation.",
    url: String = "https://apod.nasa.gov/apod/image/test.jpg",
    hdUrl: String? = null,
    mediaType: String = "image",
    copyright: String? = null,
    thumbnailUrl: String? = null
) = ApodDto(
    date = date,
    title = title,
    explanation = explanation,
    url = url,
    hdUrl = hdUrl,
    mediaType = mediaType,
    copyright = copyright,
    thumbnailUrl = thumbnailUrl
)
