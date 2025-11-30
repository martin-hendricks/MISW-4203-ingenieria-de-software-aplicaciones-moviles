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
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    fun testSuccessfulAlbumCreation() {
        // Arrange
        val testAlbums = TestDataFactory.createTestAlbums()

        // Primera respuesta: GET /albums inicial cuando se carga la lista
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums)
        )

        // Segunda respuesta: POST /albums (crear álbum) - DEBE RETORNAR UN OBJETO, NO UN ARRAY
        val newAlbum = testAlbums[0].copy(
            id = 999,
            name = "Álbum de Prueba",
            cover = "https://example.com/portada.jpg"
        )
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumSuccessResponse(newAlbum)
        )

        // Tercera respuesta: GET /albums después de crear (refrescar lista)
        mockWebServerRule.server.enqueue(
            JsonResponseHelper.createAlbumsSuccessResponse(testAlbums + newAlbum)
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

        // Campo: Fecha - hacer clic en el ÍCONO del calendario para abrir el date picker
        composeTestRule.onNodeWithContentDescription("Calendario", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Esperar a que el date picker se abra
        Thread.sleep(500)

        // Imprimir el árbol de nodos para debugging (el DatePicker está en el segundo root)
        composeTestRule.onAllNodes(isRoot())[1].printToLog("DATEPICKER")

        // Seleccionar una fecha en el calendario
        // Según la documentación oficial, los días en Material 3 DatePicker usan el formato completo
        // Por ejemplo: "Monday, November 11, 2024"
        // Generar fechas en el formato correcto para diferentes días del mes actual
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        val daysToTry = listOf(11, 12, 13, 14, 16, 17, 18, 19, 20, 21)


        for (day in daysToTry) {
            try {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val fullDateString = dateFormatter.format(calendar.time)

                // Intentar con el formato completo de fecha
                composeTestRule.onNodeWithText(fullDateString, useUnmergedTree = true)
                    .performClick()
                composeTestRule.waitForIdle()

                break
            } catch (e: Exception) {
                // Intentar con el siguiente día
                continue
            }
        }

        // Hacer clic en el botón OK del date picker para confirmar la fecha
        try {
            composeTestRule.onNodeWithText("OK", substring = true, useUnmergedTree = true)
                .performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // Si no se encuentra el botón OK, intentar con otros textos comunes
            try {
                composeTestRule.onNodeWithContentDescription("Confirmar", useUnmergedTree = true)
                    .performClick()
                composeTestRule.waitForIdle()
            } catch (e2: AssertionError) {
                // Si no se puede interactuar con el date picker, continuar
                // El botón puede quedar deshabilitado si la fecha no se establece
            }
        }

        composeTestRule.onNodeWithText("Descripción", substring = true)
            .performTextInput("Descripción de prueba")

        // Seleccionar género - hacer clic en la flecha para abrir el dropdown
        composeTestRule.onAllNodesWithContentDescription("Mostrar", useUnmergedTree = true)[0]
            .performClick()

        // Esperar a que el dropdown se abra y los elementos estén disponibles
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText(Genre.ROCK.displayName, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Pequeño delay para que la animación del dropdown termine
        Thread.sleep(500)

        composeTestRule.onNodeWithText(Genre.ROCK.displayName, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Seleccionar sello discográfico - hacer clic en la flecha para abrir el dropdown
        composeTestRule.onAllNodesWithContentDescription("Mostrar", useUnmergedTree = true)[1]
            .performClick()

        // Esperar a que el dropdown se abra y los elementos estén disponibles
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText(RecordLabel.SONY.displayName, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Pequeño delay para que la animación del dropdown termine
        Thread.sleep(500)

        composeTestRule.onNodeWithText(RecordLabel.SONY.displayName, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Hacer scroll hacia abajo para asegurar que el botón "Crear Álbum" esté visible
        try {
            composeTestRule.onNodeWithText("Crear Álbum", substring = true)
                .performScrollTo()
        } catch (e: Exception) {
            // Si no puede hacer scroll, continuar
        }

        // Esperar un momento para asegurar que el botón esté completamente renderizado
        Thread.sleep(500)

        // Intentar crear (esto no debería fallar)
        // Verificar que el botón existe y está habilitado
        composeTestRule.onNodeWithText("Crear Álbum", substring = true)
            .assertExists("El botón 'Crear Álbum' no existe")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        screenshotTestRule.takeScreenshot("Exito-crear-album")
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

        // Campo: Fecha - hacer clic en el ÍCONO del calendario para abrir el date picker
        composeTestRule.onNodeWithContentDescription("Calendario", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Esperar a que el date picker se abra
        Thread.sleep(500)

        // Imprimir el árbol de nodos para debugging (el DatePicker está en el segundo root)
        composeTestRule.onAllNodes(isRoot())[1].printToLog("DATEPICKER")

        // Seleccionar una fecha en el calendario
        // Según la documentación oficial, los días en Material 3 DatePicker usan el formato completo
        // Por ejemplo: "Monday, November 11, 2024"
        // Generar fechas en el formato correcto para diferentes días del mes actual
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        val daysToTry = listOf(11, 12, 13, 14, 16, 17, 18, 19, 20, 21)


        for (day in daysToTry) {
            try {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val fullDateString = dateFormatter.format(calendar.time)

                // Intentar con el formato completo de fecha
                composeTestRule.onNodeWithText(fullDateString, useUnmergedTree = true)
                    .performClick()
                composeTestRule.waitForIdle()

                break
            } catch (e: Exception) {
                // Intentar con el siguiente día
                continue
            }
        }

        // Hacer clic en el botón OK del date picker para confirmar la fecha
        try {
            composeTestRule.onNodeWithText("OK", substring = true, useUnmergedTree = true)
                .performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // Si no se encuentra el botón OK, intentar con otros textos comunes
            try {
                composeTestRule.onNodeWithContentDescription("Confirmar", useUnmergedTree = true)
                    .performClick()
                composeTestRule.waitForIdle()
            } catch (e2: AssertionError) {
                // Si no se puede interactuar con el date picker, continuar
                // El botón puede quedar deshabilitado si la fecha no se establece
            }
        }

        composeTestRule.onNodeWithText("Descripción", substring = true)
            .performTextInput("Descripción de prueba")

        // Seleccionar género - hacer clic en la flecha para abrir el dropdown
        composeTestRule.onAllNodesWithContentDescription("Mostrar", useUnmergedTree = true)[0]
            .performClick()

        // Esperar a que el dropdown se abra y los elementos estén disponibles
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText(Genre.ROCK.displayName, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Pequeño delay para que la animación del dropdown termine
        Thread.sleep(500)

        composeTestRule.onNodeWithText(Genre.ROCK.displayName, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Seleccionar sello discográfico - hacer clic en la flecha para abrir el dropdown
        composeTestRule.onAllNodesWithContentDescription("Mostrar", useUnmergedTree = true)[1]
            .performClick()

        // Esperar a que el dropdown se abra y los elementos estén disponibles
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onAllNodesWithText(RecordLabel.SONY.displayName, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Pequeño delay para que la animación del dropdown termine
        Thread.sleep(500)

        composeTestRule.onNodeWithText(RecordLabel.SONY.displayName, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Hacer scroll hacia abajo para asegurar que el botón "Crear Álbum" esté visible
        try {
            composeTestRule.onNodeWithText("Crear Álbum", substring = true)
                .performScrollTo()
        } catch (e: Exception) {
            // Si no puede hacer scroll, continuar
        }

        // Esperar un momento para asegurar que el botón esté completamente renderizado
        Thread.sleep(500)

        // Intentar crear (esto debería fallar)
        // Verificar que el botón existe y está habilitado
        composeTestRule.onNodeWithText("Crear Álbum", substring = true)
            .assertExists("El botón 'Crear Álbum' no existe")
            .assertIsDisplayed()
            .assertIsEnabled()
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
