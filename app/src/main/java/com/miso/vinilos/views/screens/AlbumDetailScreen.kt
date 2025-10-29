package com.miso.vinilos.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.miso.vinilos.model.data.Comment
import com.miso.vinilos.model.data.Track
import com.miso.vinilos.viewmodels.AlbumDetailUiState
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.views.theme.LightGreen
import com.miso.vinilos.views.theme.MidGreen
import com.miso.vinilos.views.theme.Yellow
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla que muestra el detalle completo de un álbum
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param albumId ID del álbum a mostrar
 * @param albumViewModel ViewModel que gestiona el estado del álbum
 * @param onBack Callback para volver a la pantalla anterior
 */
@Composable
fun AlbumDetailScreen(
    albumId: Int,
    albumViewModel: AlbumViewModel,
    onBack: () -> Unit
) {
    // Cargar el álbum cuando se crea la pantalla
    LaunchedEffect(albumId) {
        albumViewModel.loadAlbumDetail(albumId)
    }

    // Observar el estado del detalle del álbum
    val detailState by albumViewModel.albumDetailState.collectAsStateWithLifecycle()

    // Renderizar según el estado actual
    when (val currentState = detailState) {
        is AlbumDetailUiState.Loading -> {
            LoadingState(onBack = onBack)
        }
        is AlbumDetailUiState.Success -> {
            AlbumDetailContent(
                album = currentState.album,
                onBack = onBack
            )
        }
        is AlbumDetailUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onBack = onBack,
                onRetry = { albumViewModel.loadAlbumDetail(albumId) }
            )
        }
    }
}

/**
 * Componente que muestra el contenido del detalle del álbum
 */
@Composable
private fun AlbumDetailContent(
    album: Album,
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
            // Header con botón de back
            item {
                HeaderSection(onBack = onBack)
            }

            // Cover del álbum
            item {
                AlbumCoverSection(album = album)
            }

            // Detalles del álbum
            item {
                AlbumDetailsSection(album = album)
            }

            // Lista de canciones
            item {
                TrackListSection(tracks = album.tracks)
            }

            // Comentarios
            item {
                CommentsSection(comments = album.comments)
            }

            // Espaciado final
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Sección del header con botón de back
 */
@Composable
private fun HeaderSection(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Sección del cover del álbum
 */
@Composable
private fun AlbumCoverSection(album: Album) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover del álbum
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(album.cover)
                .crossfade(true)
                .build(),
            contentDescription = "Cover de ${album.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFD4C8A8)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MidGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFD4C8A8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Error al cargar imagen",
                        tint = MidGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre del álbum
        Text(
            text = album.name,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Artista(s)
        Text(
            text = getPerformersString(album.performers),
            color = Yellow,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Sección de detalles del álbum
 */
@Composable
private fun AlbumDetailsSection(album: Album) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Detalles del Álbum",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = album.description,
            color = LightGreen,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        DetailRow(label = "Lanzamiento", value = formatDate(album.releaseDate))
        DetailRow(label = "Género", value = album.genre.displayName)
        DetailRow(label = "Sello Discográfico", value = album.recordLabel.displayName)
    }
}

/**
 * Fila de detalle con label y valor
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = LightGreen,
            fontSize = 14.sp
        )
    }
}

/**
 * Sección de lista de canciones
 */
@Composable
private fun TrackListSection(tracks: List<Track>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lista de Canciones",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(
                onClick = { /* TODO: Agregar canción */ },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Yellow
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar canción",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (tracks.isNullOrEmpty()) {
            Text(
                text = "No hay canciones en este álbum.",
                color = LightGreen,
                fontSize = 14.sp
            )
        } else {
            tracks.forEach { track ->
                TrackItem(track = track)
            }
        }
    }
}

/**
 * Item de canción
 */
@Composable
private fun TrackItem(track: Track) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Yellow,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = track.name,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = track.duration,
            color = LightGreen,
            fontSize = 14.sp
        )
    }
}

/**
 * Sección de comentarios
 */
@Composable
private fun CommentsSection(comments: List<Comment>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Comentarios",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (comments.isNullOrEmpty()) {
            Text(
                text = "No hay comentarios aún.",
                color = LightGreen,
                fontSize = 14.sp
            )
        } else {
            comments.forEach { comment ->
                CommentItem(comment = comment)
            }
        }
    }
}

/**
 * Item de comentario
 */
@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A4940)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "U",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Usuario",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.description,
                color = LightGreen,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rating: ${comment.rating}/5",
                color = Yellow,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Estado de carga
 */
@Composable
private fun LoadingState(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidGreen)
    ) {
        HeaderSection(onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Yellow)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando álbum...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Estado de error
 */
@Composable
private fun ErrorState(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidGreen)
    ) {
        HeaderSection(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Error al cargar el álbum",
                color = MaterialTheme.colorScheme.error,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = MidGreen
                )
            ) {
                Text("Reintentar")
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

/**
 * Formatea una fecha para mostrar
 */
private fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(date)
}
