package com.miso.vinilos.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.network.NetworkConstants
import com.miso.vinilos.utils.ImageUrlHelper
import com.miso.vinilos.viewmodels.AlbumUiState
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.AddAlbumToMusicianUiState
import com.miso.vinilos.viewmodels.MusicianViewModel
import com.miso.vinilos.views.theme.DarkGreen
import com.miso.vinilos.views.theme.MidGreen
import com.miso.vinilos.views.theme.Yellow

/**
 * Pantalla para seleccionar un álbum y agregarlo a un artista
 * 
 * @param musicianId ID del músico al que se agregará el álbum
 * @param albumViewModel ViewModel que gestiona el estado de los álbumes
 * @param musicianViewModel ViewModel que gestiona el estado del músico
 * @param onBack Callback que se ejecuta cuando se cancela o completa la operación
 */
@Composable
fun SelectAlbumToArtistScreen(
    musicianId: Int,
    albumViewModel: AlbumViewModel,
    musicianViewModel: MusicianViewModel,
    onBack: () -> Unit
) {
    val uiState by albumViewModel.uiState.collectAsStateWithLifecycle()
    val addAlbumState by musicianViewModel.addAlbumToMusicianState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    
    // Observar el estado de agregar álbum
    LaunchedEffect(addAlbumState) {
        if (addAlbumState is AddAlbumToMusicianUiState.Success) {
            // Esperar un momento para que el servidor procese
            kotlinx.coroutines.delay(500)
            // Limpiar el estado antes de navegar
            musicianViewModel.clearAddAlbumToMusicianState()
            // Navegar de vuelta después de agregar exitosamente
            onBack()
        }
    }
    
    // Limpiar el estado cuando se sale de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            // Solo limpiar si no fue exitoso (si fue exitoso, ya se limpió arriba)
            if (addAlbumState !is AddAlbumToMusicianUiState.Success) {
                musicianViewModel.clearAddAlbumToMusicianState()
            }
        }
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidGreen)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Agregar Álbum",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MidGreen)
                .padding(paddingValues)
        ) {
            when (val currentState = uiState) {
                is AlbumUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Yellow)
                    }
                }
                is AlbumUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Barra de búsqueda
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        
                        // Lista de álbumes
                        val filteredAlbums = if (searchQuery.isBlank()) {
                            currentState.albums
                        } else {
                            currentState.albums.filter { album ->
                                album.name.contains(searchQuery, ignoreCase = true) ||
                                album.performers?.any { it.name.contains(searchQuery, ignoreCase = true) } == true
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAlbums) { album ->
                                AlbumSelectionItem(
                                    album = album,
                                    isSelected = selectedAlbum?.id == album.id,
                                    onClick = { selectedAlbum = album }
                                )
                            }
                        }
                        
                        // Botón de agregar (solo visible si hay un álbum seleccionado)
                        if (selectedAlbum != null) {
                            Button(
                                onClick = {
                                    selectedAlbum?.let { album ->
                                        musicianViewModel.addAlbumToMusician(musicianId, album.id)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                enabled = addAlbumState !is AddAlbumToMusicianUiState.Loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Yellow,
                                    contentColor = Color.Black
                                )
                            ) {
                                if (addAlbumState is AddAlbumToMusicianUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = "Agregar Álbum Seleccionado",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Mostrar error si existe
                        val errorState = addAlbumState as? AddAlbumToMusicianUiState.Error
                        errorState?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
                is AlbumUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Error al cargar álbumes",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = currentState.message,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { albumViewModel.refreshAlbums() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                is AlbumUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay álbumes disponibles",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Barra de búsqueda
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = "Buscar álbum...",
                color = Color.White.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = Color.White.copy(alpha = 0.7f)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkGreen,
            unfocusedContainerColor = DarkGreen,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
            focusedBorderColor = Yellow,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

/**
 * Item de álbum en la lista de selección
 */
@Composable
private fun AlbumSelectionItem(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val normalizedImageUrl = remember(album.cover) {
        ImageUrlHelper.normalizeImageUrl(album.cover, NetworkConstants.BASE_URL)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del álbum
            if (normalizedImageUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(normalizedImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Portada de ${album.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    loading = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Yellow
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Error",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Sin imagen",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Información del álbum
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getPerformersString(album.performers),
                    color = Yellow,
                    fontSize = 14.sp
                )
            }
            
            // Indicador de selección
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Yellow,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Obtiene una cadena con los nombres de los performers del álbum
 */
private fun getPerformersString(performers: List<com.miso.vinilos.model.data.Performer>?): String {
    return if (performers.isNullOrEmpty()) {
        "Artista desconocido"
    } else {
        performers.joinToString(", ") { it.name }
    }
}

