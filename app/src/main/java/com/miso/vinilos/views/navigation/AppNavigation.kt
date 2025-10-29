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
import com.miso.vinilos.views.components.BottomNavigationBar
import com.miso.vinilos.views.screens.AlbumListScreen
import com.miso.vinilos.views.screens.ProfileScreen
import com.miso.vinilos.viewmodels.AlbumViewModel
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
                    profileViewModel = sharedProfileViewModel
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
                ProfileScreen(viewModel = sharedProfileViewModel)
            }
        }
    }
}


