package com.example.vinilos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.vinilos.ui.navigation.AppNavigation
import com.example.vinilos.ui.theme.VinilosTheme

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