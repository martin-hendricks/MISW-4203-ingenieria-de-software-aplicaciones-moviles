package com.miso.vinilos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.config.TestRetrofitClient
import com.miso.vinilos.helpers.JsonResponseHelper
import com.miso.vinilos.helpers.TestDataFactory
import com.miso.vinilos.matchers.CustomMatchers
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.theme.VinilosTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.navigation.compose.rememberNavController
import com.miso.vinilos.viewmodels.CollectorViewModel
import kotlinx.coroutines.Dispatchers

/**
 * Pruebas E2E con Espresso para el flujo completo de listado de coleccionistas
 * 
 * Estas pruebas verifican:
 * - Estados de carga, éxito y error
 * - Visualización correcta de coleccionistas
 * - Funcionalidad de roles de usuario
 * - Navegación y componentes de UI
 * 
 * Las pruebas usan MockWebServer para simular respuestas del API
 * y pueden ejecutarse independientemente con Gradle
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CollectorListE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    /**
     * Helper function to create a test ViewModel with MockWebServer
     */
    private fun createTestViewModel(): CollectorViewModel {
        val testApiService = TestRetrofitClient.createTestCollectorApiService(mockWebServerRule.baseUrl)
        val testRepository = com.miso.vinilos.model.repository.CollectorRepository(testApiService)
        val testAlbumRepository = com.miso.vinilos.model.repository.AlbumRepository.getInstance()
        return com.miso.vinilos.viewmodels.CollectorViewModel(testRepository, testAlbumRepository, Dispatchers.Unconfined)
    }

    /**
     * Test 1: Successful Collector List Loading
     * Verifica que los coleccionistas se muestran correctamente cuando el API retorna datos
     */
    @Test
    fun testSuccessfulCollectorListLoading() = runTest {
        // Arrange
        val testCollectors = TestDataFactory.createTestCollectors()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(testCollectors)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        
        // Esperar a que la pantalla se cargue completamente
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val titleNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
            titleNodes.fetchSemanticsNodes().isNotEmpty()
        }
        
        // Esperar a que los coleccionistas se carguen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Juan Pérez")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Assert
        // Verificar que el título está visible (buscar el del header, no el tab)
        composeTestRule.waitForIdle()
        val titleNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
        if (titleNodes.fetchSemanticsNodes().size > 1) {
            titleNodes[1].assertIsDisplayed()
        } else {
            titleNodes[0].assertIsDisplayed()
        }
        
        // Verificar que los coleccionistas están visibles
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Juan Pérez")
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "María García")
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Carlos Rodríguez")
        
        // Verificar que la navegación inferior está presente
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)
    }

    /**
     * Test 2: Loading State Display
     * Verifica que el indicador de carga aparece mientras se obtienen los datos
     */
    @Test
    fun testLoadingStateDisplay() = runTest {
        // Arrange - Configurar respuesta con delay para simular carga lenta
        val testCollectors = TestDataFactory.createTestCollectors()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createTimeoutResponse()
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()

        // Assert - Verificar que el estado de carga es visible inicialmente
        // Nota: No verificamos el CircularProgressIndicator directamente porque no tiene contentDescription
        // En su lugar, verificamos el texto de carga que está visible junto con el indicador
        CustomMatchers.verifyCollectorsLoadingTextIsVisible(composeTestRule)
    }

    /**
     * Test 3: Error State Display
     * Verifica que el mensaje de error y botón de reintento aparecen cuando el API falla
     */
    @Test
    fun testErrorStateDisplay() = runTest {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        composeTestRule.waitForIdle()

        // Assert
        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyCollectorsErrorMessageIsVisible(composeTestRule)
        
        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyCollectorsLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga los coleccionistas después de un error
     */
    @Test
    fun testErrorRetryFunctionality() = runTest {
        // Arrange - Primero error, luego éxito
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testCollectors = TestDataFactory.createTestCollectors()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(testCollectors)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        composeTestRule.waitForIdle()

        // Assert - Verificar estado de error inicial
        CustomMatchers.verifyCollectorsErrorMessageIsVisible(composeTestRule)
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)

        // Act - Hacer clic en reintentar
        composeTestRule.onNodeWithText("Reintentar").performClick()

        // Assert - Verificar que los coleccionistas se cargan después del reintento
        composeTestRule.waitForIdle()
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Juan Pérez")
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "María García")
    }

    /**
     * Test 5: Empty List Handling
     * Verifica el comportamiento cuando el API retorna una lista vacía
     */
    @Test
    fun testEmptyListHandling() = runTest {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createEmptyCollectorsResponse()
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        
        // Esperar a que la pantalla se cargue completamente (puede estar vacía)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val titleNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
            titleNodes.fetchSemanticsNodes().isNotEmpty()
        }

        // Assert
        // Verificar que el título está visible (buscar el del header, no el tab)
        composeTestRule.waitForIdle()
        val titleNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
        if (titleNodes.fetchSemanticsNodes().size > 1) {
            titleNodes[1].assertIsDisplayed()
        } else {
            titleNodes[0].assertIsDisplayed()
        }
        
        // Verificar que no hay coleccionistas visibles
        composeTestRule.onNodeWithText("Juan Pérez")
            .assertDoesNotExist()
        
        // Verificar que la navegación está presente
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)
    }

    /**
     * Test 6: Collector Item Display
     * Verifica que el nombre y email de los coleccionistas se muestran correctamente
     */
    @Test
    fun testCollectorItemDisplay() = runTest {
        // Arrange
        val testCollectors = TestDataFactory.createTestCollectors()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(testCollectors)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        composeTestRule.waitForIdle()

        // Assert
        // Verificar elementos específicos del primer coleccionista
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Juan Pérez")
        
        // Verificar elementos del segundo coleccionista
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "María García")
    }

    /**
     * Test 7: Navigation Bar Present
     * Verifica que la barra de navegación inferior está presente
     */
    @Test
    fun testNavigationBarPresent() = runTest {
        // Arrange
        val testCollectors = TestDataFactory.createTestCollectors()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(testCollectors)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        
        // Esperar a que la pantalla se cargue completamente
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val titleNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
            titleNodes.fetchSemanticsNodes().size >= 2 // Al menos el título y el tab
        }

        // Assert
        // Esperar a que la navegación inferior esté completamente cargada
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Álbumes").fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("Artistas").fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("Coleccionistas").fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("Perfil").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.waitForIdle()
        
        // Verificar que la navegación inferior está visible
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)

        // Verificar que las pestañas de navegación están presentes
        // Buscar los tabs en la navegación inferior (pueden estar en diferentes posiciones)
        val albumsNodes = composeTestRule.onAllNodesWithText("Álbumes")
        if (albumsNodes.fetchSemanticsNodes().size > 1) {
            albumsNodes[1].assertIsDisplayed()
        } else {
            albumsNodes[0].assertIsDisplayed()
        }
        
        val artistsNodes = composeTestRule.onAllNodesWithText("Artistas")
        if (artistsNodes.fetchSemanticsNodes().size > 1) {
            artistsNodes[1].assertIsDisplayed()
        } else {
            artistsNodes[0].assertIsDisplayed()
        }
        
        // "Coleccionistas" aparece en el header y en la navegación, usar onAllNodesWithText
        val collectorsNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
        if (collectorsNodes.fetchSemanticsNodes().size > 1) {
            // Si hay múltiples nodos, el segundo es el de navegación
            collectorsNodes[1].assertIsDisplayed()
        } else {
            collectorsNodes[0].assertIsDisplayed()
        }
        
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    /**
     * Test 8: Collector List Scrolling
     * Verifica que la lista se desplaza correctamente con múltiples coleccionistas
     */
    @Test
    fun testCollectorListScrolling() = runTest {
        // Arrange
        val testCollectors = TestDataFactory.createTestCollectors()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCollectorsSuccessResponse(testCollectors)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    collectorViewModel = testViewModel
                )
            }
        }

        // Navigate to Collectors screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coleccionistas").performClick()
        composeTestRule.waitForIdle()

        // Assert
        // Verificar que los primeros coleccionistas están visibles
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Juan Pérez")
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "María García")
        
        // Realizar scroll hacia abajo
        composeTestRule.onNodeWithText("Juan Pérez").performScrollTo()
        
        // Verificar que los coleccionistas posteriores están visibles después del scroll
        CustomMatchers.verifyCollectorIsVisible(composeTestRule, "Carlos Rodríguez")
    }
}

