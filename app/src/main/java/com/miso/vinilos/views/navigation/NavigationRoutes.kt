package com.miso.vinilos.views.navigation

/**
 * Rutas de navegación de la aplicación
 * Cada objeto representa una pantalla o flujo de navegación
 */
sealed class NavigationRoutes(val route: String) {
    object Albums : NavigationRoutes("albums")
    object AlbumDetail : NavigationRoutes("albums/{albumId}") {
        fun createRoute(albumId: Int) = "albums/$albumId"
    }
    object Artists : NavigationRoutes("artists")
    object Collectors : NavigationRoutes("collectors")
    object Profile : NavigationRoutes("profile")
}

