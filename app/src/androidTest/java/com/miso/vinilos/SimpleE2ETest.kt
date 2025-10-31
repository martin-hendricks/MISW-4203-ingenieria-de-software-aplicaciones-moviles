package com.miso.vinilos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.theme.VinilosTheme
import androidx.navigation.compose.rememberNavController
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E simplificadas que verifican elementos básicos de la UI
 * sin depender del estado del API
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SimpleE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        // Configurar el contenido antes de cada prueba
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Test: Verificar que la aplicación se inicia y muestra elementos básicos
     */
    @Test
    fun testAppStartsAndShowsBasicElements() {
        // Verificar que la aplicación se carga
        composeTestRule.onRoot().assertExists()

        // Esperar a que aparezcan las pestañas de navegación (esto indica que la UI está lista)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Álbumes").fetchSemanticsNodes().size >= 2
        }

        // Verificar que hay exactamente 2 nodos con "Álbumes" (header + navigation)
        composeTestRule.onAllNodesWithText("Álbumes").assertCountEquals(2)

        // Verificar que las pestañas de navegación están presentes
        composeTestRule.onNodeWithText("Artistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coleccionistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    /**
     * Test: Verificar que se puede navegar entre pestañas
     */
    @Test
    fun testNavigationWorks() {
        // Esperar a que la UI esté completamente cargada
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Álbumes").fetchSemanticsNodes().size >= 2
        }

        // Verificar que estamos en la pantalla de Álbumes (por defecto)
        composeTestRule.onAllNodesWithText("Álbumes")[0].assertIsDisplayed() // Header

        // Hacer clic en Perfil (pestaña del bottom navigation)
        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.waitForIdle()

        // Verificar que la navegación funciona (no hay crash)
        composeTestRule.onRoot().assertExists()

        // Volver a Álbumes haciendo clic en la última ocurrencia (bottom navigation)
        val albumNodes = composeTestRule.onAllNodesWithText("Álbumes").fetchSemanticsNodes()
        if (albumNodes.size > 1) {
            composeTestRule.onAllNodesWithText("Álbumes")[albumNodes.size - 1].performClick()
        } else {
            composeTestRule.onNodeWithText("Álbumes").performClick()
        }
        composeTestRule.waitForIdle()

        // Verificar que volvimos
        composeTestRule.onRoot().assertExists()
    }

    /**
     * Test: Verificar que la UI responde a interacciones básicas
     */
    @Test
    fun testBasicUIInteractions() {
        // Esperar a que la UI esté completamente cargada
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Álbumes").fetchSemanticsNodes().size >= 2
        }

        // Verificar que podemos interactuar con elementos básicos
        composeTestRule.onNodeWithText("Artistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coleccionistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()

        // Verificar que los elementos son clickeables
        composeTestRule.onNodeWithText("Artistas").assertHasClickAction()
        composeTestRule.onNodeWithText("Coleccionistas").assertHasClickAction()
        composeTestRule.onNodeWithText("Perfil").assertHasClickAction()
    }
}
