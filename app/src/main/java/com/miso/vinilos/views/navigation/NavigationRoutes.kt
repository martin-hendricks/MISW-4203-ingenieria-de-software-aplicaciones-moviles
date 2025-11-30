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
    object CreateAlbum : NavigationRoutes("albums/create")
    object Artists : NavigationRoutes("artists")
    object ArtistDetail : NavigationRoutes("artists/{musicianId}") {
        fun createRoute(musicianId: Int) = "artists/$musicianId"
    }
    object SelectAlbumToArtist : NavigationRoutes("artists/{musicianId}/select-album") {
        fun createRoute(musicianId: Int) = "artists/$musicianId/select-album"
    }
    object Collectors : NavigationRoutes("collectors")
    object CollectorDetail : NavigationRoutes("collectors/{collectorId}") {
        fun createRoute(collectorId: Int) = "collectors/$collectorId"
    }
    object Profile : NavigationRoutes("profile")
}

