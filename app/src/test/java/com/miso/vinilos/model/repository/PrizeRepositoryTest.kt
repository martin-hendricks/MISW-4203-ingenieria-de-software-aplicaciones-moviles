package com.miso.vinilos.model.repository

import com.miso.vinilos.model.data.Prize
import com.miso.vinilos.model.data.PrizeCreateDTO
import com.miso.vinilos.model.network.PrizeApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Tests unitarios para PrizeRepository
 */
class PrizeRepositoryTest {

    private lateinit var apiService: PrizeApiService
    private lateinit var repository: PrizeRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = PrizeRepository(apiService)
    }

    @Test
    fun `getPrizes success returns list of prizes`() = runTest {
        // Given
        val prizes = listOf(
            createTestPrize(1, "Prize 1"),
            createTestPrize(2, "Prize 2")
        )
        val response = Response.success(prizes)
        coEvery { apiService.getPrizes() } returns response

        // When
        val result = repository.getPrizes()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(prizes, result.getOrNull())
    }

    @Test
    fun `getPrizes failure returns error`() = runTest {
        // Given
        val response = Response.error<List<Prize>>(500, ResponseBody.create(null, ""))
        coEvery { apiService.getPrizes() } returns response

        // When
        val result = repository.getPrizes()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Error") == true)
    }

    @Test
    fun `getPrizes exception returns error`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { apiService.getPrizes() } throws exception

        // When
        val result = repository.getPrizes()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getPrize success returns prize`() = runTest {
        // Given
        val prize = createTestPrize(1, "Prize 1")
        val response = Response.success(prize)
        coEvery { apiService.getPrize(1) } returns response

        // When
        val result = repository.getPrize(1)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(prize, result.getOrNull())
    }

    @Test
    fun `getPrize failure returns error`() = runTest {
        // Given
        val response = Response.error<Prize>(404, ResponseBody.create(null, ""))
        coEvery { apiService.getPrize(1) } returns response

        // When
        val result = repository.getPrize(1)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("no encontrado") == true)
    }

    @Test
    fun `createPrize success returns created prize`() = runTest {
        // Given
        val prizeDTO = PrizeCreateDTO(
            name = "New Prize",
            description = "Description",
            organization = "Organization"
        )
        val createdPrize = createTestPrize(1, "New Prize")
        val response = Response.success(createdPrize)
        coEvery { apiService.createPrize(prizeDTO) } returns response

        // When
        val result = repository.createPrize(prizeDTO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdPrize, result.getOrNull())
    }

    @Test
    fun `updatePrize success returns updated prize`() = runTest {
        // Given
        val prizeDTO = PrizeCreateDTO(
            name = "Updated Prize",
            description = "Updated Description",
            organization = "Updated Organization"
        )
        val updatedPrize = createTestPrize(1, "Updated Prize")
        val response = Response.success(updatedPrize)
        coEvery { apiService.updatePrize(1, prizeDTO) } returns response

        // When
        val result = repository.updatePrize(1, prizeDTO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedPrize, result.getOrNull())
    }

    @Test
    fun `deletePrize success returns Unit`() = runTest {
        // Given
        val response = Response.success<Unit>(Unit)
        coEvery { apiService.deletePrize(1) } returns response

        // When
        val result = repository.deletePrize(1)

        // Then
        assertTrue(result.isSuccess)
    }

    private fun createTestPrize(id: Int, name: String): Prize {
        return Prize(
            id = id,
            name = name,
            description = "Test description",
            organization = "Test organization"
        )
    }
}

