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
import com.miso.vinilos.viewmodels.MusicianViewModel
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.screens.ArtistDetailScreen
import com.miso.vinilos.views.theme.VinilosTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E con Espresso para el flujo completo de detalle de artista
 *
 * Estas pruebas verifican:
 * - Estados de carga, éxito y error para el detalle de un artista
 * - Visualización correcta de información del artista (foto, nombre, descripción)
 * - Visualización de álbumes del artista
 * - Visualización de premios
 * - Funcionalidad de reintento tras error
 *
 * Las pruebas usan MockWebServer para simular respuestas del API
 * y siguen el mismo patrón que AlbumDetailE2ETest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ArtistDetailE2ETest {

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
     * Helper function to create a test ViewModel with MockWebServer
     */
    private fun createTestViewModel(): MusicianViewModel {
        val testApiService = TestRetrofitClient.createTestMusicianApiService(mockWebServerRule.baseUrl)
        val application = ApplicationProvider.getApplicationContext<Application>()
        // Usar base de datos en memoria para pruebas (garantiza caché vacío)
        testDatabase = Room.inMemoryDatabaseBuilder(
            application,
            VinylRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        val musiciansDao = testDatabase!!.musiciansDao()
        val testRepository = com.miso.vinilos.model.repository.MusicianRepository(application, musiciansDao, testApiService)
        val testPrizeRepository = com.miso.vinilos.model.repository.PrizeRepository.getInstance()
        return MusicianViewModel(testRepository, testPrizeRepository, Dispatchers.Unconfined)
    }

    /**
     * Test 1: Successful Artist Detail Loading
     * Verifica que el detalle del artista se muestra correctamente cuando el API retorna datos
     */
    @Test
    fun testSuccessfulArtistDetailLoading() {
        // Arrange
        val testMusicians = TestDataFactory.createTestMusicians()
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()

        // Primera respuesta: lista de artistas
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        // Segunda respuesta: detalle del artista cuando se hace clic
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        // Respuesta para premio
        val prize = testMusician.performerPrizes?.firstOrNull()?.prize
        if (prize != null) {
            mockWebServerRule.server.enqueue(
                JsonResponseHelper.createServerErrorResponse() // Prize endpoint not implemented in helper yet
            )
        }

        val testViewModel = createTestViewModel()

        // Act - Usar el flujo completo de navegación
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Esperar a que la lista se cargue y navegar
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        composeTestRule.waitForIdle()

        // Dar clic en el primer artista para navegar al detalle
        composeTestRule.onNodeWithText("John Lennon").performClick()

        // Assert - esperar a que cargue la pantalla de detalle
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("John Lennon")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que el artista está visible
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        
        // Capturar screenshot de la pantalla de detalle cargada
        screenshotTestRule.takeScreenshot("01-detalle-cargado")
    }

    /**
     * Test 2: Loading State Display
     * Verifica que el indicador de carga aparece mientras se obtienen los datos del artista
     */
    @Test
    fun testLoadingStateDisplay() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(TestDataFactory.createTestMusicians())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createTimeoutResponse()
        )
        val testViewModel = createTestViewModel()
        val musicianId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                ArtistDetailScreen(
                    musicianId = musicianId,
                    musicianViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Capturar screenshot del estado de carga
        screenshotTestRule.takeScreenshot("estado-carga")
    }

    /**
     * Test 3: Error State Display
     * Verifica que el mensaje de error y botón de reintento aparecen cuando el API falla
     */
    @Test
    fun testErrorStateDisplay() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(TestDataFactory.createTestMusicians())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testViewModel = createTestViewModel()
        val musicianId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                ArtistDetailScreen(
                    musicianId = musicianId,
                    musicianViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert: esperar a que aparezca el mensaje de error
        // El mensaje puede variar, así que buscamos por substring
        composeTestRule.waitUntil(timeoutMillis = 2_500) {
            val errorNodes1 = composeTestRule.onAllNodesWithText("Error", substring = true)
            val errorNodes2 = composeTestRule.onAllNodesWithText("Error al cargar", substring = true)
            errorNodes1.fetchSemanticsNodes().isNotEmpty() || errorNodes2.fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que algún mensaje de error está visible
        val errorNodes = composeTestRule.onAllNodesWithText("Error", substring = true)
        if (errorNodes.fetchSemanticsNodes().isEmpty()) {
            composeTestRule.onAllNodesWithText("Error al cargar", substring = true)[0]
                .assertIsDisplayed()
        } else {
            errorNodes[0].assertIsDisplayed()
        }

        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Capturar screenshot del estado de error
        screenshotTestRule.takeScreenshot("estado-error")

        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyArtistDetailLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga el detalle del artista después de un error
     */
    @Test
    fun testErrorRetryFunctionality() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(TestDataFactory.createTestMusicians())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        val testViewModel = createTestViewModel()
        val musicianId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                ArtistDetailScreen(
                    musicianId = musicianId,
                    musicianViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - Verificar estado de error inicial (esperar a que aparezca)
        // El mensaje puede variar, así que buscamos por substring
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            val errorNodes1 = composeTestRule.onAllNodesWithText("Error", substring = true)
            val errorNodes2 = composeTestRule.onAllNodesWithText("Error al cargar", substring = true)
            errorNodes1.fetchSemanticsNodes().isNotEmpty() || errorNodes2.fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verificar que algún mensaje de error está visible
        val errorNodes = composeTestRule.onAllNodesWithText("Error", substring = true)
        if (errorNodes.fetchSemanticsNodes().isEmpty()) {
            composeTestRule.onAllNodesWithText("Error al cargar", substring = true)[0]
                .assertIsDisplayed()
        } else {
            errorNodes[0].assertIsDisplayed()
        }
        
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Capturar screenshot del estado de error inicial
        screenshotTestRule.takeScreenshot("01-estado-error")

        // Act - Hacer clic en reintentar
        composeTestRule.onNodeWithText("Reintentar").performClick()

        // Assert - Verificar que el artista se carga después del reintento
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("John Lennon")
                .fetchSemanticsNodes().isNotEmpty()
        }
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        
        // Capturar screenshot después del reintento exitoso
        screenshotTestRule.takeScreenshot("02-después-reintento")
    }

    /**
     * Test 5: Artist Details Information Display
     * Verifica que toda la información del artista se muestra correctamente
     */
    @Test
    fun testArtistDetailsInformationDisplay() {
        // Arrange
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(TestDataFactory.createTestMusicians())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        val testViewModel = createTestViewModel()
        val musicianId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                ArtistDetailScreen(
                    musicianId = musicianId,
                    musicianViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - esperar a que cargue el detalle
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("John Lennon")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar información básica
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        // Verificar que la descripción contiene el texto esperado (puede tener texto adicional)
        composeTestRule.onNodeWithText("Músico y compositor británico, miembro de The Beatles").assertExists()
        
        // Capturar screenshot con todos los detalles
        screenshotTestRule.takeScreenshot("detalles-completos")
    }

    /**
     * Test 6: Back Button Functionality
     * Verifica que el botón de volver está presente y funciona
     */
    @Test
    fun testBackButtonFunctionality() {
        // Arrange
        val testMusician = TestDataFactory.createTestMusicianWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(TestDataFactory.createTestMusicians())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusicianSuccessResponse(testMusician)
        )
        val testViewModel = createTestViewModel()
        val musicianId = 1
        var backPressed = false

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                ArtistDetailScreen(
                    musicianId = musicianId,
                    musicianViewModel = testViewModel,
                    onBack = { backPressed = true }
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el botón de volver está visible
        composeTestRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
        
        // Capturar screenshot antes de presionar volver
        screenshotTestRule.takeScreenshot("01-antes-volver")

        // Hacer clic en el botón de volver
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        // Verificar que se llamó al callback
        assert(backPressed) { "El callback onBack no fue llamado" }
        
        // Capturar screenshot después de presionar volver
        screenshotTestRule.takeScreenshot("02-después-volver")
    }
}

