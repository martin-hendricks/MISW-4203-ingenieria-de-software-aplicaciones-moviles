package com.example.vinilos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.vinilos.ui.navigation.BottomNavItem

/**
 * Barra de navegación inferior con las 4 opciones principales de la app
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val bottomNavItems = listOf(
        BottomNavItem.Albums,
        BottomNavItem.Artists,
        BottomNavItem.Collectors,
        BottomNavItem.Profile
    )

   Column(modifier = modifier) {

       HorizontalDivider(
           color = MaterialTheme.colorScheme.surfaceBright,
           thickness = 0.74.dp
       )
       
       NavigationBar(
           containerColor = MaterialTheme.colorScheme.surface,
           contentColor = MaterialTheme.colorScheme.secondary
       ) {
           bottomNavItems.forEach { item ->
               NavigationBarItem(
                   icon = {
                       Icon(
                           imageVector = item.icon,
                           contentDescription = item.title
                       )
                   },
                   label = { Text(
                       text = item.title,
                       style = MaterialTheme.typography.labelSmall
                   ) },
                   selected = currentDestination?.hierarchy?.any {
                       it.route == item.route
                   } == true,
                   onClick = {
                       navController.navigate(item.route) {
                           // Evitar múltiples copias de la misma pantalla
                           popUpTo(navController.graph.startDestinationId) {
                               saveState = true
                           }
                           // Reutilizar instancia si ya existe
                           launchSingleTop = true
                           // Restaurar estado al volver
                           restoreState = true
                       }
                   },
                   colors = NavigationBarItemDefaults.colors(
                       selectedIconColor = MaterialTheme.colorScheme.tertiary,
                       selectedTextColor = MaterialTheme.colorScheme.tertiary,
                       unselectedIconColor = MaterialTheme.colorScheme.secondary,
                       unselectedTextColor = MaterialTheme.colorScheme.secondary,
                       indicatorColor = Color.Transparent,
                   )
               )
           }
       }
   }
    

}



