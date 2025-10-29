package com.miso.vinilos.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.views.components.VinilosListItem
import com.miso.vinilos.views.components.VinilosListView
import com.miso.vinilos.viewmodels.AlbumUiState
import com.miso.vinilos.viewmodels.AlbumViewModel

/**
 * Pantalla que muestra la lista de álbumes
 * Implementa el patrón MVVM con Jetpack Compose
 * 
 * @param viewModel ViewModel que gestiona el estado de la pantalla
 */
@Composable
fun AlbumListScreen(
    viewModel: AlbumViewModel
) {
    // Observa el estado de la UI desde el ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    // Renderiza según el estado actual
    when (val currentState = uiState) {
        is AlbumUiState.Loading -> {
            LoadingState()
        }
        is AlbumUiState.Success -> {
            AlbumsList(
                albums = currentState.albums,
                onRefresh = { viewModel.refreshAlbums() },
                onAlbumClick = { album ->
                    // TODO: Navegar al detalle del álbum en una iteración futura
                }
            )
        }
        is AlbumUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onRetry = { viewModel.refreshAlbums() }
            )
        }
    }
}

/**
 * Componente que muestra la lista de álbumes usando VinilosListView
 * 
 * @param albums Lista de álbumes a mostrar
 * @param onRefresh Callback para refrescar la lista
 * @param onAlbumClick Callback cuando se selecciona un álbum
 */
@Composable
private fun AlbumsList(
    albums: List<Album>,
    onRefresh: () -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    VinilosListView(
        title = "Álbumes",
        items = albums,
        onItemSelected = onAlbumClick
    ) { album ->
        VinilosListItem(
            imageUrl = album.cover,
            topLabel = album.name,
            bottomLabel = getPerformersString(album.performers),
            onClick = { onAlbumClick(album) }
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
                text = "Cargando álbumes...",
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
                text = "Error al cargar álbumes",
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
 * Obtiene una cadena con los nombres de los performers del álbum
 * Si no hay performers, retorna "Artista desconocido"
 * 
 * @param performers Lista opcional de performers
 * @return Cadena con los nombres de los performers separados por coma
 */
private fun getPerformersString(performers: List<com.miso.vinilos.model.data.Performer>?): String {
    return if (performers.isNullOrEmpty()) {
        "Artista desconocido"
    } else {
        performers.joinToString(", ") { it.name }
    }
}

