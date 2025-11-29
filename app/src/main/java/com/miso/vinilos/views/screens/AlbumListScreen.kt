package com.miso.vinilos.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 * @param onAddAlbum Callback que se ejecuta cuando se presiona el botón de agregar
 */
@Composable
fun AlbumListScreen(
    albumViewModel: AlbumViewModel,
    profileViewModel: ProfileViewModel,
    onAlbumClick: (Album) -> Unit,
    onAddAlbum: () -> Unit = {}
) {
    // Observa el estado de la UI desde el ViewModel
    val uiState by albumViewModel.uiState.collectAsStateWithLifecycle()
    val userRole by profileViewModel.userRole.collectAsStateWithLifecycle()
    val createAlbumState by albumViewModel.createAlbumUiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Forzar refresh cuando la pantalla se vuelve visible (onResume)
    // Esto asegura que siempre se obtengan los datos más recientes del servidor
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Forzar un refresh completo contra el servicio cuando se vuelve a la pantalla
                albumViewModel.refreshAlbums()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Refrescar la lista cuando se detecta que se creó un álbum exitosamente
    LaunchedEffect(createAlbumState) {
        if (createAlbumState is com.miso.vinilos.viewmodels.CreateAlbumUiState.Success) {
            // Forzar un refresh completo de la lista para mostrar el nuevo álbum
            // Esperar un momento para asegurar que el servidor haya procesado el nuevo álbum
            kotlinx.coroutines.delay(500)
            albumViewModel.refreshAlbums()
            // Esperar a que el refresh se complete antes de limpiar el estado
            kotlinx.coroutines.delay(2000)
            albumViewModel.clearCreateAlbumState()
        }
    }
    
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
                onAddAlbum = onAddAlbum
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
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                // LiveRegion para que TalkBack anuncie el estado de carga automáticamente
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Cargando álbumes, por favor espere"
            },
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
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                // LiveRegion Assertive para que TalkBack anuncie errores inmediatamente
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "Error al cargar álbumes. $message"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // Ícono de error para no depender solo del color
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null, // Decorativo, el texto ya describe el error
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
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
            Button(
                onClick = onRetry,
                modifier = Modifier.semantics {
                    contentDescription = "Botón reintentar, toque dos veces para volver a cargar los álbumes"
                }
            ) {
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

