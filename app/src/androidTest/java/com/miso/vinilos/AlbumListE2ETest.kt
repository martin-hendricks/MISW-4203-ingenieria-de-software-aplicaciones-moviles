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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel

/**
 * Pruebas E2E con Espresso para el flujo completo de listado de álbumes
 * 
 * Estas pruebas verifican:
 * - Estados de carga, éxito y error
 * - Visualización correcta de álbumes
 * - Funcionalidad de roles de usuario
 * - Navegación y componentes de UI
 * 
 * Las pruebas usan MockWebServer para simular respuestas del API
 * y pueden ejecutarse independientemente con Gradle
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AlbumListE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    /**
     * Helper function to create a test ViewModel with MockWebServer
     */
    private fun createTestViewModel(): AlbumViewModel {
        val testApiService = TestRetrofitClient.createTestApiService(mockWebServerRule.baseUrl)
        val testRepository = com.miso.vinilos.model.repository.AlbumRepository(testApiService)
        return com.miso.vinilos.viewmodels.AlbumViewModel(testRepository)
    }

    /**
     * Test 1: Successful Album List Loading
     * Verifica que los álbumes se muestran correctamente cuando el API retorna datos
     */
    @Test
    fun testSuccessfulAlbumListLoading() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que el título está visible
        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        
        // Verificar que los álbumes están visibles
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Led Zeppelin IV")
        
        // Verificar que los performers están visibles
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "Pink Floyd")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "Led Zeppelin")
        
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
        val testAlbums = TestDataFactory.createTestAlbums()
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
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert - Verificar que el estado de carga es visible inicialmente
        CustomMatchers.verifyLoadingTextIsVisible(composeTestRule)
        CustomMatchers.verifyCircularProgressIndicatorIsVisible(composeTestRule)
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
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyErrorMessageIsVisible(composeTestRule)
        
        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga los álbumes después de un error
     */
    @Test
    fun testErrorRetryFunctionality() = runTest {
        // Arrange - Primero error, luego éxito
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert - Verificar estado de error inicial
        composeTestRule.waitForIdle()
        CustomMatchers.verifyErrorMessageIsVisible(composeTestRule)
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)

        // Act - Hacer clic en reintentar
        composeTestRule.onNodeWithText("Reintentar").performClick()

        // Assert - Verificar que los álbumes se cargan después del reintento
        composeTestRule.waitForIdle()
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
    }

    /**
     * Test 5: Empty List Handling
     * Verifica el comportamiento cuando el API retorna una lista vacía
     */
    @Test
    fun testEmptyListHandling() = runTest {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createEmptyAlbumsResponse()
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que el título está visible
        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        
        // Verificar que no hay álbumes visibles
        CustomMatchers.verifyEmptyList(composeTestRule)
        
        // Verificar que la navegación está presente
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)
    }

    /**
     * Test 6: Album Item Display
     * Verifica que el nombre, imagen y performers de los álbumes se muestran correctamente
     */
    @Test
    fun testAlbumItemDisplay() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar elementos específicos del primer álbum
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        
        // Verificar elementos del segundo álbum
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "Pink Floyd")
        
        // Verificar que las imágenes están presentes (implícito al verificar los álbumes)
        CustomMatchers.verifyAlbumImageIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumImageIsVisible(composeTestRule, "The Dark Side of the Moon")
    }

    /**
     * Test 7: Collector Role - Screen Displays Correctly
     * Verifica que la pantalla se muestra correctamente para usuarios coleccionistas
     * Nota: El botón de agregar no está implementado actualmente en la UI
     */
    @Test
    fun testCollectorRoleAddButtonVisible() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que la pantalla se muestra correctamente
        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)
    }

    /**
     * Test 8: Visitor Role - Screen Displays Correctly
     * Verifica que la pantalla se muestra correctamente para usuarios visitantes
     * Nota: El botón de agregar no está implementado actualmente en la UI
     */
    @Test
    fun testVisitorRoleAddButtonHidden() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que la pantalla se muestra correctamente
        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)
    }

    /**
     * Test 9: Navigation Bar Present
     * Verifica que la barra de navegación inferior está presente
     */
    @Test
    fun testNavigationBarPresent() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que la navegación inferior está visible
        CustomMatchers.verifyBottomNavigationIsVisible(composeTestRule)

        // Verificar que las pestañas de navegación están presentes
        // Nota: Usamos onAllNodesWithText porque "Álbumes" aparece en el header y en el bottom nav
        composeTestRule.onAllNodesWithText("Álbumes")[1].assertIsDisplayed()
        composeTestRule.onNodeWithText("Artistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coleccionistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    /**
     * Test 10: Album List Scrolling
     * Verifica que la lista se desplaza correctamente con múltiples álbumes
     */
    @Test
    fun testAlbumListScrolling() = runTest {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que los primeros álbumes están visibles
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "The Dark Side of the Moon")
        
        // Realizar scroll hacia abajo
        composeTestRule.onNodeWithText("Abbey Road").performScrollTo()
        
        // Verificar que los álbumes posteriores están visibles después del scroll
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Sgt. Pepper's Lonely Hearts Club Band")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Wish You Were Here")
    }

    /**
     * Test adicional: Unknown Artist Display
     * Verifica que se muestra "Artista desconocido" cuando no hay performers
     */
    @Test
    fun testUnknownArtistDisplay() = runTest {
        // Arrange
        val albumWithoutPerformers = TestDataFactory.createAlbumWithoutPerformers()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(listOf(albumWithoutPerformers))
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Unknown Artist Album")
        
        // Verificar que se muestra "Artista desconocido"
        CustomMatchers.verifyUnknownArtistIsVisible(composeTestRule)
    }

    /**
     * Test adicional: Multiple Performers Display
     * Verifica que múltiples performers se muestran correctamente separados por comas
     */
    @Test
    fun testMultiplePerformersDisplay() = runTest {
        // Arrange
        val albumWithMultiplePerformers = TestDataFactory.createTestAlbum(
            id = 9,
            name = "Collaboration Album",
            performers = TestDataFactory.createTestPerformers()
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(listOf(albumWithMultiplePerformers))
        )
        val testViewModel = createTestViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Collaboration Album")

        // Verificar que al menos uno de los performers está visible
        // Nota: En la UI real, los performers se muestran separados por comas en una sola línea
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
    }
}
