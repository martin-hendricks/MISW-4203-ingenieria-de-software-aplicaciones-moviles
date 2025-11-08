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
import com.miso.vinilos.views.screens.ArtistListScreen
import com.miso.vinilos.views.screens.ArtistDetailScreen
import com.miso.vinilos.views.screens.CollectorListScreen
import com.miso.vinilos.views.screens.ProfileScreen
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.MusicianViewModel
import com.miso.vinilos.viewmodels.CollectorViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel

/**
 * Configura el NavHost con todas las rutas de navegación de la aplicación
 *
 * @param navController Controlador de navegación
 * @param albumViewModel ViewModel de álbumes (opcional, para testing)
 * @param profileViewModel ViewModel de perfil (opcional, para testing)
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    albumViewModel: AlbumViewModel? = null,
    profileViewModel: ProfileViewModel? = null
) {
    // ViewModels compartidos entre pantallas
    val sharedProfileViewModel = profileViewModel ?: viewModel()
    
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
                val sharedAlbumViewModel = albumViewModel ?: viewModel()
                AlbumListScreen(
                    albumViewModel = sharedAlbumViewModel,
                    profileViewModel = sharedProfileViewModel,
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
                // Usar el ViewModel compartido si existe, sino crear uno nuevo
                val sharedAlbumViewModel = albumViewModel ?: viewModel()
                AlbumDetailScreen(
                    albumId = albumId,
                    albumViewModel = sharedAlbumViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Pantalla de Artistas
            composable(NavigationRoutes.Artists.route) {
                val musicianViewModel: MusicianViewModel = viewModel()
                ArtistListScreen(
                    musicianViewModel = musicianViewModel,
                    onArtistClick = { musician ->
                        navController.navigate(NavigationRoutes.ArtistDetail.createRoute(musician.id))
                    }
                )
            }
            
            // Pantalla de Detalle de Artista
            composable(
                route = NavigationRoutes.ArtistDetail.route,
                arguments = listOf(
                    navArgument("musicianId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val musicianId = backStackEntry.arguments?.getInt("musicianId") ?: 0
                val musicianViewModel: MusicianViewModel = viewModel()
                ArtistDetailScreen(
                    musicianId = musicianId,
                    musicianViewModel = musicianViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Pantalla de Coleccionistas
            composable(NavigationRoutes.Collectors.route) {
                val collectorViewModel: CollectorViewModel = viewModel()
                CollectorListScreen(
                    collectorViewModel = collectorViewModel,
                    onCollectorClick = { collector ->
                        // TODO: Implementar navegación al detalle del coleccionista cuando se necesite
                        // navController.navigate(NavigationRoutes.CollectorDetail.createRoute(collector.id))
                    }
                )
            }
            
            // Pantalla de Perfil
            composable(NavigationRoutes.Profile.route) {
                ProfileScreen(viewModel = sharedProfileViewModel)
            }
        }
    }
}


