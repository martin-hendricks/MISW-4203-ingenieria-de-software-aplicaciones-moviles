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
import com.miso.vinilos.viewmodels.MusicianViewModel
import kotlinx.coroutines.Dispatchers

/**
 * Pruebas E2E con Espresso para el flujo completo de listado de artistas
 * 
 * Estas pruebas verifican:
 * - Estados de carga, éxito y error
 * - Visualización correcta de artistas
 * - Funcionalidad de roles de usuario
 * - Navegación y componentes de UI
 * 
 * Las pruebas usan MockWebServer para simular respuestas del API
 * y pueden ejecutarse independientemente con Gradle
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ArtistListE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    /**
     * Helper function to create a test ViewModel with MockWebServer
     */
    private fun createTestViewModel(): MusicianViewModel {
        val testApiService = TestRetrofitClient.createTestMusicianApiService(mockWebServerRule.baseUrl)
        val testRepository = com.miso.vinilos.model.repository.MusicianRepository(testApiService)
        val testPrizeRepository = com.miso.vinilos.model.repository.PrizeRepository.getInstance()
        return com.miso.vinilos.viewmodels.MusicianViewModel(testRepository, testPrizeRepository, Dispatchers.Unconfined)
    }

    /**
     * Test 1: Successful Artist List Loading
     * Verifica que los artistas se muestran correctamente cuando el API retorna datos
     */
    @Test
    fun testSuccessfulArtistListLoading() = runTest {
        // Arrange
        val testMusicians = TestDataFactory.createTestMusicians()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        
        // Esperar a que la pantalla se cargue completamente
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            // Verificar que el título está visible (puede ser el nodo 0 o 1 dependiendo de si hay tab)
            val titleNodes = composeTestRule.onAllNodesWithText("Artistas")
            titleNodes.fetchSemanticsNodes().isNotEmpty()
        }
        
        // Esperar a que los artistas se carguen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("John Lennon")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Assert
        // Verificar que el título está visible (buscar el del header, no el tab)
        composeTestRule.waitForIdle()
        val titleNodes = composeTestRule.onAllNodesWithText("Artistas")
        // El título del header debería estar en el primer nodo o segundo si hay tab
        if (titleNodes.fetchSemanticsNodes().size > 1) {
            // Si hay múltiples nodos, el primero suele ser el tab, el segundo el título
            titleNodes[1].assertIsDisplayed()
        } else {
            titleNodes[0].assertIsDisplayed()
        }
        
        // Verificar que los artistas están visibles
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "Paul McCartney")
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "David Gilmour")
        
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
        val testMusicians = TestDataFactory.createTestMusicians()
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
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()

        // Assert - Verificar que el estado de carga es visible inicialmente
        // Nota: No verificamos el CircularProgressIndicator directamente porque no tiene contentDescription
        // En su lugar, verificamos el texto de carga que está visible junto con el indicador
        CustomMatchers.verifyArtistsLoadingTextIsVisible(composeTestRule)
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
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        composeTestRule.waitForIdle()

        // Assert
        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyArtistsErrorMessageIsVisible(composeTestRule)
        
        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyArtistsLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga los artistas después de un error
     */
    @Test
    fun testErrorRetryFunctionality() = runTest {
        // Arrange - Primero error, luego éxito
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testMusicians = TestDataFactory.createTestMusicians()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        composeTestRule.waitForIdle()

        // Assert - Verificar estado de error inicial
        CustomMatchers.verifyArtistsErrorMessageIsVisible(composeTestRule)
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)

        // Act - Hacer clic en reintentar
        composeTestRule.onNodeWithText("Reintentar").performClick()

        // Assert - Verificar que los artistas se cargan después del reintento
        composeTestRule.waitForIdle()
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "Paul McCartney")
    }

    /**
     * Test 5: Empty List Handling
     * Verifica el comportamiento cuando el API retorna una lista vacía
     */
    @Test
    fun testEmptyListHandling() = runTest {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createEmptyMusiciansResponse()
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        
        // Esperar a que la pantalla se cargue completamente (puede estar vacía)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val titleNodes = composeTestRule.onAllNodesWithText("Artistas")
            titleNodes.fetchSemanticsNodes().isNotEmpty()
        }

        // Assert
        // Verificar que el título está visible (buscar el del header, no el tab)
        composeTestRule.waitForIdle()
        val titleNodes = composeTestRule.onAllNodesWithText("Artistas")
        if (titleNodes.fetchSemanticsNodes().size > 1) {
            titleNodes[1].assertIsDisplayed()
        } else {
            titleNodes[0].assertIsDisplayed()
        }
        
        // Verificar que no hay artistas visibles
        composeTestRule.onNodeWithText("John Lennon")
            .assertDoesNotExist()
        
        // Verificar que la navegación está presente
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)
    }

    /**
     * Test 6: Artist Item Display
     * Verifica que el nombre y descripción de los artistas se muestran correctamente
     */
    @Test
    fun testArtistItemDisplay() = runTest {
        // Arrange
        val testMusicians = TestDataFactory.createTestMusicians()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        composeTestRule.waitForIdle()

        // Assert
        // Verificar elementos específicos del primer artista
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        
        // Verificar elementos del segundo artista
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "Paul McCartney")
    }

    /**
     * Test 7: Navigation Bar Present
     * Verifica que la barra de navegación inferior está presente
     */
    @Test
    fun testNavigationBarPresent() = runTest {
        // Arrange
        val testMusicians = TestDataFactory.createTestMusicians()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        
        // Esperar a que la pantalla se cargue completamente
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val titleNodes = composeTestRule.onAllNodesWithText("Artistas")
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
        
        // "Coleccionistas" solo aparece en la navegación, pero por seguridad usamos onAllNodesWithText
        val collectorsNodes = composeTestRule.onAllNodesWithText("Coleccionistas")
        collectorsNodes[0].assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    /**
     * Test 8: Artist List Scrolling
     * Verifica que la lista se desplaza correctamente con múltiples artistas
     */
    @Test
    fun testArtistListScrolling() = runTest {
        // Arrange
        val testMusicians = TestDataFactory.createTestMusicians()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createMusiciansSuccessResponse(testMusicians)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    musicianViewModel = testViewModel
                )
            }
        }

        // Navigate to Artists screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artistas").performClick()
        composeTestRule.waitForIdle()

        // Assert
        // Verificar que los primeros artistas están visibles
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "John Lennon")
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "Paul McCartney")
        
        // Realizar scroll hacia abajo
        composeTestRule.onNodeWithText("John Lennon").performScrollTo()
        
        // Verificar que los artistas posteriores están visibles después del scroll
        CustomMatchers.verifyArtistIsVisible(composeTestRule, "David Gilmour")
    }
}

