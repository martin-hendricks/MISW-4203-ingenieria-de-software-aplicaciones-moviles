package com.example.vinilos.ui.navigation

/**
 * Rutas de navegación de la aplicación
 * Cada objeto representa una pantalla o flujo de navegación
 */
sealed class NavigationRoutes(val route: String) {
    object Albums : NavigationRoutes("albums")
    object Artists : NavigationRoutes("artists")
    object Collectors : NavigationRoutes("collectors")
    object Profile : NavigationRoutes("profile")
}

