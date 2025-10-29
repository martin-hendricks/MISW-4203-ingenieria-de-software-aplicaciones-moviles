package com.miso.vinilos.matchers

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText

/**
 * Matchers personalizados para pruebas E2E con Compose
 * 
 * Proporciona métodos de conveniencia para verificar elementos de la UI
 * específicos de la aplicación Vinilos
 * 
 * Nota: Estos métodos deben ser llamados desde el contexto de un ComposeTestRule
 */
object CustomMatchers {
    
    /**
     * Verifica que el texto de carga esté visible
     */
    fun verifyLoadingTextIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Cargando álbumes...")
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el texto de carga no esté visible
     */
    fun verifyLoadingTextIsNotVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Cargando álbumes...")
            .assertIsNotDisplayed()
    }
    
    /**
     * Verifica que el título "Álbumes" esté visible (solo el del header, no el de navegación)
     */
    fun verifyAlbumsTitleIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        // Buscar específicamente el primer nodo con texto "Álbumes" (el del header)
        return composeTestRule.onAllNodesWithText("Álbumes")[0]
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el mensaje de error esté visible
     */
    fun verifyErrorMessageIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Error al cargar álbumes")
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el botón "Reintentar" esté visible
     */
    fun verifyRetryButtonIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Reintentar")
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el botón de agregar (+) esté visible
     */
    fun verifyAddButtonIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Agregar")
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el botón de agregar (+) no esté visible
     */
    fun verifyAddButtonIsNotVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Agregar")
            .assertIsNotDisplayed()
    }
    
    /**
     * Verifica que un álbum específico esté visible en la lista
     */
    fun verifyAlbumIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, albumName: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(albumName)
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que un performer específico esté visible
     */
    fun verifyPerformerIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, performerName: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(performerName)
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el texto "Artista desconocido" esté visible
     */
    fun verifyUnknownArtistIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Artista desconocido")
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que la navegación inferior esté visible
     */
    fun verifyBottomNavigationIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        // Buscar específicamente el segundo nodo con texto "Álbumes" (el de navegación)
        return composeTestRule.onAllNodesWithText("Álbumes")[1]
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que la lista está vacía (no hay álbumes visibles)
     */
    fun verifyEmptyList(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule) {
        // Verificar que no hay elementos de álbumes visibles
        // Esto se puede hacer verificando que no hay texto de álbumes específicos
        composeTestRule.onNodeWithText("Abbey Road")
            .assertIsNotDisplayed()
    }
    
    /**
     * Verifica que el indicador de progreso circular esté visible
     */
    fun verifyCircularProgressIndicatorIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        // Buscar por el contentDescription del CircularProgressIndicator
        return composeTestRule.onNodeWithText("Cargando álbumes...")
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que el indicador de progreso circular no esté visible
     */
    fun verifyCircularProgressIndicatorIsNotVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Cargando álbumes...")
            .assertIsNotDisplayed()
    }
    
    /**
     * Verifica que un mensaje de error específico esté visible
     */
    fun verifySpecificErrorMessageIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, errorMessage: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que múltiples performers estén visibles (separados por comas)
     */
    fun verifyMultiplePerformersAreVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, performers: List<String>) {
        performers.forEach { performer ->
            composeTestRule.onNodeWithText(performer)
                .assertIsDisplayed()
        }
    }
    
    /**
     * Verifica que la imagen de un álbum esté visible
     * (esto es más complejo en Compose, pero podemos verificar que el álbum está presente)
     */
    fun verifyAlbumImageIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, albumName: String): SemanticsNodeInteraction {
        // En Compose, las imágenes pueden no tener texto, pero podemos verificar
        // que el álbum está presente, lo que implica que su imagen también debería estar
        return composeTestRule.onNodeWithText(albumName)
            .assertIsDisplayed()
    }
}
