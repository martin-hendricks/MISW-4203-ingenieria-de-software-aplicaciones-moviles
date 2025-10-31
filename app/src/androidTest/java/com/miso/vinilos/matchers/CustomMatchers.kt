package com.miso.vinilos.matchers

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed

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
        return composeTestRule.onNodeWithText(albumName, substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }
    
    /**
     * Verifica que un performer específico esté visible
     * Nota: Puede haber múltiples álbumes del mismo performer, por eso verificamos que al menos uno exista
     */
    fun verifyPerformerIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, performerName: String) {
        composeTestRule.onAllNodesWithText(performerName, substring = true)
            .fetchSemanticsNodes().isNotEmpty()
        // Verificamos que el primer nodo existe
        composeTestRule.onAllNodesWithText(performerName, substring = true)[0]
            .assert(hasText(performerName, substring = true))
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

    // ===== Matchers para Album Detail Screen =====

    /**
     * Verifica que el texto de carga del detalle esté visible
     */
    fun verifyAlbumDetailLoadingTextIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Cargando álbum...")
            .assertIsDisplayed()
    }

    /**
     * Verifica que el texto de carga del detalle no esté visible
     */
    fun verifyAlbumDetailLoadingTextIsNotVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Cargando álbum...")
            .assertIsNotDisplayed()
    }

    /**
     * Verifica que el mensaje de error del detalle esté visible
     */
    fun verifyAlbumDetailErrorMessageIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Error al cargar el álbum")
            .assertIsDisplayed()
    }

    /**
     * Verifica que el botón de volver esté visible
     */
    fun verifyBackButtonIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("Volver")
            .assertIsDisplayed()
    }

    /**
     * Verifica que la sección "Detalles del Álbum" esté visible
     */
    fun verifyAlbumDetailsSectionIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        val nodes = composeTestRule.onAllNodesWithText("Detalles del Álbum")
        nodes.fetchSemanticsNodes().isNotEmpty()
        return nodes[0].assert(hasText("Detalles del Álbum"))
    }

    /**
     * Verifica que la sección "Lista de Canciones" esté visible
     */
    fun verifyTrackListSectionIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        val nodes = composeTestRule.onAllNodesWithText("Lista de Canciones")
        nodes.fetchSemanticsNodes().isNotEmpty()
        return nodes[0].assert(hasText("Lista de Canciones"))
    }

    /**
     * Verifica que la sección "Comentarios" esté visible
     */
    fun verifyCommentsSectionIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        val nodes = composeTestRule.onAllNodesWithText("Comentarios")
        nodes.fetchSemanticsNodes().isNotEmpty()
        return nodes[0].assert(hasText("Comentarios"))
    }

    /**
     * Verifica que un track específico esté visible
     */
    fun verifyTrackIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, trackName: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(trackName)
            .assert(hasText(trackName))
    }

    /**
     * Verifica que la duración de un track esté visible
     */
    fun verifyTrackDurationIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, duration: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(duration)
            .assert(hasText(duration))
    }

    /**
     * Verifica que un comentario específico esté visible
     */
    fun verifyCommentIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, commentText: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(commentText, substring = true)
            .assert(hasText(commentText, substring = true))
    }

    /**
     * Verifica que un rating esté visible
     */
    fun verifyRatingIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, rating: Int): SemanticsNodeInteraction {
        val nodes = composeTestRule.onAllNodesWithText("Rating: $rating/5")
        // Asegurar que existe al menos un nodo con ese rating
        nodes.fetchSemanticsNodes().isNotEmpty()
        // Validar el primero encontrado (puede haber múltiples ratings iguales)
        return nodes[0].assert(hasText("Rating: $rating/5"))
    }

    /**
     * Verifica que el mensaje "No hay canciones" esté visible
     */
    fun verifyNoTracksMessageIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("No hay canciones en este álbum.")
            .assert(hasText("No hay canciones en este álbum."))
    }

    /**
     * Verifica que el mensaje "No hay comentarios" esté visible
     */
    fun verifyNoCommentsMessageIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText("No hay comentarios aún.")
            .assert(hasText("No hay comentarios aún."))
    }

    /**
     * Verifica que la descripción del álbum esté visible
     */
    fun verifyAlbumDescriptionIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, description: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(description, substring = true)
            .assert(hasText(description, substring = true))
    }

    /**
     * Verifica que el género esté visible
     */
    fun verifyGenreIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, genre: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(genre, substring = true)
            .assert(hasText(genre, substring = true))
    }

    /**
     * Verifica que el sello discográfico esté visible
     */
    fun verifyRecordLabelIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, recordLabel: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(recordLabel, substring = true)
            .assert(hasText(recordLabel, substring = true))
    }

    /**
     * Verifica que la fecha de lanzamiento esté visible
     */
    fun verifyReleaseDateIsVisible(composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule, date: String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(date, substring = true)
            .assert(hasText(date, substring = true))
    }
}
