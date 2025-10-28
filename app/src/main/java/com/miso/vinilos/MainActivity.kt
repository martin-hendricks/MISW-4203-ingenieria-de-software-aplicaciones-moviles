package com.miso.vinilos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.miso.vinilos.ui.navigation.AppNavigation
import com.miso.vinilos.ui.theme.VinilosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinilosTheme(dynamicColor = false) {  // Desactiva colores dinámicos
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}