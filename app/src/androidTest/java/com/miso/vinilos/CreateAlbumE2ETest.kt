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
import com.miso.vinilos.model.data.Genre
import com.miso.vinilos.model.data.RecordLabel
import com.miso.vinilos.model.data.UserRole
import com.miso.vinilos.rules.MockWebServerRule
import com.miso.vinilos.rules.ScreenshotTestRule
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.theme.VinilosTheme
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E para el flujo completo de creación de álbumes
 * 
 * Estas pruebas verifican:
 * - Navegación a la pantalla de crear álbum
 * - Llenado del formulario de creación
 * - Envío del formulario
 * - Verificación de éxito y actualización de la lista
 * - Manejo de errores
 * 
 * Las pruebas usan MockWebServer para simular respuestas del API
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateAlbumE2ETest {

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
    private fun createTestViewModel(): AlbumViewModel {
        val testApiService = TestRetrofitClient.createTestApiService(mockWebServerRule.baseUrl)
        val application = ApplicationProvider.getApplicationContext<Application>()
        // Usar base de datos en memoria para pruebas (garantiza caché vacío)
        testDatabase = Room.inMemoryDatabaseBuilder(
            application,
            VinylRoomDatabase::class.java
        ).allowMainThreadQueries().build()
        val albumsDao = testDatabase!!.albumsDao()
        val testRepository = com.miso.vinilos.model.repository.AlbumRepository(application, albumsDao, testApiService)
        return AlbumViewModel(testRepository, Dispatchers.Unconfined)
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
     * Test 1: Successful Album Creation
     * Verifica que se puede crear un álbum exitosamente
     */
    @Test
    fun testSuccessfulAlbumCreation() = runTest {
        // Arrange - Configurar respuestas del servidor mock
        val existingAlbums = TestDataFactory.createTestAlbums()
        val newAlbum = TestDataFactory.createTestAlbum(
            id = 10,
            name = "Nuevo Álbum",
            cover = "https://example.com/nuevo-album.jpg",
            description = "Descripción del nuevo álbum"
        )
        
        // IMPORTANTE: Encolar las respuestas ANTES de crear el ViewModel
        // porque el ViewModel carga automáticamente en init
        // 
        // Flujo esperado:
        // 1. GET /albums (init del ViewModel) -> primera respuesta
        // 2. POST /albums (crear álbum) -> segunda respuesta  
        // 3. GET /albums (refresh después de crear) -> tercera respuesta
        //
        // Primera respuesta: lista inicial de álbumes (GET /albums)
        // Esta se usa cuando el ViewModel se crea y llama a loadAlbums() en init
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(existingAlbums)
        )
        // Segunda respuesta: crear álbum (POST /albums) - retorna 201 Created
        // Esta se usa cuando se hace clic en el botón "Crear"
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createCreatedResponse(newAlbum)
        )
        // Tercera respuesta: lista actualizada después de crear (GET /albums para refresh)
        // Esta se llama desde refreshAlbumsSync() después de crear el álbum exitosamente
        val updatedAlbums = existingAlbums + newAlbum
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(updatedAlbums)
        )
        
        // Verificar que el MockWebServer está listo
        assert(mockWebServerRule.server.url("/") != null) { "MockWebServer no está listo" }
        
        // Crear los ViewModels DESPUÉS de encolar las respuestas
        // El ViewModel cargará automáticamente en init y usará la primera respuesta encolada
        val testViewModel = createTestViewModel()
        val profileViewModel = createTestProfileViewModel()
        
        // Esperar un poco para que el init del ViewModel inicie la carga
        kotlinx.coroutines.delay(100)

        // Act - Configurar la UI UNA SOLA VEZ con todos los ViewModels necesarios
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }

        // Assert - Seguir EXACTAMENTE el mismo patrón que AlbumListE2ETest.testSuccessfulAlbumListLoading
        composeTestRule.waitForIdle()
        
        // Esperar a que la carga inicial de álbumes se complete antes de continuar
        // Esto asegura que la primera respuesta GET /albums se haya procesado correctamente
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onAllNodesWithText("Abbey Road", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        
        // Capturar screenshot del estado inicial después de cargar
        screenshotTestRule.takeScreenshot("01-lista-cargada")
        
        // Verificar que el título está visible (igual que en AlbumListE2ETest)
        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        
        // Verificar que los álbumes están visibles (igual que en AlbumListE2ETest)
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        
        // Capturar screenshot después de verificar los álbumes
        screenshotTestRule.takeScreenshot("02-albumes-verificados")
        
        // Esperar un poco para que el rol se propague a través de DataStore
        // Esto es necesario porque DataStore puede tardar en actualizar el StateFlow
        kotlinx.coroutines.delay(1500)
        composeTestRule.waitForIdle()
        
        // Buscar y hacer clic en el botón de agregar (solo visible para coleccionistas)
        // El botón tiene contentDescription "Agregar"
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onAllNodesWithContentDescription("Agregar")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithContentDescription("Agregar")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Capturar screenshot después de navegar a crear álbum
        screenshotTestRule.takeScreenshot("02-pantalla-crear")
        
        // Verificar que estamos en la pantalla de crear álbum
        composeTestRule.onNodeWithText("Crear Nuevo Álbum", substring = true)
            .assertIsDisplayed()
        
        // Llenar el formulario
        // Campo: Nombre
        composeTestRule.onNodeWithText("Nombre del Álbum", substring = true)
            .performTextInput("Nuevo Álbum")
        
        // Campo: Portada
        composeTestRule.onNodeWithText("URL de la Portada", substring = true)
            .performTextInput("https://example.com/nuevo-album.jpg")
        
        // Campo: Fecha - hacer clic en el campo de fecha para abrir el date picker
        composeTestRule.onNodeWithText("Fecha de Lanzamiento", substring = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Intentar interactuar con el date picker dialog
        // El date picker puede aparecer como un Dialog
        // Intentar encontrar y confirmar la fecha seleccionada
        // Nota: La interacción exacta puede variar según la implementación del date picker
        try {
            // Buscar el botón de confirmar/OK en el date picker
            composeTestRule.onNodeWithText("OK", substring = true, useUnmergedTree = true)
                .performClick()
        } catch (e: AssertionError) {
            // Si no se encuentra el botón OK, intentar con otros textos comunes
            try {
                composeTestRule.onNodeWithContentDescription("Confirmar", useUnmergedTree = true)
                    .performClick()
            } catch (e2: AssertionError) {
                // Si no se puede interactuar con el date picker, el test puede fallar
                // En ese caso, se necesitaría ajustar la interacción según la implementación real
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Campo: Descripción
        composeTestRule.onNodeWithText("Descripción", substring = true)
            .performTextInput("Descripción del nuevo álbum")
        
        // Campo: Género - hacer clic para abrir el dropdown
        composeTestRule.onNodeWithText("Género", substring = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Seleccionar un género (ROCK)
        composeTestRule.onNodeWithText(Genre.ROCK.displayName)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Campo: Sello Discográfico - hacer clic para abrir el dropdown
        composeTestRule.onNodeWithText("Sello Discográfico", substring = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Seleccionar un sello discográfico (SONY)
        composeTestRule.onNodeWithText(RecordLabel.SONY.displayName)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Capturar screenshot del formulario lleno
        screenshotTestRule.takeScreenshot("03-formulario-llenado")
        
        // Hacer clic en el botón de crear
        composeTestRule.onNodeWithText("Crear", substring = true)
            .assertIsDisplayed()
            .performClick()
        
        // Esperar a que se complete la creación
        // El ViewModel hace: POST para crear -> luego GET para refresh
        // Necesitamos esperar a que ambas operaciones se completen
        composeTestRule.waitForIdle()
        
        // Esperar un poco más para que el refresh se complete
        // El refreshAlbumsSync() se ejecuta después del POST exitoso
        kotlinx.coroutines.delay(1000)
        composeTestRule.waitForIdle()
        
        // Verificar que volvimos a la lista de álbumes
        // Esperar a que el nuevo álbum aparezca en la lista después del refresh
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onAllNodesWithText("Nuevo Álbum")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        
        // Verificar que el nuevo álbum está en la lista
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Nuevo Álbum")
        
        // Capturar screenshot final con el álbum creado
        screenshotTestRule.takeScreenshot("04-album-creado")
    }

    /**
     * Test 2: Create Album Error Handling
     * Verifica que se manejan correctamente los errores al crear un álbum
     */
    @Test
    fun testCreateAlbumErrorHandling() {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )
        // Respuesta de error al crear
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createServerErrorResponse()
        )
        val testViewModel = createTestViewModel()
        val profileViewModel = createTestProfileViewModel()

        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    albumViewModel = testViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        
        // Esperar a que los álbumes se carguen completamente antes de verificar
        // Seguir el mismo patrón que el test principal
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText("Abbey Road", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        
        // Verificar que los álbumes se cargaron correctamente
        CustomMatchers.verifyAlbumsTitleIsVisible(composeTestRule)
        CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
        
        // Esperar un poco para que el rol se propague a través de DataStore
        Thread.sleep(1500)
        composeTestRule.waitForIdle()
        
        // Navegar a crear álbum - esperar a que el botón esté visible
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithContentDescription("Agregar")
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        composeTestRule.onNodeWithContentDescription("Agregar")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Llenar el formulario con datos válidos
        composeTestRule.onNodeWithText("Nombre del Álbum", substring = true)
            .performTextInput("Álbum de Prueba")
        
        composeTestRule.onNodeWithText("URL de la Portada", substring = true)
            .performTextInput("https://example.com/portada.jpg")
        
        composeTestRule.onNodeWithText("Descripción", substring = true)
            .performTextInput("Descripción de prueba")
        
        // Seleccionar género
        composeTestRule.onNodeWithText("Género", substring = true)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(Genre.ROCK.displayName)
            .performClick()
        composeTestRule.waitForIdle()
        
        // Seleccionar sello discográfico
        composeTestRule.onNodeWithText("Sello Discográfico", substring = true)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(RecordLabel.SONY.displayName)
            .performClick()
        composeTestRule.waitForIdle()
        
        // Intentar crear (esto debería fallar)
        composeTestRule.onNodeWithText("Crear", substring = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verificar que se muestra un mensaje de error
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Error", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Capturar screenshot del error
        screenshotTestRule.takeScreenshot("error-crear-album")
    }
}
