package com.miso.vinilos.views.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.miso.vinilos.views.components.BottomNavigationBar
import com.miso.vinilos.views.screens.AlbumListScreen
import com.miso.vinilos.views.screens.AlbumDetailScreen
import com.miso.vinilos.views.screens.ProfileScreen
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel

/**
 * Configura el NavHost con todas las rutas de navegación de la aplicación
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    // ViewModels compartidos entre pantallas
    val profileViewModel: ProfileViewModel = viewModel()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,  // Background for all screens
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.Albums.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Pantalla de Álbumes
            composable(NavigationRoutes.Albums.route) {
                val albumViewModel: AlbumViewModel = viewModel()
                AlbumListScreen(
                    albumViewModel = albumViewModel,
                    profileViewModel = profileViewModel,
                    onAlbumClick = { album ->
                        navController.navigate(NavigationRoutes.AlbumDetail.createRoute(album.id))
                    }
                )
            }

            // Pantalla de Detalle de Álbum
            composable(
                route = NavigationRoutes.AlbumDetail.route,
                arguments = listOf(
                    navArgument("albumId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getInt("albumId") ?: 0
                val albumViewModel: AlbumViewModel = viewModel()
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = albumViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Pantalla de Artistas
            composable(NavigationRoutes.Artists.route) {
                // TODO: Implementar pantalla de artistas
            }
            
            // Pantalla de Coleccionistas
            composable(NavigationRoutes.Collectors.route) {
                // TODO: Implementar pantalla de coleccionistas
            }
            
            // Pantalla de Perfil
            composable(NavigationRoutes.Profile.route) {
                ProfileScreen(viewModel = profileViewModel)
            }
        }
    }
}


