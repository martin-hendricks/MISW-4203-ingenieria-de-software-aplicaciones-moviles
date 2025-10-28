package com.miso.vinilos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.miso.vinilos.ui.components.BottomNavigationBar

/**
 * Configura el NavHost con todas las rutas de navegación de la aplicación
 */
@Composable
fun AppNavigation(navController: NavHostController) {
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
                
            }

            // Pantalla de Artistas
            composable(NavigationRoutes.Artists.route) {

            }
            
            // Pantalla de Coleccionistas
            composable(NavigationRoutes.Collectors.route) {

            }
            
            // Pantalla de Perfil
            composable(NavigationRoutes.Profile.route) {

            }
        }
    }
}


