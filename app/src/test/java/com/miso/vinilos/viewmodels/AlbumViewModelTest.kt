package com.miso.vinilos.viewmodels

import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.Genre
import com.miso.vinilos.model.data.RecordLabel
import com.miso.vinilos.model.repository.AlbumRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Tests unitarios para AlbumViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    private lateinit var repository: AlbumRepository
    private lateinit var viewModel: AlbumViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAlbums success updates uiState to Success`() = runTest(testDispatcher) {
        // Given
        val albums = listOf(
            createTestAlbum(1, "Album 1"),
            createTestAlbum(2, "Album 2")
        )
        coEvery { repository.getAlbums() } returns Result.success(albums)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AlbumUiState.Success)
        assertEquals(albums, (state as AlbumUiState.Success).albums)
    }

    @Test
    fun `loadAlbums failure updates uiState to Error`() = runTest(testDispatcher) {
        // Given
        val error = Exception("Network error")
        coEvery { repository.getAlbums() } returns Result.failure(error)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AlbumUiState.Error)
        assertTrue((state as AlbumUiState.Error).message.contains("Error"))
    }

    @Test
    fun `loadAlbums sets Loading state initially`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.getAlbums() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(emptyList())
        }

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)

        // Then - should be loading initially
        assertTrue(viewModel.uiState.value is AlbumUiState.Loading)
    }

    @Test
    fun `refreshAlbums calls loadAlbums`() = runTest(testDispatcher) {
        // Given
        val albums = listOf(createTestAlbum(1, "Album 1"))
        coEvery { repository.getAlbums() } returns Result.success(albums)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()
        viewModel.refreshAlbums()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AlbumUiState.Success)
        assertEquals(albums, (state as AlbumUiState.Success).albums)
    }

    @Test
    fun `loadAlbumDetail success updates albumDetailState to Success`() = runTest(testDispatcher) {
        // Given
        val album = createTestAlbum(1, "Album 1")
        coEvery { repository.getAlbums() } returns Result.success(emptyList())
        coEvery { repository.getAlbum(1) } returns Result.success(album)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()
        viewModel.loadAlbumDetail(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.albumDetailState.value
        assertTrue(state is AlbumDetailUiState.Success)
        assertEquals(album, (state as AlbumDetailUiState.Success).album)
    }

    @Test
    fun `loadAlbumDetail failure updates albumDetailState to Error`() = runTest(testDispatcher) {
        // Given
        val error = Exception("Album not found")
        coEvery { repository.getAlbums() } returns Result.success(emptyList())
        coEvery { repository.getAlbum(1) } returns Result.failure(error)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()
        viewModel.loadAlbumDetail(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.albumDetailState.value
        assertTrue(state is AlbumDetailUiState.Error)
        assertTrue((state as AlbumDetailUiState.Error).message.contains("Error"))
    }

    @Test
    fun `clearAlbumDetail resets albumDetailState to Loading`() = runTest(testDispatcher) {
        // Given
        val album = createTestAlbum(1, "Album 1")
        coEvery { repository.getAlbums() } returns Result.success(emptyList())
        coEvery { repository.getAlbum(1) } returns Result.success(album)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()
        viewModel.loadAlbumDetail(1)
        advanceUntilIdle()
        viewModel.clearAlbumDetail()

        // Then
        assertTrue(viewModel.albumDetailState.value is AlbumDetailUiState.Loading)
    }

    @Test
    fun `loadAlbums handles connection error message`() = runTest(testDispatcher) {
        // Given
        val error = Exception("Unable to resolve host")
        coEvery { repository.getAlbums() } returns Result.failure(error)

        // When
        viewModel = AlbumViewModel(repository, testDispatcher)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is AlbumUiState.Error)
        assertTrue((state as AlbumUiState.Error).message.contains("servidor"))
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

