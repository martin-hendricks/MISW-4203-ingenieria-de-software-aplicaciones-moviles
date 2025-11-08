package com.miso.vinilos.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.CollectorAlbum
import com.miso.vinilos.model.data.Performer
import com.miso.vinilos.model.network.NetworkConstants
import com.miso.vinilos.utils.ImageUrlHelper
import com.miso.vinilos.views.components.ImagePlaceholder
import com.miso.vinilos.views.components.VinilosListItem
import com.miso.vinilos.viewmodels.AlbumState
import com.miso.vinilos.viewmodels.CollectorDetailUiState
import com.miso.vinilos.viewmodels.CollectorViewModel
import com.miso.vinilos.views.theme.LightGreen
import com.miso.vinilos.views.theme.MidGreen
import android.util.Log

/**
 * Pantalla que muestra el detalle completo de un coleccionista
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param collectorId ID del coleccionista a mostrar
 * @param collectorViewModel ViewModel que gestiona el estado del coleccionista
 * @param onBack Callback para volver a la pantalla anterior
 */
@Composable
fun CollectorDetailScreen(
    collectorId: Int,
    collectorViewModel: CollectorViewModel,
    onBack: () -> Unit
) {
    // Cargar el coleccionista cuando se crea la pantalla
    LaunchedEffect(collectorId) {
        collectorViewModel.loadCollectorDetail(collectorId)
    }

    // Observar el estado del detalle del coleccionista
    val detailState by collectorViewModel.collectorDetailState.collectAsStateWithLifecycle()
    
    // Observar el estado de los álbumes cargados
    val albumsState by collectorViewModel.albumsState.collectAsStateWithLifecycle()

    // Renderizar según el estado actual
    when (val currentState = detailState) {
        is CollectorDetailUiState.Loading -> {
            LoadingState(onBack = onBack)
        }
        is CollectorDetailUiState.Success -> {
            // Cargar los álbumes cuando se obtiene el coleccionista exitosamente
            LaunchedEffect(currentState.collector.id) {
                val collectorAlbums = currentState.collector.collectorAlbums
                Log.d("CollectorDetailScreen", "LaunchedEffect: collectorAlbums: ${collectorAlbums?.size ?: 0} álbumes para coleccionista ${currentState.collector.id}")
                
                collectorAlbums?.forEachIndexed { idx, ca ->
                    Log.d("CollectorDetailScreen", "collectorAlbum[$idx]: id=${ca.id}, albumId=${ca.albumId}, album=${ca.album}, album?.id=${ca.album?.id}, price=${ca.price}, status=${ca.status}")
                }
                
                if (!collectorAlbums.isNullOrEmpty()) {
                    Log.d("CollectorDetailScreen", "Cargando álbumes para coleccionista ${currentState.collector.id}")
                    collectorViewModel.loadAlbums(collectorAlbums)
                } else {
                    Log.d("CollectorDetailScreen", "El coleccionista no tiene álbumes asociados")
                    collectorViewModel.clearAlbumsState()
                }
            }
            
            CollectorDetailContent(
                collector = currentState.collector,
                albumsState = albumsState,
                onBack = onBack
            )
        }
        is CollectorDetailUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onBack = onBack
            )
        }
    }
}

/**
 * Componente que muestra el contenido del detalle del coleccionista
 */
@Composable
private fun CollectorDetailContent(
    collector: Collector,
    albumsState: Map<Int, AlbumState>,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidGreen)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header con botón de retroceso
            item {
                CollectorDetailHeader(onBack = onBack)
            }
            
            // Foto y nombre del coleccionista
            item {
                CollectorPhotoSection(collector)
            }
            
            // Información de contacto
            item {
                CollectorContactInfoSection(collector)
            }
            
            // Álbumes del coleccionista
            item {
                CollectorAlbumsSection(
                    collectorAlbums = collector.collectorAlbums,
                    albumsState = albumsState
                )
            }
            
            // Performers favoritos
            item {
                CollectorFavoritePerformersSection(collector.favoritePerformers)
            }
        }
    }
}

/**
 * Header con botón de retroceso
 */
@Composable
private fun CollectorDetailHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }
    }
}

/**
 * Sección de la foto del coleccionista
 */
