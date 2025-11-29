package com.miso.vinilos

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.config.TestRetrofitClient
import com.miso.vinilos.helpers.JsonResponseHelper
import com.miso.vinilos.helpers.TestDataFactory
import com.miso.vinilos.matchers.CustomMatchers
import androidx.room.Room
import com.miso.vinilos.model.database.VinylRoomDatabase
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.rules.ScreenshotTestRule
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.theme.VinilosTheme
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.MusicianViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel
import com.miso.vinilos.model.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E para el flujo completo de asociar un álbum a un artista
 * 
 * Estas pruebas verifican:
 * - Navegación al detalle del artista
 * - Acceso a la pantalla de seleccionar álbum
 * - Selección de un álbum
 * - Asociación del álbum al artista
 * - Verificación de éxito y actualización del detalle
 * - Manejo de errores
 * 
 * Las pruebas usan MockWebServer para simular respuestas del API
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

    @After
    fun tearDown() {
        testDatabase?.close()
        testDatabase = null
    }

    /**
     * Helper function to create a test ProfileViewModel with COLLECTOR role
     */
    private fun createTestProfileViewModel(): ProfileViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val profileViewModel = ProfileViewModel(application)
        // Establecer el rol como COLLECTOR para que el botón de agregar sea visible
        runBlocking {
            profileViewModel.selectRole(UserRole.COLLECTOR)
            // Esperar más tiempo para que DataStore actualice y el StateFlow se propague
            kotlinx.coroutines.delay(500)
        }
        return profileViewModel
    }

    /**
     * Helper function to create test ViewModels with MockWebServer
     */
    private fun createTestViewModels(): Pair<AlbumViewModel, MusicianViewModel> {
        val testAlbumApiService = TestRetrofitClient.createTestApiService(mockWebServerRule.baseUrl)
        val testMusicianApiService = TestRetrofitClient.createTestMusicianApiService(mockWebServerRule.baseUrl)
        val application = ApplicationProvider.getApplicationContext<Application>()

        // Crear base de datos en memoria pero DESHABILITAR el cache
        testDatabase = Room.inMemoryDatabaseBuilder(
            application,
            VinylRoomDatabase::class.java
        )
        .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .build()

        val albumsDao = testDatabase!!.albumsDao()
        val musiciansDao = testDatabase!!.musiciansDao()

        // IMPORTANTE: enableCache = false para que NO use Room, solo el API mockeado
        val albumRepository = com.miso.vinilos.model.repository.AlbumRepository(
            application, albumsDao, testAlbumApiService, enableCache = false
        )
        val musicianRepository = com.miso.vinilos.model.repository.MusicianRepository(
            application, musiciansDao, testMusicianApiService, enableCache = false
        )
        val prizeRepository = com.miso.vinilos.model.repository.PrizeRepository.getInstance()

        val albumViewModel = AlbumViewModel(albumRepository, Dispatchers.Unconfined)
        val musicianViewModel = MusicianViewModel(
            musicianRepository, prizeRepository, Dispatchers.Unconfined
        )

        return Pair(albumViewModel, musicianViewModel)
    }

    /**
     * Test 1: Successful Album Association to Artist
     * Verifica que se puede asociar un álbum a un artista exitosamente
     */
    @Test
    fun testSuccessfulAlbumAssociationToArtist() {
        // Arrange - Configurar respuestas del servidor mock
        // IMPORTANTE: Encolar las respuestas ANTES de crear los ViewModels
        // porque MusicianViewModel carga automáticamente en init
        val testMusicians = TestDataFactory.createTestMusicians()
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()
        val testAlbums = TestDataFactory.createTestAlbums()
        val albumToAssociate = testAlbums.first()

        // Agregar logging para ver qué requests se están haciendo
        var requestCount = 0

        // Usar un dispatcher personalizado que loguea cada request
        val loggingDispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestCount++
                val method = request.method
                val path = request.path
                android.util.Log.d("TEST_REQUEST", "Request #$requestCount: $method $path")
                println("TEST_REQUEST #$requestCount: $method $path")

                // Obtener la siguiente respuesta de la cola
                return mockWebServerRule.server.takeRequest().let {
                    // Este takeRequest ya consumió la request, ahora devolvemos la respuesta
                    // No podemos usar takeRequest aquí porque ya estamos en dispatch
                    // En su lugar, retornamos un MockResponse vacío y dejamos que el servidor use las respuestas encoladas
                    MockResponse().setResponseCode(500) // Placeholder - el servidor usará las respuestas encoladas
                }
            }
        }

        // PRIMERA respuesta: init de AlbumViewModel (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SEGUNDA respuesta: init de MusicianViewModel (GET /musicians)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // TERCERA respuesta: refresh automático de la pantalla de álbumes inicial (ON_RESUME AlbumListScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // CUARTA respuesta: refresh automático de la lista de artistas cuando se navega (ON_RESUME ArtistListScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // QUINTA respuesta: detalle del artista al hacer clic (GET /musicians/{id})
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        // SEXTA respuesta: posible refresh del detalle del artista
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        // SÉPTIMA respuesta: lista de álbumes para pantalla de selección (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // OCTAVA respuesta: refresh automático de la lista de álbumes en selección (ON_RESUME AlbumSelectionScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // NOVENA respuesta: asociar álbum (POST /musicians/{id}/albums/{albumId})
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createOkResponse()
        )
        // DÉCIMA respuesta: refrescar el detalle del artista después de asociar (GET /musicians/{id})
        val updatedMusician = testMusician.copy(
            albums = (testMusician.albums ?: emptyList()) + albumToAssociate
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(updatedMusician)
        )

        // Crear los ViewModels DESPUÉS de encolar las respuestas
        // El MusicianViewModel cargará automáticamente y usará la primera respuesta
        val (albumViewModel, musicianViewModel) = createTestViewModels()
        val profileViewModel = createTestProfileViewModel()

        // Esperar un poco para que el init del ViewModel inicie la carga
        Thread.sleep(100)

        // Act - Configurar la UI
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }

        // Esperar a que cargue la pantalla inicial (álbumes)
        composeTestRule.waitForIdle()

        // Esperar a que los álbumes se carguen antes de navegar
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText("Abbey Road", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Capturar screenshot del estado inicial
        screenshotTestRule.takeScreenshot("01-pantalla-inicial")

        // Navegar a la pantalla de artistas
        composeTestRule.onNodeWithText("Artistas")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Esperar a que la navegación se complete y la pantalla de artistas esté visible
        // Verificamos que la lista de artistas se haya cargado (no solo el título)
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                // Verificar que "John Lennon" está visible, lo que indica que la lista se cargó
                composeTestRule.onAllNodesWithText("John Lennon", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Pequeña pausa para asegurar que la UI está completamente estable
        Thread.sleep(300)

        // Capturar screenshot de la lista de artistas
        screenshotTestRule.takeScreenshot("02-lista-artistas")

        // Hacer clic en el primer artista para ver su detalle
        composeTestRule.onNodeWithText("John Lennon", substring = true)
            .assertIsDisplayed()
            .performClick()

        // Esperar a que cargue el detalle del artista
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithText("John Lennon")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        Thread.sleep(300)
        // VALIDAR EL DETALLE DEL ARTISTA (similar a ArtistDetailE2ETest)
        // Verificar que el artista está visible
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")

        // Verificar que la descripción está visible
        composeTestRule.onNodeWithText("Músico y compositor británico, miembro de The Beatles", substring = true)
            .assertExists()

        // Verificar que la sección de álbumes está presente
        // Usar el primer nodo (el del detalle del artista), no el tab de navegación
        composeTestRule.onAllNodesWithText("Álbumes", substring = true)[0]
            .assertExists()

        // Capturar screenshot del detalle del artista validado
        screenshotTestRule.takeScreenshot("03-detalle-artista-validado")

        // Esperar un poco para que el rol se propague y el botón sea visible
        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Hacer clic en el botón PLUS de la sección de álbumes en el detalle del artista
        // El botón tiene contentDescription "Agregar álbum" y está en la sección de álbumes
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithContentDescription("Agregar álbum")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithContentDescription("Agregar álbum")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Capturar screenshot de la pantalla de seleccionar álbum
        screenshotTestRule.takeScreenshot("04-seleccionar-album")

        // Verificar que estamos en la pantalla de seleccionar álbum
        composeTestRule.onNodeWithText("Agregar Álbum", substring = true)
            .assertIsDisplayed()

        // Esperar a que la lista de álbumes se cargue
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithText(albumToAssociate.name, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Seleccionar un álbum de la lista
        composeTestRule.onNodeWithText(albumToAssociate.name, substring = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Capturar screenshot con álbum seleccionado
        screenshotTestRule.takeScreenshot("05-album-seleccionado")

        // Hacer clic en el botón "Agregar Álbum Seleccionado"
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado", substring = true)
            .assertIsDisplayed()
            .performClick()

        // Esperar a que se complete la asociación
        // El ViewModel hace: POST para asociar -> luego GET para refrescar el detalle
        // El SelectAlbumToArtistScreen espera 500ms antes de navegar de vuelta
        composeTestRule.waitForIdle()

        // Esperar a que se complete el refresh del detalle (puede tardar un poco)
        // El refreshMusicianDetail se ejecuta después del POST exitoso
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // Verificar que volvimos al detalle del artista
        // Esto puede tardar porque el SelectAlbumToArtistScreen espera 500ms antes de navegar
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithText("John Lennon")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Verificar que el artista sigue visible (confirmando que volvimos al detalle)
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")

        // Esperar a que el detalle se refresque completamente con el nuevo álbum
        // El refreshMusicianDetail ya se ejecutó, pero puede tardar en actualizar la UI
        // Esperamos a que el álbum aparezca en la sección de álbumes
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            try {
                // Buscar el álbum en la sección de álbumes del detalle
                composeTestRule.onAllNodesWithText(albumToAssociate.name, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Verificar que el álbum asociado está visible en la sección de álbumes del artista
        // Usar onAllNodesWithText porque puede haber múltiples instancias (en lista y en detalle)
        val albumNodes = composeTestRule.onAllNodesWithText(albumToAssociate.name, substring = true)
        assert(albumNodes.fetchSemanticsNodes().isNotEmpty()) {
            "El álbum ${albumToAssociate.name} debería estar visible en la sección de álbumes del artista"
        }

        // Capturar screenshot final con el álbum asociado visible en el detalle
        screenshotTestRule.takeScreenshot("06-album-asociado-visible")
    }

    /**
     * Test 2: Associate Album Error Handling
     * Verifica que se manejan correctamente los errores al asociar un álbum
     */
    @Test
    fun testAssociateAlbumErrorHandling() {
        // Arrange - IMPORTANTE: Encolar las respuestas ANTES de crear los ViewModels
        val testMusicians = TestDataFactory.createTestMusicians()
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()
        val testAlbums = TestDataFactory.createTestAlbums()

        // PRIMERA respuesta: init de AlbumViewModel (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SEGUNDA respuesta: init de MusicianViewModel (GET /musicians)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // TERCERA respuesta: refresh automático de la pantalla de álbumes inicial (ON_RESUME AlbumListScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // CUARTA respuesta: refresh automático de la lista de artistas cuando se navega (ON_RESUME ArtistListScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // QUINTA respuesta: detalle del artista al hacer clic (GET /musicians/{id})
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        // SEXTA respuesta: lista de álbumes para pantalla de selección (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SÉPTIMA respuesta: refresh automático de la lista de álbumes en selección (ON_RESUME AlbumSelectionScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // OCTAVA respuesta: error al asociar (POST /musicians/{id}/albums/{albumId})
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )

        // Crear los ViewModels DESPUÉS de encolar las respuestas
        val (albumViewModel, musicianViewModel) = createTestViewModels()
        val profileViewModel = createTestProfileViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Esperar a que los álbumes se carguen antes de navegar
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText("Abbey Road", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Navegar a artistas
        composeTestRule.onNodeWithText("Artistas")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Esperar a que la navegación se complete y la pantalla de artistas esté visible
        // Verificamos que la lista de artistas se haya cargado (no solo el título)
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                // Verificar que "John Lennon" está visible, lo que indica que la lista se cargó
                composeTestRule.onAllNodesWithText("John Lennon", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Pequeña pausa para asegurar que la UI está completamente estable
        Thread.sleep(300)
        
        // Hacer clic en un artista
        composeTestRule.onNodeWithText("John Lennon", substring = true)
            .assertIsDisplayed()
            .performClick()
        
        // Esperar a que cargue el detalle del artista
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            try {
                composeTestRule.onAllNodesWithText("John Lennon")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        
        // VALIDAR EL DETALLE DEL ARTISTA
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        // Verificar que la sección de álbumes está presente
        // Usar el primer nodo (el del detalle del artista), no el tab de navegación
        composeTestRule.onAllNodesWithText("Álbumes", substring = true)[0]
            .assertExists()
        
        // Esperar un poco para que el rol se propague y el botón sea visible
        Thread.sleep(1500)
        composeTestRule.waitForIdle()
        
        // Hacer clic en el botón PLUS de la sección de álbumes en el detalle del artista
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithContentDescription("Agregar álbum")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithContentDescription("Agregar álbum")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Esperar a que la lista de álbumes se cargue en la pantalla de selección
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            try {
                composeTestRule.onAllNodesWithText(testAlbums.first().name, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        
        // Seleccionar un álbum
        val albumToAssociate = testAlbums.first()
        composeTestRule.onNodeWithText(albumToAssociate.name, substring = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        
        // Intentar agregar (esto debería fallar)
        composeTestRule.onNodeWithText("Agregar Álbum Seleccionado", substring = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verificar que se muestra un mensaje de error
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText("Error", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Capturar screenshot del error
        screenshotTestRule.takeScreenshot("error-asociar-album")
    }

    /**
     * Test 3: Album Selection and Search
     * Verifica que se puede buscar y seleccionar un álbum en la pantalla de selección
     */
    @Test
    fun testAlbumSelectionAndSearch() {
        // Arrange - IMPORTANTE: Encolar las respuestas ANTES de crear los ViewModels
        val testMusicians = TestDataFactory.createTestMusicians()
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()
        val testAlbums = TestDataFactory.createTestAlbums()

        // PRIMERA respuesta: init de AlbumViewModel (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SEGUNDA respuesta: init de MusicianViewModel (GET /musicians)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // TERCERA respuesta: refresh automático de la lista de artistas (ON_RESUME)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // CUARTA respuesta: detalle del artista al hacer clic (GET /musicians/{id})
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        // QUINTA respuesta: refresh automático del detalle del artista (ON_RESUME)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        // SEXTA respuesta: lista de álbumes para pantalla de selección (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SEXTA respuesta: lista de álbumes para pantalla de selección (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SEXTA respuesta: lista de álbumes para pantalla de selección (GET /albums)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // SÉPTIMA respuesta: refresh automático de la lista de álbumes (ON_RESUME de SelectAlbumToArtistScreen)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        // Crear los ViewModels DESPUÉS de encolar las respuestas
        val (albumViewModel, musicianViewModel) = createTestViewModels()
        val profileViewModel = createTestProfileViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = albumViewModel,
                    musicianViewModel = musicianViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        
        // Navegar a artistas y luego al detalle
        composeTestRule.onNodeWithText("Artistas")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        
        // Esperar a que la lista de artistas se cargue completamente
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            try {
                composeTestRule.onAllNodesWithText("John Lennon", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        composeTestRule.waitForIdle()
        // Hacer clic en el artista
        composeTestRule.onNodeWithText("John Lennon", substring = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        // Esperar a que cargue el detalle del artista
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            try {
                composeTestRule.onAllNodesWithText("John Lennon")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.waitForIdle()
        // VALIDAR EL DETALLE DEL ARTISTA
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        // Verificar que la sección de álbumes está presente
        // Usar el primer nodo (el del detalle del artista), no el tab de navegación
        composeTestRule.onAllNodesWithText("Álbumes", substring = true)[0]
            .assertExists()
        // Esperar un poco para que el rol se propague y el botón sea visible
        Thread.sleep(1500)
        composeTestRule.waitForIdle()
        
        // Hacer clic en el botón PLUS de la sección de álbumes en el detalle del artista
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithContentDescription("Agregar álbum")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithContentDescription("Agregar álbum")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verificar que la barra de búsqueda está visible
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            try {
                composeTestRule.onAllNodesWithText("Buscar álbum...", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithText("Buscar álbum...", substring = true)
            .assertIsDisplayed()
        
        // Buscar un álbum específico
        composeTestRule.onNodeWithText("Buscar álbum...", substring = true)
            .performTextInput("Abbey")
        
        composeTestRule.waitForIdle()
        
        // Verificar que se filtra la lista
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onAllNodesWithText("Abbey Road", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            try {
                composeTestRule.onNodeWithText("Abbey Road", substring = true)
                    .isDisplayed()
            } catch (e: Exception) {
                false
            }
        }
        
        // Capturar screenshot de la búsqueda
        screenshotTestRule.takeScreenshot("buscar-album")
    }
}

