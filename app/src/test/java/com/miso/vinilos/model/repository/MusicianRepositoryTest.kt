package com.miso.vinilos.model.repository

import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.MusicianCreateDTO
import com.miso.vinilos.model.network.MusicianApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.Date

/**
 * Tests unitarios para MusicianRepository
 */
class MusicianRepositoryTest {

    private lateinit var apiService: MusicianApiService
    private lateinit var repository: MusicianRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = MusicianRepository(apiService)
    }

    @Test
    fun `getMusicians success returns list of musicians`() = runTest {
        // Given
        val musicians = listOf(
            createTestMusician(1, "Musician 1"),
            createTestMusician(2, "Musician 2")
        )
        val response = Response.success(musicians)
        coEvery { apiService.getMusicians() } returns response

        // When
        val result = repository.getMusicians()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(musicians, result.getOrNull())
    }

    @Test
    fun `getMusicians failure returns error`() = runTest {
        // Given
        val response = Response.error<List<Musician>>(500, ResponseBody.create(null, ""))
        coEvery { apiService.getMusicians() } returns response

        // When
        val result = repository.getMusicians()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Error") == true)
    }

    @Test
    fun `getMusicians exception returns error`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { apiService.getMusicians() } throws exception

        // When
        val result = repository.getMusicians()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getMusician success returns musician`() = runTest {
        // Given
        val musician = createTestMusician(1, "Musician 1")
        val response = Response.success(musician)
        coEvery { apiService.getMusician(1) } returns response

        // When
        val result = repository.getMusician(1)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(musician, result.getOrNull())
    }

    @Test
    fun `getMusician failure returns error`() = runTest {
        // Given
        val response = Response.error<Musician>(404, ResponseBody.create(null, ""))
        coEvery { apiService.getMusician(1) } returns response

        // When
        val result = repository.getMusician(1)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("no encontrado") == true)
    }

    @Test
    fun `createMusician success returns created musician`() = runTest {
        // Given
        val musicianDTO = MusicianCreateDTO(
            name = "New Musician",
            image = "https://example.com/image.jpg",
            description = "Description",
            birthDate = Date()
        )
        val createdMusician = createTestMusician(1, "New Musician")
        val response = Response.success(createdMusician)
        coEvery { apiService.createMusician(musicianDTO) } returns response

        // When
        val result = repository.createMusician(musicianDTO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdMusician, result.getOrNull())
    }

    @Test
    fun `updateMusician success returns updated musician`() = runTest {
        // Given
        val musicianDTO = MusicianCreateDTO(
            name = "Updated Musician",
            image = "https://example.com/image.jpg",
            description = "Updated Description",
            birthDate = Date()
        )
        val updatedMusician = createTestMusician(1, "Updated Musician")
        val response = Response.success(updatedMusician)
        coEvery { apiService.updateMusician(1, musicianDTO) } returns response

        // When
        val result = repository.updateMusician(1, musicianDTO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedMusician, result.getOrNull())
    }

    @Test
    fun `deleteMusician success returns Unit`() = runTest {
        // Given
        val response = Response.success<Unit>(Unit)
        coEvery { apiService.deleteMusician(1) } returns response

        // When
        val result = repository.deleteMusician(1)

        // Then
        assertTrue(result.isSuccess)
    }

    private fun createTestMusician(id: Int, name: String): Musician {
        return Musician(
            id = id,
            name = name,
            image = "https://example.com/image.jpg",
            description = "Test description",
            birthDate = Date()
        )
    }
}

