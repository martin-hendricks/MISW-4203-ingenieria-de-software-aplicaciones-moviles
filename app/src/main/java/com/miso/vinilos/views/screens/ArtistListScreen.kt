package com.miso.vinilos.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.views.components.VinilosListItem
import com.miso.vinilos.views.components.VinilosListView
import com.miso.vinilos.viewmodels.MusicianUiState
import com.miso.vinilos.viewmodels.MusicianViewModel

/**
 * Pantalla que muestra la lista de artistas (músicos)
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param musicianViewModel ViewModel que gestiona el estado de los músicos
 * @param onArtistClick Callback que se ejecuta cuando se hace clic en un artista
 */
@Composable
fun ArtistListScreen(
    musicianViewModel: MusicianViewModel,
    onArtistClick: (Musician) -> Unit
) {
    // Observa el estado de la UI desde el ViewModel
    val uiState by musicianViewModel.uiState.collectAsStateWithLifecycle()
    
    // Renderiza según el estado actual
    when (val currentState = uiState) {
        is MusicianUiState.Loading -> {
            LoadingState()
        }
        is MusicianUiState.Success -> {
            ArtistsList(
                musicians = currentState.musicians,
                onArtistClick = onArtistClick
            )
        }
        is MusicianUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onRetry = { musicianViewModel.refreshMusicians() }
            )
        }
    }
}

/**
 * Componente que muestra la lista de artistas usando VinilosListView
 * 
 * @param musicians Lista de músicos a mostrar
 * @param onArtistClick Callback cuando se selecciona un artista
 */
@Composable
private fun ArtistsList(
    musicians: List<Musician>,
    onArtistClick: (Musician) -> Unit
) {
    VinilosListView(
        title = "Artistas",
        items = musicians
    ) { musician ->
        VinilosListItem(
            imageUrl = musician.image,
            topLabel = musician.name,
            bottomLabel = getMusicianDescription(musician),
            isImageCircular = true,
            onClick = { onArtistClick(musician) }
        )
    }
}

/**
 * Componente que muestra el estado de carga
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Cargando artistas...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Componente que muestra el estado de error con opción de reintento
 * 
 * @param message Mensaje de error a mostrar
 * @param onRetry Callback para intentar cargar nuevamente
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Error al cargar artistas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

/**
 * Obtiene una descripción del músico para mostrar en la lista
 * Muestra el año de nacimiento si está disponible
 * 
 * @param musician Músico del cual obtener la descripción
 * @return Cadena con la descripción o información del músico
 */
private fun getMusicianDescription(musician: Musician): String {
    return if (musician.description.isNotEmpty()) {
        // Mostrar el año de nacimiento si está disponible
        val birthYear = musician.getBirthYear()
        if (birthYear > 0) {
            "Nacido en $birthYear"
        } else {
            musician.description.takeIf { it.length <= 50 } 
                ?: "${musician.description.take(47)}..."
        }
    } else {
        "Sin descripción"
    }
}

