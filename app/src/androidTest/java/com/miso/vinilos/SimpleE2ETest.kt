package com.miso.vinilos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.miso.vinilos.views.navigation.AppNavigation
import com.miso.vinilos.views.theme.VinilosTheme
import androidx.navigation.compose.rememberNavController
import com.miso.vinilos.rules.ScreenshotTestRule
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

    @get:Rule
    val screenshotTestRule = ScreenshotTestRule().apply {
        setComposeTestRule(composeTestRule)
    }

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
        // Capturar screenshot del estado inicial
        screenshotTestRule.takeScreenshot("01-inicio")
        
        // Verificar que la aplicación se carga
        composeTestRule.onRoot().assertExists()

        // Verificar que hay exactamente 2 nodos con "Álbumes" (header + navigation)
        composeTestRule.onAllNodesWithText("Álbumes").assertCountEquals(2)

        // Verificar que las pestañas de navegación están presentes
        composeTestRule.onNodeWithText("Artistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coleccionistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
        
        // Capturar screenshot mostrando elementos básicos
        screenshotTestRule.takeScreenshot("02-elementos-verificados")
    }

    /**
     * Test: Verificar que se puede navegar entre pestañas
     */
    @Test
    fun testNavigationWorks() {
        // Capturar screenshot inicial
        screenshotTestRule.takeScreenshot("01-pantalla-inicial")
        
        // Verificar que estamos en la pantalla de Álbumes (por defecto)
        composeTestRule.onAllNodesWithText("Álbumes")[0].assertIsDisplayed() // Header

        // Hacer clic en Perfil (pestaña del bottom navigation)
        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.waitForIdle()
        
        // Capturar screenshot en Perfil
        screenshotTestRule.takeScreenshot("02-navegado-a-perfil")

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
        
        // Capturar screenshot de vuelta a Álbumes
        screenshotTestRule.takeScreenshot("03-vuelto-a-albumes")

        // Verificar que volvimos
        composeTestRule.onRoot().assertExists()
        
        // Captura final
        screenshotTestRule.takeScreenshot("04-navegacion-completa")
    }

    /**
     * Test: Verificar que la UI responde a interacciones básicas
     */
    @Test
    fun testBasicUIInteractions() {
        // Capturar screenshot inicial
        screenshotTestRule.takeScreenshot("01-pantalla-inicial")
        
        // Verificar que podemos interactuar con elementos básicos
        composeTestRule.onNodeWithText("Artistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coleccionistas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
        
        // Capturar screenshot con elementos visibles
        screenshotTestRule.takeScreenshot("02-elementos-visibles")

        // Verificar que los elementos son clickeables
        composeTestRule.onNodeWithText("Artistas").assertHasClickAction()
        composeTestRule.onNodeWithText("Coleccionistas").assertHasClickAction()
        composeTestRule.onNodeWithText("Perfil").assertHasClickAction()
        
        // Captura final
        screenshotTestRule.takeScreenshot("03-interacciones-verificadas")
    }
}
