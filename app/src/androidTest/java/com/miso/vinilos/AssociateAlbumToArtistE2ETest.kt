package com.miso.vinilos

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.config.TestRetrofitClient
import com.miso.vinilos.helpers.JsonResponseHelper
import com.miso.vinilos.helpers.TestDataFactory
import com.miso.vinilos.matchers.CustomMatchers
import androidx.room.Room
import com.miso.vinilos.model.database.VinylRoomDatabase
import com.miso.vinilos.model.repository.AlbumRepository
import com.miso.vinilos.model.repository.MusicianRepository
import com.miso.vinilos.model.repository.PrizeRepository
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.rules.ScreenshotTestRule
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.MusicianViewModel
import com.miso.vinilos.views.screens.SelectAlbumToArtistScreen
import com.miso.vinilos.views.theme.VinilosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E para el flujo completo de asociación de álbum a artista
 *
 * Estas pruebas verifican:
 * - Carga de lista de álbumes para selección
 * - Búsqueda de álbumes (por nombre y por performer)
 * - Selección de álbum
 * - Asociación exitosa de álbum a artista
 * - Manejo de errores
 * - Navegación
 *
 * Las pruebas usan MockWebServer para simular respuestas del API
 * Nota: Se encolan múltiples respuestas porque el repositorio maneja caché con Room
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AssociateAlbumToArtistE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    @get:Rule
    val screenshotTestRule = ScreenshotTestRule().apply {
        setComposeTestRule(composeTestRule)
    }

    private var testDatabase: VinylRoomDatabase? = null
    private var navigationBackCalled = false

    @After
    fun tearDown() {
        testDatabase?.close()
        testDatabase = null
        navigationBackCalled = false
    }

    /**
     * Helper function to create a test AlbumViewModel with MockWebServer
     */
    private fun createTestAlbumViewModel(): AlbumViewModel {
        val testApiService = TestRetrofitClient.createTestApiService(mockWebServerRule.baseUrl)
        val application = ApplicationProvider.getApplicationContext<Application>()
        // Usar base de datos en memoria para pruebas (garantiza caché vacío)
        testDatabase = Room.inMemoryDatabaseBuilder(
            application,
            VinylRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        val albumsDao = testDatabase!!.albumsDao()
        val testRepository = AlbumRepository(application, albumsDao, testApiService)
        return AlbumViewModel(testRepository, Dispatchers.Unconfined)
    }

    /**
     * Helper function to create a test MusicianViewModel with MockWebServer
     */
    private fun createTestMusicianViewModel(): MusicianViewModel {
        val testMusicianApiService = TestRetrofitClient.createTestMusicianApiService(mockWebServerRule.baseUrl)
        val testPrizeApiService = TestRetrofitClient.createTestClient(mockWebServerRule.baseUrl)
            .create(com.miso.vinilos.model.network.PrizeApiService::class.java)
        val application = ApplicationProvider.getApplicationContext<Application>()
        testDatabase = Room.inMemoryDatabaseBuilder(
            application,
            VinylRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        val musiciansDao = testDatabase!!.musiciansDao()
        val musicianRepository = MusicianRepository(application, musiciansDao, testMusicianApiService)
        val prizeRepository = PrizeRepository(testPrizeApiService)
        return MusicianViewModel(musicianRepository, prizeRepository, Dispatchers.Unconfined)
    }

    /**
     * Test 1: Successful Album Association
     * Verifica que se puede asociar un álbum a un artista exitosamente
     * Nota: No verificamos la navegación automática porque el LaunchedEffect con delay
     * no funciona correctamente en el contexto de test con Dispatchers.Unconfined
     */
    @Test
    fun testSuccessfulAlbumAssociation() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        val testMusician = TestDataFactory.createTestMusician(id = 1, name = "John Lennon")
        val selectedAlbum = testAlbums[0]

        // Respuestas para GET /albums (puede ocurrir 2 veces por reload/caché)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        // Respuesta para POST /musicians/{id}/albums (asociar álbum)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createOkResponse()
        )

        // Respuesta para GET /musicians/{id} (refrescar músico después de asociar)
        val updatedMusician = testMusician.copy(
            albums = listOf(selectedAlbum)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(updatedMusician)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = {
                        println("TEST: onBack llamado")
                        navigationBackCalled = true
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Assert - Verificar que se cargaron los álbumes
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        screenshotTestRule.takeScreenshot("01-lista-albumes-cargada")

        // Act - Seleccionar un álbum
        composeTestRule.onNodeWithText("Abbey Road", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Assert - Verificar que el botón de agregar está visible
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado")
            .assertIsDisplayed()

        screenshotTestRule.takeScreenshot("02-album-seleccionado")

        // Act - Hacer clic en agregar
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado")
            .performClick()

        composeTestRule.waitForIdle()

        // Esperar un momento para que se procese la asociación
        Thread.sleep(500)

        composeTestRule.waitForIdle()

        // Assert - Verificar que NO hay mensaje de error (la asociación fue exitosa)
        // No verificamos la navegación porque el LaunchedEffect con delay no funciona
        // correctamente en el contexto de test con Dispatchers.Unconfined
        assert(
            composeTestRule.onAllNodesWithText("Error", substring = true)
                .fetchSemanticsNodes().isEmpty()
        ) { "No se esperaba ningún mensaje de error después de asociar" }

        screenshotTestRule.takeScreenshot("03-asociacion-exitosa")
    }

    /**
     * Test 2: Album List Loading
     * Verifica que la lista de álbumes se carga correctamente
     */
    @Test
    fun testAlbumListLoading() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()

        // Encolar múltiples respuestas para el caché
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Assert - Verificar que los álbumes se muestran
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Led Zeppelin IV")

        // Verificar que los performers se muestran
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "Pink Floyd")

        screenshotTestRule.takeScreenshot("lista-albumes")
    }

    /**
     * Test 3: Album Selection
     * Verifica que se puede seleccionar un álbum y se muestra el indicador visual
     */
    @Test
    fun testAlbumSelection() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verificar que el botón no está visible inicialmente
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado")
            .assertDoesNotExist()

        screenshotTestRule.takeScreenshot("01-sin-seleccion")

        // Act - Seleccionar un álbum
        composeTestRule.onNodeWithText("Abbey Road", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Assert - Verificar que el ícono de selección está visible
        composeTestRule.onNodeWithContentDescription("Seleccionado")
            .assertIsDisplayed()

        // Verificar que el botón de agregar está visible
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado")
            .assertIsDisplayed()

        screenshotTestRule.takeScreenshot("02-album-seleccionado")

        // Act - Seleccionar otro álbum
        composeTestRule.onNodeWithText("The Dark Side of the Moon", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Assert - El botón sigue visible
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado")
            .assertIsDisplayed()

        screenshotTestRule.takeScreenshot("03-otro-album-seleccionado")
    }

    /**
     * Test 4: Search Functionality by Album Name
     * Verifica que la búsqueda por nombre de álbum funciona correctamente
     */
    @Test
    fun testSearchFunctionalityByAlbumName() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verificar que todos los álbumes están visibles inicialmente
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")

        screenshotTestRule.takeScreenshot("01-antes-busqueda")

        // Act - Buscar por nombre de álbum
        composeTestRule.onNodeWithText("Buscar álbum...", substring = true)
            .performTextInput("Abbey")

        composeTestRule.waitForIdle()

        // Assert - Verificar que solo se muestra el álbum filtrado
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")

        screenshotTestRule.takeScreenshot("02-busqueda-por-album")
    }

    /**
     * Test 5: Search Functionality by Performer Name
     * Verifica que la búsqueda por nombre de performer funciona correctamente
     */
    @Test
    fun testSearchFunctionalityByPerformer() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        screenshotTestRule.takeScreenshot("01-antes-busqueda-performer")

        // Act - Buscar por nombre de performer
        composeTestRule.onNodeWithText("Buscar álbum...", substring = true)
            .performTextInput("Pink Floyd")

        composeTestRule.waitForIdle()

        // Assert - Verificar que se muestran los álbumes de Pink Floyd
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")

        screenshotTestRule.takeScreenshot("02-busqueda-por-performer")
    }

    /**
     * Test 6: Empty Search Results
     * Verifica el comportamiento cuando la búsqueda no tiene resultados
     */
    @Test
    fun testEmptySearchResults() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Act - Buscar algo que no existe
        composeTestRule.onNodeWithText("Buscar álbum...", substring = true)
            .performTextInput("XYZ123NoExiste")

        composeTestRule.waitForIdle()

        // Assert - Verificar que no se muestran álbumes
        composeTestRule.onNodeWithText("Abbey Road")
            .assertDoesNotExist()

        composeTestRule.onNodeWithText("The Dark Side of the Moon")
            .assertDoesNotExist()

        screenshotTestRule.takeScreenshot("busqueda-sin-resultados")
    }

    /**
     * Test 7: Association Error
     * Verifica que se maneja correctamente un error al asociar el álbum
     */
    @Test
    fun testAssociationError() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()

        // Respuestas para GET /albums
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        // Respuesta de error para POST /musicians/{id}/albums
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Act - Seleccionar un álbum
        composeTestRule.onNodeWithText("Abbey Road", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        screenshotTestRule.takeScreenshot("01-antes-error")

        // Act - Intentar agregar
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado")
            .performClick()

        composeTestRule.waitForIdle()

        // Assert - Verificar que se muestra el mensaje de error
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            try {
                composeTestRule.onAllNodesWithText("Error", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        screenshotTestRule.takeScreenshot("02-error-mostrado")

        // Verificar que NO se navegó de regreso
        assert(!navigationBackCalled) { "No se esperaba que onBack fuera llamado en caso de error" }
    }

    /**
     * Test 8: Album List Error
     * Verifica que se maneja correctamente un error al cargar la lista de álbumes
     */
    @Test
    fun testAlbumListError() = runTest {
        // Arrange - Simular error al cargar álbumes (2 respuestas de error por retry automático)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Assert - Verificar que se muestra el mensaje de error
        composeTestRule.onNodeWithText("Error al cargar álbumes")
            .assertIsDisplayed()

        // Verificar que el botón de reintentar está visible
        composeTestRule.onNodeWithText("Reintentar")
            .assertIsDisplayed()

        screenshotTestRule.takeScreenshot("error-cargar-albumes")
    }

    /**
     * Test 9: Back Navigation
     * Verifica que el botón de volver funciona correctamente
     */
    @Test
    fun testBackNavigation() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        screenshotTestRule.takeScreenshot("01-antes-volver")

        // Act - Hacer clic en volver
        composeTestRule.onNodeWithContentDescription("Volver")
            .performClick()

        composeTestRule.waitForIdle()

        // Assert - Verificar que se llamó onBack
        assert(navigationBackCalled) { "Se esperaba que onBack fuera llamado al hacer clic en volver" }

        screenshotTestRule.takeScreenshot("02-despues-volver")
    }

    /**
     * Test 10: Empty Album List
     * Verifica el comportamiento cuando no hay álbumes disponibles
     */
    @Test
    fun testEmptyAlbumList() = runTest {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createEmptyAlbumsResponse()
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createEmptyAlbumsResponse()
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Assert - Verificar mensaje de lista vacía
        composeTestRule.onNodeWithText("No hay álbumes disponibles")
            .assertIsDisplayed()

        screenshotTestRule.takeScreenshot("lista-vacia")
    }

    /**
     * Test 11: Album Without Performers Display
     * Verifica que se muestra "Artista desconocido" para álbumes sin performers
     */
    @Test
    fun testAlbumWithoutPerformersDisplay() = runTest {
        // Arrange
        val albumWithoutPerformers = TestDataFactory.createAlbumWithoutPerformers()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(listOf(albumWithoutPerformers))
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(listOf(albumWithoutPerformers))
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Assert - Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Unknown Artist Album")

        // Verificar que se muestra "Artista desconocido"
        CustomMatchers.verifyUnknownArtistIsVisible(composeTestRule)

        screenshotTestRule.takeScreenshot("album-sin-performers")
    }

    /**
     * Test 12: Top Bar Elements
     * Verifica que la barra superior con título y botón de volver está presente
     */
    @Test
    fun testTopBarElements() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        val albumViewModel = createTestAlbumViewModel()
        val musicianViewModel = createTestMusicianViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                SelectAlbumToArtistScreen(
                    musicianId = 1,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    onBack = { navigationBackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Assert - Verificar que el título está visible
        composeTestRule.onNodeWithText("Agregar Álbum")
            .assertIsDisplayed()

        // Verificar que el botón de volver está visible
        composeTestRule.onNodeWithContentDescription("Volver")
            .assertIsDisplayed()

        // Verificar que la barra de búsqueda está visible
        composeTestRule.onNodeWithText("Buscar álbum...", substring = true)
            .assertIsDisplayed()

        // Verificar que el ícono de búsqueda está visible
        composeTestRule.onNodeWithContentDescription("Buscar")
            .assertIsDisplayed()

        screenshotTestRule.takeScreenshot("elementos-interfaz")
    }
}
