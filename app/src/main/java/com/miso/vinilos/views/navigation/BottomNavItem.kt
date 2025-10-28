package com.miso.vinilos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed  class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
    ) {
    object Albums: BottomNavItem(
        route = NavigationRoutes.Albums.route,
        title = "Álbumes",
        icon = Icons.Default.Album
    )
    object Artists: BottomNavItem(
        route = NavigationRoutes.Artists.route,
        title = "Artistas",
        icon = Icons.Default.People
    )
    object Collectors: BottomNavItem(
        route = NavigationRoutes.Collectors.route,
        title = "Coleccionistas",
        icon = Icons.Default.Mic
    )
    object Profile: BottomNavItem(
        route = NavigationRoutes.Profile.route,
        title = "Perfil",
        icon = Icons.Default.Person
    )
}