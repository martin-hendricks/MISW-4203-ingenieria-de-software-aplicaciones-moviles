package com.miso.vinilos.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.UserRole
import com.miso.vinilos.views.components.VinilosListItem
import com.miso.vinilos.views.components.VinilosListView
import com.miso.vinilos.viewmodels.AlbumUiState
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel

/**
 * Pantalla que muestra la lista de álbumes
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param albumViewModel ViewModel que gestiona el estado de los álbumes
 * @param profileViewModel ViewModel que gestiona el perfil del usuario
 * @param onAlbumClick Callback que se ejecuta cuando se hace clic en un álbum
 */
@Composable
fun AlbumListScreen(
    albumViewModel: AlbumViewModel,
    profileViewModel: ProfileViewModel,
    onAlbumClick: (Album) -> Unit
) {
    // Observa el estado de la UI desde el ViewModel
    val uiState by albumViewModel.uiState.collectAsStateWithLifecycle()
    val userRole by profileViewModel.userRole.collectAsStateWithLifecycle()
    
    // Renderiza según el estado actual
    when (val currentState = uiState) {
        is AlbumUiState.Loading -> {
            LoadingState()
        }
        is AlbumUiState.Success -> {
            AlbumsList(
                albums = currentState.albums,
                userRole = userRole,
                onAlbumClick = onAlbumClick,
                onAddAlbum = {
                    // TODO: Navegar a pantalla de agregar álbum
                }
            )
        }
        is AlbumUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onRetry = if (currentState.canRetry) {
                    { albumViewModel.retryLoadAlbums() }
                } else {
                    { albumViewModel.refreshAlbums() }
                }
            )
        }
        is AlbumUiState.Empty -> {
            EmptyState(
                message = "No hay álbumes disponibles"
            )
        }
    }
}

/**
 * Componente que muestra la lista de álbumes usando VinilosListView
 * 
 * @param albums Lista de álbumes a mostrar
 * @param userRole Rol actual del usuario
 * @param onAlbumClick Callback cuando se selecciona un álbum
 * @param onAddAlbum Callback cuando se presiona el botón de agregar
 */
@Composable
private fun AlbumsList(
    albums: List<Album>,
    userRole: UserRole,
    onAlbumClick: (Album) -> Unit,
    onAddAlbum: () -> Unit
) {
    VinilosListView(
        title = "Álbumes",
        items = albums,
        // Solo muestra el botón de agregar si el usuario es coleccionista
        onPlusClick = if (userRole == UserRole.COLLECTOR) {
            { onAddAlbum() }
        } else {
            null
        }
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
                text = "Cargando",
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
 * Componente que muestra el estado vacío cuando no hay álbumes
 *
 * @param message Mensaje a mostrar
 */
@Composable
private fun EmptyState(
    message: String
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
                text = message,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Aún no se han agregado álbumes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

