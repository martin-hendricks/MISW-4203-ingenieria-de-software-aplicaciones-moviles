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
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.screens.AlbumDetailScreen
import com.miso.vinilos.views.theme.VinilosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E con Espresso para el flujo completo de detalle de álbum
 *
 * Estas pruebas verifican:
 * - Estados de carga, éxito y error para el detalle de un álbum
 * - Visualización correcta de información del álbum (cover, nombre, artistas)
 * - Visualización de detalles (descripción, género, sello, fecha)
 * - Visualización de lista de canciones con sus duraciones
 * - Visualización de comentarios con ratings
 * - Manejo de casos edge (álbumes sin tracks, sin comentarios)
 * - Funcionalidad de reintento tras error
 *
 * Las pruebas usan MockWebServer para simular respuestas del API
 * y siguen el mismo patrón que AlbumListE2ETest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AlbumDetailE2ETest {

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
        return AlbumViewModel(testRepository, Dispatchers.Unconfined)
    }

    /**
     * Test 1: Successful Album Detail Loading
     * Verifica que el detalle del álbum se muestra correctamente cuando el API retorna datos
     */
    @Test
    fun testSuccessfulAlbumDetailLoading() {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()

        // Primera respuesta: lista de álbumes
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // Segunda respuesta: detalle del álbum cuando se hace clic
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )

        val testViewModel = createTestViewModel()

        // Act - Usar el flujo completo de navegación
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel
                )
            }
        }

        // Esperar a que la lista se cargue
        composeTestRule.waitForIdle()

        // Dar clic en el primer álbum para navegar al detalle
        composeTestRule.onNodeWithText("Abbey Road").performClick()

        // Assert
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Esperar a que la navegación y carga se completen

        // Verificar que estamos en la pantalla de detalle buscando elementos únicos del detalle
        // El título "Detalles del Álbum" solo aparece en la pantalla de detalle
        composeTestRule.onNodeWithText("Detalles del Álbum").assertExists()

        // Verificar que el artista está visible
        composeTestRule.onNodeWithText("The Beatles", useUnmergedTree = true).assertExists()

        // Verificar la descripción
        composeTestRule.onNodeWithText("El último álbum grabado por The Beatles", useUnmergedTree = true, substring = true).assertExists()
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)

        // Verificar que la descripción está visible
        CustomMatchers.verifyAlbumDescriptionIsVisible(composeTestRule, "El último álbum grabado por The Beatles")
    }

    /**
     * Test 2: Loading State Display
     * Verifica que el indicador de carga aparece mientras se obtienen los datos del álbum
     */
    @Test
    fun testLoadingStateDisplay() {
        // Arrange - Configurar respuesta con delay para simular carga lenta
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createTimeoutResponse()
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - Verificar que el estado de carga es visible inicialmente
        CustomMatchers.verifyAlbumDetailLoadingTextIsVisible(composeTestRule)
    }

    /**
     * Test 3: Error State Display
     * Verifica que el mensaje de error y botón de reintento aparecen cuando el API falla
     */
    @Test
    fun testErrorStateDisplay() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyAlbumDetailErrorMessageIsVisible(composeTestRule)

        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)

        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyAlbumDetailLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga el detalle del álbum después de un error
     */
    @Test
    fun testErrorRetryFunctionality() {
        // Arrange - Primero error, luego éxito
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert - Verificar estado de error inicial
        composeTestRule.waitForIdle()
        CustomMatchers.verifyAlbumDetailErrorMessageIsVisible(composeTestRule)
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)

        // Act - Hacer clic en reintentar
        composeTestRule.onNodeWithText("Reintentar").performClick()

        // Assert - Verificar que el álbum se carga después del reintento
        composeTestRule.waitForIdle()
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
    }

    /**
     * Test 5: Album Details Information Display
     * Verifica que toda la información del álbum se muestra correctamente
     */
    @Test
    fun testAlbumDetailsInformationDisplay() {
        // Arrange
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar información básica
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        CustomMatchers.verifyAlbumDescriptionIsVisible(composeTestRule, "El último álbum grabado por The Beatles")

        // Verificar detalles específicos
        CustomMatchers.verifyGenreIsVisible(composeTestRule, "Rock")
        CustomMatchers.verifyRecordLabelIsVisible(composeTestRule, "Sony Music")
        CustomMatchers.verifyReleaseDateIsVisible(composeTestRule, "26/09/1969")
    }

    /**
     * Test 6: Track List Display
     * Verifica que la lista de canciones se muestra correctamente con nombres y duraciones
     */
    @Test
    fun testTrackListDisplay() {
        // Arrange
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que la sección de tracks está visible
        CustomMatchers.verifyTrackListSectionIsVisible(composeTestRule)

        // Verificar que los tracks están visibles
        CustomMatchers.verifyTrackIsVisible(composeTestRule, "Come Together")
        CustomMatchers.verifyTrackIsVisible(composeTestRule, "Something")
        CustomMatchers.verifyTrackIsVisible(composeTestRule, "Maxwell's Silver Hammer")

        // Verificar que las duraciones están visibles
        CustomMatchers.verifyTrackDurationIsVisible(composeTestRule, "4:20")
        CustomMatchers.verifyTrackDurationIsVisible(composeTestRule, "3:03")
        CustomMatchers.verifyTrackDurationIsVisible(composeTestRule, "3:27")
    }

    /**
     * Test 7: Comments Display
     * Verifica que los comentarios se muestran correctamente con ratings
     */
    @Test
    fun testCommentsDisplay() {
        // Arrange
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que la sección de comentarios está visible
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)

        // Verificar que los comentarios están visibles
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Excelente álbum, una obra maestra del rock")
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Muy bueno, aunque prefiero sus trabajos anteriores")
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Definitivamente uno de mis álbumes favoritos")

        // Verificar que los ratings están visibles
        CustomMatchers.verifyRatingIsVisible(composeTestRule, 5)
        CustomMatchers.verifyRatingIsVisible(composeTestRule, 4)
    }

    /**
     * Test 8: Album Without Tracks
     * Verifica que se muestra el mensaje apropiado cuando el álbum no tiene canciones
     */
    @Test
    fun testAlbumWithoutTracks() {
        // Arrange
        val albumWithoutTracks = TestDataFactory.createAlbumWithoutTracks()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(albumWithoutTracks)
        )
        val testViewModel = createTestViewModel()
        val albumId = 9

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Album Without Tracks")

        // Verificar que la sección de tracks está visible
        CustomMatchers.verifyTrackListSectionIsVisible(composeTestRule)

        // Verificar que se muestra el mensaje de "No hay canciones"
        CustomMatchers.verifyNoTracksMessageIsVisible(composeTestRule)
    }

    /**
     * Test 9: Album Without Comments
     * Verifica que se muestra el mensaje apropiado cuando el álbum no tiene comentarios
     */
    @Test
    fun testAlbumWithoutComments() {
        // Arrange
        val albumWithoutComments = TestDataFactory.createAlbumWithoutComments()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(albumWithoutComments)
        )
        val testViewModel = createTestViewModel()
        val albumId = 10

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Album Without Comments")

        // Verificar que la sección de comentarios está visible
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)

        // Verificar que se muestra el mensaje de "No hay comentarios"
        CustomMatchers.verifyNoCommentsMessageIsVisible(composeTestRule)
    }

    /**
     * Test 10: Album Without Performers (Unknown Artist)
     * Verifica que se muestra "Artista desconocido" cuando el álbum no tiene performers
     */
    @Test
    fun testAlbumWithoutPerformers() {
        // Arrange
        val albumWithoutPerformers = TestDataFactory.createAlbumWithoutPerformers()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(albumWithoutPerformers)
        )
        val testViewModel = createTestViewModel()
        val albumId = 6

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
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
     * Test 11: Not Found Error (404)
     * Verifica el comportamiento cuando se solicita un álbum que no existe
     */
    @Test
    fun testAlbumNotFoundError() {
        // Arrange
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createNotFoundResponse()
        )
        val testViewModel = createTestViewModel()
        val albumId = 999

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyAlbumDetailErrorMessageIsVisible(composeTestRule)

        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
    }

    /**
     * Test 12: Back Button Functionality
     * Verifica que el botón de volver está presente y funciona
     */
    @Test
    fun testBackButtonFunctionality() {
        // Arrange
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1
        var backPressed = false

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = { backPressed = true }
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el botón de volver está visible
        composeTestRule.onNodeWithContentDescription("Volver").assertIsDisplayed()

        // Hacer clic en el botón de volver
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        // Verificar que se llamó al callback
        assert(backPressed) { "El callback onBack no fue llamado" }
    }

    /**
     * Test 13: Scroll Functionality in Detail Screen
     * Verifica que se puede hacer scroll en la pantalla de detalle para ver todo el contenido
     */
    @Test
    fun testScrollFunctionalityInDetailScreen() {
        // Arrange
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar que el contenido superior está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumDetailsSectionIsVisible(composeTestRule)

        // Hacer scroll hacia abajo para ver los comentarios
        composeTestRule.onNodeWithText("Comentarios").performScrollTo()

        // Verificar que los comentarios ahora están visibles
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Excelente álbum")
    }

    /**
     * Test 14: Complete Album Information Display
     * Verifica que toda la información del álbum se muestra de manera integral
     */
    @Test
    fun testCompleteAlbumInformationDisplay() {
        // Arrange
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(testAlbum)
        )
        val testViewModel = createTestViewModel()
        val albumId = 1

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = testViewModel,
                    onBack = {}
                )
            }
        }

        // Assert
        composeTestRule.waitForIdle()

        // Verificar header
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")

        // Verificar detalles
        CustomMatchers.verifyAlbumDetailsSectionIsVisible(composeTestRule)
        CustomMatchers.verifyAlbumDescriptionIsVisible(composeTestRule, "El último álbum grabado")
        CustomMatchers.verifyGenreIsVisible(composeTestRule, "Rock")
        CustomMatchers.verifyRecordLabelIsVisible(composeTestRule, "Sony")

        // Hacer scroll para verificar tracks
        composeTestRule.onNodeWithText("Lista de Canciones").performScrollTo()
        CustomMatchers.verifyTrackListSectionIsVisible(composeTestRule)
        CustomMatchers.verifyTrackIsVisible(composeTestRule, "Come Together")

        // Hacer scroll para verificar comentarios
        composeTestRule.onNodeWithText("Comentarios").performScrollTo()
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Excelente álbum")
    }
}
