package com.miso.vinilos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.config.TestRetrofitClient
import com.miso.vinilos.helpers.JsonResponseHelper
import com.miso.vinilos.helpers.TestDataFactory
import com.miso.vinilos.matchers.CustomMatchers
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.rules.ScreenshotTestRule
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.screens.AlbumDetailScreen
import com.miso.vinilos.views.theme.VinilosTheme
import kotlinx.coroutines.Dispatchers
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
 * - Funcionalidad de reintredsfsdnto tras error
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

    @get:Rule
    val screenshotTestRule = ScreenshotTestRule().apply {
        setComposeTestRule(composeTestRule)
    }
    /**
     * Desplaza la lista hasta un texto objetivo y asegura su visibilidad
     */
    private fun scrollToAndAssertVisible(text: String) {
        // Intenta scrollear el contenedor si existe; si no, hace scroll directo al nodo
        val triedContainer = runCatching {
            composeTestRule.onNodeWithTag("albumDetailList")
                .performScrollToNode(hasText(text, substring = true))
        }
        if (triedContainer.isFailure) {
            runCatching {
                composeTestRule.onNodeWithText(text, substring = true)
                    .performScrollTo()
            }
        }
        // Espera a que el nodo esté presente y visible
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

        // Assert - esperar a que cargue la pantalla de detalle
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que estamos en la pantalla de detalle buscando elementos únicos del detalle
        // El título "Detalles del Álbum" solo aparece en la pantalla de detalle
        composeTestRule.onNodeWithText("Detalles del Álbum").assertExists()
        
        // Capturar screenshot de la pantalla de detalle cargada
        screenshotTestRule.takeScreenshot("01-detalle-cargado")

        // Verificar que el artista está visible
        composeTestRule.onNodeWithText("The Beatles", useUnmergedTree = true).assertExists()

        // Verificar la descripción
        composeTestRule.onNodeWithText("El último álbum grabado por The Beatles", useUnmergedTree = true, substring = true).assertExists()

        // Asegurar que la sección de comentarios esté a la vista antes de validar
        scrollToAndAssertVisible("Comentarios")
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)
        
        // Capturar screenshot mostrando la sección de comentarios
        screenshotTestRule.takeScreenshot("02-comentarios-visible")

        // Verificar que la descripción está visible
        CustomMatchers.verifyAlbumDescriptionIsVisible(composeTestRule, "El último álbum grabado por The Beatles")
    }

    /**
     * Test 2: Loading State Display
     * Verifica que el indicador de carga aparece mientras se obtienen los datos del álbum
     */
    @Test
    fun testLoadingStateDisplay() {
        // Arrange - Primero lista de álbumes, luego detalle con delay para simular carga lenta
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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
        
        // Capturar screenshot del estado de carga
        screenshotTestRule.takeScreenshot("estado-carga")
    }

    /**
     * Test 3: Error State Display
     * Verifica que el mensaje de error y botón de reintento aparecen cuando el API falla
     */
    @Test
    fun testErrorStateDisplay() {
        // Arrange - Primero lista de álbumes, luego error en detalle
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert: esperar a que aparezca el mensaje de error
        composeTestRule.waitUntil(timeoutMillis = 2_500) {
            composeTestRule.onAllNodesWithText("Error al cargar el álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyAlbumDetailErrorMessageIsVisible(composeTestRule)

        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Capturar screenshot del estado de error
        screenshotTestRule.takeScreenshot("estado-error")

        // Verificar que el estado de carga ya no está visible
        CustomMatchers.verifyAlbumDetailLoadingTextIsNotVisible(composeTestRule)
    }

    /**
     * Test 4: Error Retry Functionality
     * Verifica que el botón de reintento recarga el detalle del álbum después de un error
     */
    @Test
    fun testErrorRetryFunctionality() {
        // Arrange - Lista de álbumes, luego error en detalle y finalmente éxito
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - Verificar estado de error inicial (esperar a que aparezca)
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Error al cargar el álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }
        CustomMatchers.verifyAlbumDetailErrorMessageIsVisible(composeTestRule)
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Capturar screenshot del estado de error inicial
        screenshotTestRule.takeScreenshot("01-estado-error")

        // Act - Hacer clic en reintentar
        composeTestRule.onNodeWithText("Reintentar").performClick()

        // Assert - Verificar que el álbum se carga después del reintento
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        
        // Capturar screenshot después del reintento exitoso
        screenshotTestRule.takeScreenshot("02-después-reintento")
    }

    /**
     * Test 5: Album Details Information Display
     * Verifica que toda la información del álbum se muestra correctamente
     */
    @Test
    fun testAlbumDetailsInformationDisplay() {
        // Arrange - Lista de álbumes y luego detalle exitoso
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar información básica
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")
        CustomMatchers.verifyAlbumDescriptionIsVisible(composeTestRule, "El último álbum grabado por The Beatles")

        // Verificar detalles específicos
        CustomMatchers.verifyGenreIsVisible(composeTestRule, "Rock")
        CustomMatchers.verifyRecordLabelIsVisible(composeTestRule, "Sony Music")
        CustomMatchers.verifyReleaseDateIsVisible(composeTestRule, "26/09/1969")
        
        // Capturar screenshot con todos los detalles
        screenshotTestRule.takeScreenshot("detalles-completos")
    }

    /**
     * Test 6: Track List Display
     * Verifica que la lista de canciones se muestra correctamente con nombres y duraciones
     */
    @Test
    fun testTrackListDisplay() {
        // Arrange - Lista de álbumes y luego detalle exitoso
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle y desplazar a la sección
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }
        scrollToAndAssertVisible("Lista de Canciones")


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
        
        // Capturar screenshot de la lista de canciones
        screenshotTestRule.takeScreenshot("lista-canciones")
    }

    /**
     * Test 7: Comments Display
     * Verifica que los comentarios se muestran correctamente con ratings
     */
    @Test
    fun testCommentsDisplay() {
        // Arrange - Lista de álbumes y luego detalle exitoso
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Asegurar que la sección de comentarios esté a la vista
        scrollToAndAssertVisible("Comentarios")

        // Verificar que la sección de comentarios está visible
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)

        // Verificar que los comentarios están visibles
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Excelente álbum, una obra maestra del rock")
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Muy bueno, aunque prefiero sus trabajos anteriores")
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Definitivamente uno de mis álbumes favoritos")

        // Verificar que los ratings están visibles
        CustomMatchers.verifyRatingIsVisible(composeTestRule, 5)
        CustomMatchers.verifyRatingIsVisible(composeTestRule, 4)
        
        // Capturar screenshot de los comentarios
        screenshotTestRule.takeScreenshot("comentarios-completos")
    }

    /**
     * Test 8: Album Without Tracks
     * Verifica que se muestra el mensaje apropiado cuando el álbum no tiene canciones
     */
    @Test
    fun testAlbumWithoutTracks() {
        // Arrange - Lista de álbumes y luego detalle del álbum sin tracks
        val albumWithoutTracks = TestDataFactory.createAlbumWithoutTracks()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Asegurar que la sección de tracks esté a la vista
        scrollToAndAssertVisible("Lista de Canciones")

        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Album Without Tracks")

        // Verificar que la sección de tracks está visible
        CustomMatchers.verifyTrackListSectionIsVisible(composeTestRule)

        // Verificar que se muestra el mensaje de "No hay canciones"
        CustomMatchers.verifyNoTracksMessageIsVisible(composeTestRule)
        
        // Capturar screenshot sin canciones
        screenshotTestRule.takeScreenshot("sin-canciones")
    }

    /**
     * Test 9: Album Without Comments
     * Verifica que se muestra el mensaje apropiado cuando el álbum no tiene comentarios
     */
    @Test
    fun testAlbumWithoutComments() {
        // Arrange - Lista de álbumes y luego detalle del álbum sin comentarios
        val albumWithoutComments = TestDataFactory.createAlbumWithoutComments()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle y llevar comentarios a vista
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }
        scrollToAndAssertVisible("Comentarios")

        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Album Without Comments")

        // Verificar que la sección de comentarios está visible
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)

        // Verificar que se muestra el mensaje de "No hay comentarios"
        CustomMatchers.verifyNoCommentsMessageIsVisible(composeTestRule)
        
        // Capturar screenshot sin comentarios
        screenshotTestRule.takeScreenshot("sin-comentarios")
    }

    /**
     * Test 10: Album Without Performers (Unknown Artist)
     * Verifica que se muestra "Artista desconocido" cuando el álbum no tiene performers
     */
    @Test
    fun testAlbumWithoutPerformers() {
        // Arrange - Lista de álbumes y luego detalle del álbum sin performers
        val albumWithoutPerformers = TestDataFactory.createAlbumWithoutPerformers()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que el álbum está visible
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Unknown Artist Album")

        // Verificar que se muestra "Artista desconocido"
        CustomMatchers.verifyUnknownArtistIsVisible(composeTestRule)
        
        // Capturar screenshot con artista desconocido
        screenshotTestRule.takeScreenshot("artista-desconocido")
    }

    /**
     * Test 11: Not Found Error (404)
     * Verifica el comportamiento cuando se solicita un álbum que no existe
     */
    @Test
    fun testAlbumNotFoundError() {
        // Arrange - Lista de álbumes y luego 404 en detalle
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que aparezca el estado de error
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Error al cargar el álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verificar que el mensaje de error está visible
        CustomMatchers.verifyAlbumDetailErrorMessageIsVisible(composeTestRule)

        // Verificar que el botón de reintento está visible
        CustomMatchers.verifyRetryButtonIsVisible(composeTestRule)
        
        // Capturar screenshot del error 404
        screenshotTestRule.takeScreenshot("error-404")
    }

    /**
     * Test 12: Back Button Functionality
     * Verifica que el botón de volver está presente y funciona
     */
    @Test
    fun testBackButtonFunctionality() {
        // Arrange - Lista de álbumes y luego detalle exitoso
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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
        
        // Capturar screenshot antes de presionar volver
        screenshotTestRule.takeScreenshot("01-antes-volver")

        // Hacer clic en el botón de volver
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        // Verificar que se llamó al callback
        assert(backPressed) { "El callback onBack no fue llamado" }
        
        // Capturar screenshot después de presionar volver
        screenshotTestRule.takeScreenshot("02-después-volver")
    }

    /**
     * Test 13: Scroll Functionality in Detail Screen
     * Verifica que se puede hacer scroll en la pantalla de detalle para ver todo el contenido
     */
    @Test
    fun testScrollFunctionalityInDetailScreen() {
        // Arrange - Lista de álbumes y luego detalle exitoso
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Assert - esperar a que cargue el detalle y asegurar encabezado visible
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Detalles del Álbum")
                .fetchSemanticsNodes().isNotEmpty()
        }
        scrollToAndAssertVisible("Abbey Road")
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        CustomMatchers.verifyAlbumDetailsSectionIsVisible(composeTestRule)

        // Hacer scroll hacia abajo para ver los comentarios
        scrollToAndAssertVisible("Comentarios")

        // Verificar que los comentarios ahora están visibles
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Excelente álbum")
        
        // Capturar screenshot del scroll completo
        screenshotTestRule.takeScreenshot("scroll-completo")
    }

    /**
     * Test 14: Complete Album Information Display
     * Verifica que toda la información del álbum se muestra de manera integral
     */
    @Test
    fun testCompleteAlbumInformationDisplay() {
        // Arrange - Lista de álbumes y luego detalle exitoso
        val testAlbum = TestDataFactory.createTestAlbumWithFullDetails()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(TestDataFactory.createTestAlbums())
        )
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

        // Verificar header (forzar scroll para evitar falsos negativos)
        scrollToAndAssertVisible("Abbey Road")
        CustomMatchers.verifyPerformerIsVisible(composeTestRule, "The Beatles")

        // Verificar detalles
        scrollToAndAssertVisible("Detalles del Álbum")
        CustomMatchers.verifyAlbumDetailsSectionIsVisible(composeTestRule)
        CustomMatchers.verifyAlbumDescriptionIsVisible(composeTestRule, "El último álbum grabado")
        CustomMatchers.verifyGenreIsVisible(composeTestRule, "Rock")
        CustomMatchers.verifyRecordLabelIsVisible(composeTestRule, "Sony Music")

        // Hacer scroll para verificar tracks
        scrollToAndAssertVisible("Lista de Canciones")
        CustomMatchers.verifyTrackListSectionIsVisible(composeTestRule)
        CustomMatchers.verifyTrackIsVisible(composeTestRule, "Come Together")

        // Hacer scroll para verificar comentarios
        scrollToAndAssertVisible("Comentarios")
        CustomMatchers.verifyCommentsSectionIsVisible(composeTestRule)
        CustomMatchers.verifyCommentIsVisible(composeTestRule, "Excelente álbum")
        
        // Capturar screenshot con toda la información completa
        screenshotTestRule.takeScreenshot("información-completa")
    }
}
