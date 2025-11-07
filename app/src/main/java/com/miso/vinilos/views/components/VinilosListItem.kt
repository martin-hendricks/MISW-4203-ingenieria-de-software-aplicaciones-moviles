package com.miso.vinilos.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.miso.vinilos.model.network.NetworkConstants
import com.miso.vinilos.utils.ImageUrlHelper
import android.util.Log

@Composable
fun VinilosListItem(
    imageUrl: String,
    topLabel: String,
    bottomLabel: String,
    isImageCircular: Boolean = false,
    onClick: () -> Unit
) {
    // Normalizar y validar la URL de la imagen
    val normalizedUrl = remember(imageUrl) {
        ImageUrlHelper.normalizeImageUrl(imageUrl, NetworkConstants.BASE_URL)
    }
    
    // Log para debug - ver URLs que llegan
    Log.d("VinilosListItem", "URL original: $imageUrl")
    Log.d("VinilosListItem", "URL normalizada: $normalizedUrl")
    Log.d("VinilosListItem", "Cargando imagen para: $topLabel")

    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )) {
        Row (
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
            ){
                // Imagen con manejo de estados mejorado
                if (normalizedUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(normalizedUrl)
                            .crossfade(true)
                            .allowHardware(false) // Mejor compatibilidad con diferentes formatos (PNG, JPG, etc.)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Imagen de $topLabel",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(if (isImageCircular) CircleShape else RoundedCornerShape(4.dp)),
                        loading = {
                            // Placeholder mientras carga
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        error = { error ->
                            // Log del error para debugging
                            Log.e("VinilosListItem", "Error cargando imagen: $normalizedUrl", error.result.throwable)
                            // Imagen de error si falla la carga
                            ImagePlaceholder(
                                modifier = Modifier.size(64.dp),
                                isCircular = isImageCircular
                            )
                        }
                    )
                } else {
                    // Mostrar placeholder si la URL no es válida
                    ImagePlaceholder(
                        modifier = Modifier.size(64.dp),
                        isCircular = isImageCircular
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = topLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = bottomLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
        }
    }
}

/**
 * Placeholder para cuando no hay imagen o falla la carga
 */
@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    isCircular: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(if (isCircular) CircleShape else RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = "Imagen no disponible",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp)
        )
    }
}