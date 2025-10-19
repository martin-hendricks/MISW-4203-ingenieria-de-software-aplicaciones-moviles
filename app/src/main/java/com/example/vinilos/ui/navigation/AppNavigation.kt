package com.example.vinilos.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.vinilos.ui.components.BottomNavigationBar
import com.example.vinilos.ui.components.VinilosListView
import com.example.vinilos.ui.components.VinilosListItem

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

        /*        Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pantalla de Albumes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }*/
                // Lista simple para probar
                val items = listOf(
                    "Item 1",
                    "Item 2", 
                    "Item 3",
                    "Item 4",
                    "Item 5",
                    "Item 6",
                    "Item 7"
                )
                
                VinilosListView(
                    title = "Titulo Pantalla",
                    items = items,
                    onItemSelected = { item ->
                        println("Seleccionado: $item")
                    },
                    onPlusClick = {
                        // Aquí iría la navegación a crear álbum
                        println("Crear nuevo álbum")
                        // Ejemplo: navController.navigate("albums/create")
                    }
                ) { item ->
                    VinilosListItem(
                        imageUrl = "https://softmanagement.com.co/wp-content/uploads/2024/10/placeholder.png",
                        topLabel = item,
                        bottomLabel = "Subtítulo de $item",
                        isImageCircular = false,  // Imagen cuadrada para álbumes
                        onClick = { println("Click en $item") }
                    )
                }
            }

            // Pantalla de Artistas
            composable(NavigationRoutes.Artists.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pantalla de Artistas",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Pantalla de Coleccionistas
            composable(NavigationRoutes.Collectors.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pantalla de Coleccionistas",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary  // Text color on background
                    )
                }
            }
            
            // Pantalla de Perfil
            composable(NavigationRoutes.Profile.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pantalla de Perfil",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary  // Text color on background
                    )
                }
            }
        }
    }
}


