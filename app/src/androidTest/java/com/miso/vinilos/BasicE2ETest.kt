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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas E2E básicas para verificar que la infraestructura de testing funciona
 * 
 * Estas pruebas verifican elementos básicos de la UI sin depender del API
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BasicE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val screenshotTestRule = ScreenshotTestRule().apply {
        setComposeTestRule(composeTestRule)
    }

    /**
     * Test básico: Verificar que la aplicación se inicia correctamente
     */
    @Test
    fun testAppStartsSuccessfully() {
        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Capturar screenshot del estado inicial
        screenshotTestRule.takeScreenshot("01-inicio")
        
        // Verificar que la aplicación se carga (no hay crash)
        // Esto es un test básico para verificar que la infraestructura funciona
        composeTestRule.onRoot().assertExists()
        
        // Capturar screenshot final después de verificar
        screenshotTestRule.takeScreenshot("02-verificado")
    }

    /**
     * Test básico: Verificar que la navegación inferior está presente
     */
    @Test
    fun testBottomNavigationIsPresent() {
        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que las pestañas de navegación están presentes
        composeTestRule.onAllNodesWithText("Artistas").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Coleccionistas").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Perfil").assertCountEquals(1)

        composeTestRule.onAllNodesWithText("Álbumes").assertCountEquals(1) //  Navigation


        // Capturar screenshot mostrando la navegación
        screenshotTestRule.takeScreenshot("navegacion-visible")
    }

    /**
     * Test básico: Verificar que se puede navegar entre pestañas
     */
    @Test
    fun testNavigationBetweenTabs() {
        // Act
        composeTestRule.setContent {
            VinilosTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }

        // Assert
        composeTestRule.waitForIdle()
        
        // Verificar que estamos en la pestaña de Álbumes por defecto
        composeTestRule.onAllNodesWithText("Álbumes")[0].assertIsDisplayed() // Header
        
        // Capturar screenshot en pantalla inicial
        screenshotTestRule.takeScreenshot("01-pantalla-albumes")
        
        // Intentar hacer clic en la pestaña de Perfil
        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.waitForIdle()
        
        // Capturar screenshot después de navegar a Perfil
        screenshotTestRule.takeScreenshot("02-pantalla-perfil")
        
        // Verificar que la navegación funciona (no hay crash)
        composeTestRule.onRoot().assertExists()
        
        // Capturar screenshot final
        screenshotTestRule.takeScreenshot("03-navegacion-completada")
    }
}
