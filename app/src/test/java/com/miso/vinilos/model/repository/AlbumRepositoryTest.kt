package com.miso.vinilos.model.repository

import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.AlbumCreateDTO
import com.miso.vinilos.model.data.Genre
import com.miso.vinilos.model.data.RecordLabel
import com.miso.vinilos.model.data.Track
import com.miso.vinilos.model.network.AlbumApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import okhttp3.ResponseBody
import retrofit2.Response
import java.util.Date

/**
 * Tests unitarios para AlbumRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumRepositoryTest {

    private lateinit var apiService: AlbumApiService
    private lateinit var repository: AlbumRepository
    private lateinit var testDispatcher: TestDispatcher

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            // Configurar Dispatchers.Main con Unconfined antes de crear cualquier TestDispatcher
            // Esto evita el error "The main looper is not available"
            Dispatchers.setMain(Dispatchers.Unconfined)
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            Dispatchers.resetMain()
        }
    }

    @Before
    fun setup() {
        // Ahora podemos crear UnconfinedTestDispatcher porque Dispatchers.Main ya está configurado
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        apiService = mockk()
        repository = AlbumRepository(apiService)
    }

    @After
    fun tearDown() {
        // Limpiar Dispatchers.Main después de cada test
        Dispatchers.resetMain()
        // Restaurar Dispatchers.Unconfined para el siguiente test
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @Test
    fun `getAlbums success returns list of albums`() = runTest {
        // Given
        val albums = listOf(
            createTestAlbum(1, "Album 1"),
            createTestAlbum(2, "Album 2")
        )
        val response = Response.success(albums)
        coEvery { apiService.getAlbums() } returns response

        // When
        val result = repository.getAlbums()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(albums, result.getOrNull())
    }

    @Test
    fun `getAlbums failure returns error`() = runTest {
        // Given
        val response = Response.error<List<Album>>(500, ResponseBody.create(null, ""))
        coEvery { apiService.getAlbums() } returns response

        // When
        val result = repository.getAlbums()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Error") == true)
    }

    @Test
    fun `getAlbums exception returns error`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { apiService.getAlbums() } throws exception

        // When
        val result = repository.getAlbums()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getAlbum success returns album`() = runTest {
        // Given
        val album = createTestAlbum(1, "Album 1")
        val response = Response.success(album)
        coEvery { apiService.getAlbum(1) } returns response

        // When
        val result = repository.getAlbum(1)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(album, result.getOrNull())
    }

    @Test
    fun `getAlbum failure returns error`() = runTest {
        // Given
        val response = Response.error<Album>(404, ResponseBody.create(null, ""))
        coEvery { apiService.getAlbum(1) } returns response

        // When
        val result = repository.getAlbum(1)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("no encontrado") == true)
    }

    @Test
    fun `createAlbum success returns created album`() = runTest {
        // Given
        val albumDTO = AlbumCreateDTO(
            name = "New Album",
            cover = "https://example.com/cover.jpg",
            releaseDate = Date(),
            description = "Description",
            genre = Genre.ROCK,
            recordLabel = RecordLabel.SONY
        )
        val createdAlbum = createTestAlbum(1, "New Album")
        val response = Response.success(createdAlbum)
        coEvery { apiService.createAlbum(albumDTO) } returns response

        // When
        val result = repository.createAlbum(albumDTO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdAlbum, result.getOrNull())
    }

    @Test
    fun `updateAlbum success returns updated album`() = runTest {
        // Given
        val albumDTO = AlbumCreateDTO(
            name = "Updated Album",
            cover = "https://example.com/cover.jpg",
            releaseDate = Date(),
            description = "Description",
            genre = Genre.ROCK,
            recordLabel = RecordLabel.SONY
        )
        val updatedAlbum = createTestAlbum(1, "Updated Album")
        val response = Response.success(updatedAlbum)
        coEvery { apiService.updateAlbum(1, albumDTO) } returns response

        // When
        val result = repository.updateAlbum(1, albumDTO)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedAlbum, result.getOrNull())
    }

    @Test
    fun `getAlbumTracks success returns list of tracks`() = runTest {
        // Given
        val tracks = listOf(
            Track(id = 1, name = "Track 1", duration = "3:30"),
            Track(id = 2, name = "Track 2", duration = "4:00")
        )
        val response = Response.success(tracks)
        coEvery { apiService.getAlbumTracks(1) } returns response

        // When
        val result = repository.getAlbumTracks(1)

        // Then
        assertTrue(result.isSuccess)
        val resultTracks = result.getOrNull()
        assertNotNull(resultTracks)
        assertEquals(2, resultTracks?.size)
        assertEquals("Track 1", resultTracks?.get(0)?.name)
    }

    @Test
    fun `addTrackToAlbum success returns updated album`() = runTest {
        // Given
        val album = createTestAlbum(1, "Album 1")
        val response = Response.success(album)
        coEvery { apiService.addTrackToAlbum(1, 1) } returns response

        // When
        val result = repository.addTrackToAlbum(1, 1)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(album, result.getOrNull())
    }

    private fun createTestAlbum(id: Int, name: String): Album {
        return Album(
            id = id,
            name = name,
            cover = "https://example.com/cover.jpg",
            releaseDate = Date(),
            description = "Test description",
            genre = Genre.ROCK,
            recordLabel = RecordLabel.SONY
        )
    }
}

