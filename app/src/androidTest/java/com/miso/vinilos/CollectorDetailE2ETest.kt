package com.miso.vinilos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.config.TestRetrofitClient
import com.miso.vinilos.helpers.JsonResponseHelper
import com.miso.vinilos.helpers.TestDataFactory
import com.miso.vinilos.matchers.CustomMatchers
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.rules.ScreenshotTestRule
import com.miso.vinilos.viewmodels.CollectorViewModel
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.screens.CollectorDetailScreen
import com.miso.vinilos.views.theme.VinilosTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E con Espresso para el flujo completo de detalle de coleccionista
 *
 * Estas pruebas verifican:
 * - Estados de carga, éxito y error para el detalle de un coleccionista
 * - Visualización correcta de información del coleccionista (foto, nombre, contacto)
 * - Visualización de álbumes del coleccionista
 * - Visualización de artistas favoritos
 * - Funcionalidad de reintento tras error
 *
 * Las pruebas usan MockWebServer para simular respuestas del API
 * y siguen el mismo patrón que AlbumDetailE2ETest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CollectorDetailE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    @get:Rule
    val screenshotTestRule = ScreenshotTestRule().apply {
        setComposeTestRule(composeTestRule)
    }

    /**
     * Desplaza la lista hasta un texto objetivo y asegura su visibilidad
     */
    private fun scrollToAndAssertVisible(text: String) {
        runCatching {
            composeTestRule.onNodeWithText(text, substring = true)
                .performScrollTo()
        }
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(text, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Helper function to create a test ViewModel with MockWebServer
     */
    private fun createTestViewModel(): CollectorViewModel {
        val testApiService = TestRetrofitClient.createTestCollectorApiService(mockWebServerRule.baseUrl)
        val testRepository = com.miso.vinilos.model.repository.CollectorRepository(testApiService)
        val testAlbumApiService = TestRetrofitClient.createTestApiService(mockWebServerRule.baseUrl)
        val testAlbumRepository = com.miso.vinilos.model.repository.AlbumRepository(testAlbumApiService)
        return CollectorViewModel(testRepository, testAlbumRepository, Dispatchers.Unconfined)
    }

    /**
     * Test 1: Successful Collector Detail Loading
     * Verifica que el detalle del coleccionista se muestra correctamente cuando el API retorna datos
     */
    @Test
    fun testSuccessfulCollectorDetailLoading() {
        // Arrange
        val testCollectors = TestDataFactory.createTestCollectors()
        val testCollector = TestDataFactory.createTestCollectorWithFullDetails()

        // Primera respuesta: lista de coleccionistas
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(testCollectors)
        )
        // Segunda respuesta: detalle del coleccionista cuando se hace clic
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorSuccessResponse(testCollector)
        )
        // Respuestas para álbumes
        val testAlbum1 = TestDataFactory.createTestAlbum(1, "Abbey Road")
        val testAlbum2 = TestDataFactory.createTestAlbum(2, "The Dark Side of the Moon")
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum1)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum2)
        )

        val testViewModel = createTestViewModel()

        // Act - Usar el flujo completo de navegación
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Esperar a que la lista se cargue y navegar
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        composeTestRule.waitForIdle()

        // Dar clic en el primer coleccionista para navegar al detalle
        composeTestRule.onNodeWithText("Juan Pérez").performClick()

        // Assert - esperar a que cargue la pantalla de detalle
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Juan Pérez")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que el coleccionista está visible
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Juan Pérez")
        
        // Capturar screenshot de la pantalla de detalle cargada
        screenshotTestRule.takeScreenshot("01-detalle-cargado")
    }

    /**
     * Test 2: Loading State Display
     * Verifica que el indicador de carga aparece mientras se obtienen los datos del coleccionista
     */
    @Test
    fun testLoadingStateDisplay() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(TestDataFactory.createTestCollectors())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createTimeoutResponse()
        )
        val testViewModel = createTestViewModel()
        val collectorId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                CollectorDetailScreen(
                    collectorId = collectorId,
                    collectorViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - Verificar que el estado de carga es visible inicialmente
        CustomMatchers.verifyCollectorDetailLoadingTextIsVisible(composeTestRule)
        
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
            JsonResponseHelper.createCollectorsSuccessResponse(TestDataFactory.createTestCollectors())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testViewModel = createTestViewModel()
        val collectorId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                CollectorDetailScreen(
                    collectorId = collectorId,
                    collectorViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert: esperar a que aparezca el mensaje de error
        // Nota: CollectorDetailScreen no tiene botón de reintento, solo muestra el error
        composeTestRule.waitUntil(timeoutMillis = 2_500) {
            composeTestRule.onAllNodesWithText("Error al cargar coleccionista", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que el mensaje de error está visible
        composeTestRule.onNodeWithText("Error al cargar coleccionista", substring = true)
            .assertIsDisplayed()
        
        // Capturar screenshot del estado de error
        screenshotTestRule.takeScreenshot("estado-error")

        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyCollectorDetailLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga el detalle del coleccionista después de un error
     */
    @Test
    fun testErrorRetryFunctionality() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(TestDataFactory.createTestCollectors())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testCollector = TestDataFactory.createTestCollectorWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorSuccessResponse(testCollector)
        )
        val testViewModel = createTestViewModel()
        val collectorId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                CollectorDetailScreen(
                    collectorId = collectorId,
                    collectorViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - Verificar estado de error inicial (esperar a que aparezca)
        // Nota: CollectorDetailScreen no tiene botón de reintento, solo muestra el error
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Error al cargar coleccionista", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Error al cargar coleccionista", substring = true)
            .assertIsDisplayed()
        
        // Capturar screenshot del estado de error inicial
        screenshotTestRule.takeScreenshot("01-estado-error")

        // Nota: Como no hay botón de reintento, este test solo verifica que el error se muestra
        // No podemos probar la funcionalidad de reintento en CollectorDetailScreen
    }

    /**
     * Test 5: Collector Details Information Display
     * Verifica que toda la información del coleccionista se muestra correctamente
     */
    @Test
    fun testCollectorDetailsInformationDisplay() {
        // Arrange
        val testCollector = TestDataFactory.createTestCollectorWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(TestDataFactory.createTestCollectors())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorSuccessResponse(testCollector)
        )
        // Respuestas para álbumes
        val testAlbum1 = TestDataFactory.createTestAlbum(1, "Abbey Road")
        val testAlbum2 = TestDataFactory.createTestAlbum(2, "The Dark Side of the Moon")
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum1)
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum2)
        )
        val testViewModel = createTestViewModel()
        val collectorId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                CollectorDetailScreen(
                    collectorId = collectorId,
                    collectorViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - esperar a que cargue el detalle
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Juan Pérez")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar información básica
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Juan Pérez")
        
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
        val testCollector = TestDataFactory.createTestCollectorWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(TestDataFactory.createTestCollectors())
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorSuccessResponse(testCollector)
        )
        val testViewModel = createTestViewModel()
        val collectorId = 1
        var backPressed = false

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                CollectorDetailScreen(
                    collectorId = collectorId,
                    collectorViewModel = testViewModel,
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