@Composable
private fun CollectorPhotoSection(collector: Collector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Foto del coleccionista (circular)
        val normalizedImageUrl = remember(collector.image) {
            collector.image?.let { ImageUrlHelper.normalizeImageUrl(it, NetworkConstants.BASE_URL) }
        }
        
        // Generar iniciales del nombre
        val initials = collector.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        
        if (normalizedImageUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(normalizedImageUrl)
                    .crossfade(true)
                    .allowHardware(false)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Foto de ${collector.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape),
                loading = {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFFD4C8A8)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MidGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
                error = { error ->
                    Log.e("CollectorDetailScreen", "Error cargando foto: $normalizedImageUrl", error.result.throwable)
                    ImagePlaceholder(
                        modifier = Modifier.size(200.dp),
                        isCircular = true,
                        initials = initials
                    )
                }
            )
        } else {
            // Placeholder con iniciales si no hay imagen
            ImagePlaceholder(
                modifier = Modifier.size(200.dp),
                isCircular = true,
                initials = initials
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre del coleccionista
        Text(
            text = collector.name,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * Sección de información de contacto del coleccionista
 */
@Composable
private fun CollectorContactInfoSection(collector: Collector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Información de contacto",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Email
        ContactInfoRow(
            icon = Icons.Default.Email,
            label = "Email",
            value = collector.email
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Teléfono
        ContactInfoRow(
            icon = Icons.Default.Phone,
            label = "Teléfono",
            value = collector.telephone
        )
    }
}

/**
 * Fila de información de contacto
 */
@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = LightGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = LightGreen,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Sección de álbumes del coleccionista
 */
@Composable
private fun CollectorAlbumsSection(
    collectorAlbums: List<CollectorAlbum>?,
    albumsState: Map<Int, AlbumState>
) {
    val albums = collectorAlbums ?: emptyList()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Álbumes en colección (${albums.size})",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (albums.isEmpty()) {
            Text(
                text = "Este coleccionista no tiene álbumes registrados",
                color = LightGreen,
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            albums.forEachIndexed { index, collectorAlbum ->
                Log.d("CollectorDetailScreen", "Renderizando collectorAlbum[$index]: id=${collectorAlbum.id}, albumId=${collectorAlbum.albumId}, album=${collectorAlbum.album}, album?.id=${collectorAlbum.album?.id}")
                
                // Usar la misma lógica que en el ViewModel para obtener el albumId
                val albumId = collectorAlbum.album?.id 
                    ?: collectorAlbum.albumId
                    ?: collectorAlbum.id // Usar el mismo fallback que en el ViewModel
                    ?: run {
                        Log.w("CollectorDetailScreen", "No se pudo obtener albumId del collectorAlbum id=${collectorAlbum.id}")
                        return@forEachIndexed
                    }
                
                Log.d("CollectorDetailScreen", "albumId obtenido: $albumId (de album.id=${collectorAlbum.album?.id}, albumId=${collectorAlbum.albumId}, collectorAlbum.id=${collectorAlbum.id})")
                
                // Obtener el estado del álbum
                val albumState = albumsState[albumId]
                
                Log.d("CollectorDetailScreen", "albumState para $albumId: isLoading=${albumState?.isLoading}, hasAlbum=${albumState?.album != null}, error=${albumState?.error}")
                Log.d("CollectorDetailScreen", "albumsState keys: ${albumsState.keys}, tamaño: ${albumsState.size}")
                
                when {
                    albumState == null -> {
                        // Estado no existe aún, mostrar loading
                        Log.d("CollectorDetailScreen", "⚠️ Estado no existe para $albumId, mostrando loading")
                        AlbumCardLoading()
                    }
                    albumState.isLoading -> {
                        // Mostrar loading
                        Log.d("CollectorDetailScreen", "⏳ Álbum $albumId está cargando")
                        AlbumCardLoading()
                    }
                    albumState.album != null -> {
                        // Mostrar el álbum
                        Log.d("CollectorDetailScreen", "✅ Mostrando álbum $albumId: ${albumState.album.name}")
                        AlbumCard(
                            album = albumState.album,
                            price = collectorAlbum.price,
                            status = collectorAlbum.status
                        )
                    }
                    albumState.error != null -> {
                        // Mostrar error
                        Log.e("CollectorDetailScreen", "❌ Error cargando álbum $albumId: ${albumState.error}")
                        AlbumCardError(
                            collectorAlbum = collectorAlbum,
                            errorMessage = albumState.error
                        )
                    }
                    else -> {
                        // Estado inicial, mostrar loading
                        Log.d("CollectorDetailScreen", "⚠️ Estado inicial para $albumId, mostrando loading")
                        AlbumCardLoading()
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Tarjeta de álbum cargando
 */
@Composable
private fun AlbumCardLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFD4C8A8)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MidGreen,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Cargando...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Tarjeta de álbum con información completa
 */
@Composable
private fun AlbumCard(
    album: com.miso.vinilos.model.data.Album,
    price: Int,
    status: String
) {
    VinilosListItem(
        imageUrl = album.cover,
        topLabel = album.name,
        bottomLabel = "Precio: $${price.formatPrice()} • Estado: $status",
        onClick = { /* TODO: Navegar al detalle del álbum */ }
    )
}

/**
 * Tarjeta de álbum con error
 */
@Composable
private fun AlbumCardError(
    collectorAlbum: CollectorAlbum,
    errorMessage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFD4C8A8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Error",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Álbum ID: ${collectorAlbum.album?.id ?: collectorAlbum.albumId ?: "N/A"}",
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = errorMessage,
                color = LightGreen,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Formatea el precio con separadores de miles
 */
private fun Int.formatPrice(): String {
    return String.format("%,d", this)
}

/**
 * Sección de performers favoritos del coleccionista
 */
@Composable
private fun CollectorFavoritePerformersSection(favoritePerformers: List<Performer>?) {
    val performers = favoritePerformers ?: emptyList()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Artistas favoritos (${performers.size})",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (performers.isEmpty()) {
            Text(
                text = "Este coleccionista no tiene artistas favoritos registrados",
                color = LightGreen,
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            performers.forEach { performer ->
                val initials = performer.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                VinilosListItem(
                    imageUrl = performer.image,
                    topLabel = performer.name,
                    bottomLabel = performer.description.take(50) + if (performer.description.length > 50) "..." else "",
                    isImageCircular = true,
                    onClick = { /* TODO: Navegar al detalle del performer */ },
                    placeholderInitials = initials
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Componente que muestra el estado de carga
 */
@Composable
private fun LoadingState(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidGreen)
    ) {
        // Header con botón de retroceso
        CollectorDetailHeader(onBack = onBack)
        
        // Indicador de carga centrado
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando coleccionista...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Componente que muestra el estado de error
 */
@Composable
private fun ErrorState(
    message: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidGreen)
    ) {
        // Header con botón de retroceso
        CollectorDetailHeader(onBack = onBack)
        
        // Mensaje de error centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Error",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error al cargar coleccionista",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = LightGreen,
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

