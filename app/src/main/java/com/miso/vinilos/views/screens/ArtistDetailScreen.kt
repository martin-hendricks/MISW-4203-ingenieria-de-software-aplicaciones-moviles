@file:Suppress("KotlinConstantConditions", "KotlinConstantConditions")

package com.miso.vinilos.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Add
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
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.PerformerPrize
import com.miso.vinilos.model.data.Prize
import android.util.Log
import com.miso.vinilos.model.network.NetworkConstants
import com.miso.vinilos.utils.ImageUrlHelper
import com.miso.vinilos.viewmodels.MusicianDetailUiState
import com.miso.vinilos.viewmodels.MusicianViewModel
import com.miso.vinilos.viewmodels.PrizeState
import com.miso.vinilos.views.theme.LightGreen
import com.miso.vinilos.views.theme.MidGreen
import com.miso.vinilos.views.theme.Yellow
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla que muestra el detalle completo de un artista (músico)
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param musicianId ID del músico a mostrar
 * @param musicianViewModel ViewModel que gestiona el estado del músico
 * @param onBack Callback para volver a la pantalla anterior
 * @param onAddAlbum Callback para agregar un álbum al artista
 */
@Composable
fun ArtistDetailScreen(
    musicianId: Int,
    musicianViewModel: MusicianViewModel,
    onBack: () -> Unit,
    onAddAlbum: () -> Unit = {}
) {
    // Cargar el músico cuando se crea la pantalla
    LaunchedEffect(musicianId) {
        musicianViewModel.loadMusicianDetail(musicianId)
    }

    // Observar el estado del detalle del músico
    val detailState by musicianViewModel.musicianDetailState.collectAsStateWithLifecycle()
    
    // Observar el estado de los premios
    val prizesState by musicianViewModel.prizesState.collectAsStateWithLifecycle()

    // Renderizar según el estado actual
    when (val currentState = detailState) {
        is MusicianDetailUiState.Loading -> {
            LoadingState(onBack = onBack)
        }
        is MusicianDetailUiState.Success -> {
            // Cargar los premios cuando se obtiene el músico exitosamente
            LaunchedEffect(currentState.musician.id) {
                val performerPrizes = currentState.musician.performerPrizes
                Log.d("ArtistDetailScreen", "LaunchedEffect: performerPrizes: ${performerPrizes?.size ?: 0} premios para artista ${currentState.musician.id}")
                
                // Log detallado de cada performerPrize
                performerPrizes?.forEachIndexed { idx, pp ->
                    Log.d("ArtistDetailScreen", "performerPrize[$idx]: id=${pp.id}, prizeId=${pp.prizeId}, prize.id=${pp.prize?.id}, premiationDate=${pp.premiationDate}")
                }
                
                if (!performerPrizes.isNullOrEmpty()) {
                    Log.d("ArtistDetailScreen", "Cargando premios para artista ${currentState.musician.id}")
                    musicianViewModel.loadPrizes(performerPrizes)
                } else {
                    Log.d("ArtistDetailScreen", "El artista no tiene premios asociados")
                    // Limpiar el estado de premios si el artista no tiene premios
                    musicianViewModel.clearPrizesState()
                }
            }
            
            ArtistDetailContent(
                musician = currentState.musician,
                prizesState = prizesState,
                onBack = onBack,
                onAddAlbum = onAddAlbum
            )
        }
        is MusicianDetailUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onBack = onBack,
                onRetry = { musicianViewModel.loadMusicianDetail(musicianId) }
            )
        }
    }
}

/**
 * Componente que muestra el contenido del detalle del artista
 */
@Composable
private fun ArtistDetailContent(
    musician: Musician,
    prizesState: Map<Int, PrizeState>,
    onBack: () -> Unit,
    onAddAlbum: () -> Unit = {}
) {
    // Calcular si todos los premios han terminado de cargarse (fuera del LazyColumn)
    // Como prizesState viene de un StateFlow, Compose recompone automáticamente cuando cambia
    // No necesitamos remember porque queremos recalcular cada vez que prizesState cambie
    val performerPrizes = musician.performerPrizes
    val hasPrizes = !performerPrizes.isNullOrEmpty()
    
    // Calcular directamente - Compose recompone cuando prizesState cambia (viene de StateFlow)
    val allPrizesLoaded = if (!hasPrizes) {
        Log.d("ArtistDetailScreen", "No hay premios, allPrizesLoaded=true")
        true // Si no hay premios, considerar "cargado" para mostrar el mensaje
    } else {
        val loaded = performerPrizes.all { pp ->
            // Usar la misma lógica que el ViewModel para obtener el prizeId
            val prizeId = pp.prize?.id 
                ?: pp.prizeId
                ?: pp.id // Usar el mismo fallback que en el ViewModel
            if (prizeId == null) {
                Log.w("ArtistDetailScreen", "PerformerPrize sin prizeId: ${pp.id}")
                false
            } else {
                val state = prizesState[prizeId]
                val isLoaded = state?.let { 
                    it.prize != null || it.error != null 
                } == true
                if (isLoaded) {
                    Log.d("ArtistDetailScreen", "Premio $prizeId: ✅ Cargado - ${state?.prize?.name}")
                } else {
                    Log.d("ArtistDetailScreen", "Premio $prizeId: ⏳ Pendiente - isLoading=${state?.isLoading}, error=${state?.error}")
                }
                isLoaded
            }
        }
        Log.d("ArtistDetailScreen", "allPrizesLoaded=$loaded, total premios=${performerPrizes.size}, estados=${prizesState.size}, keys=${prizesState.keys}")
        loaded
    }
    
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
                ArtistDetailHeader(onBack = onBack)
            }
            
            // Foto y nombre del artista
            item {
                ArtistPhotoSection(musician)
            }
            
            // Información personal
            item {
                ArtistPersonalInfoSection(musician)
            }
            
            // Descripción
            item {
                ArtistDescriptionSection(musician)
            }
            
            // Álbumes del artista
            item {
                ArtistAlbumsSection(
                    albums = musician.albums,
                    onAddAlbum = onAddAlbum
                )
            }
            
            // Siempre renderizar la sección de premios - ella manejará el estado de carga internamente
            // Como prizesState viene de StateFlow, Compose recompone automáticamente cuando cambia
            // La key incluye información sobre el estado para forzar recomposición
            val prizesStateKey = prizesState.keys.joinToString(",") + "_" + 
                prizesState.values.joinToString(",") { 
                    "${it.prize?.id ?: "null"}_${it.isLoading}_${it.error != null}" 
                }
            item(key = "prizes_${musician.id}_${prizesState.size}_$prizesStateKey") {
                Log.d("ArtistDetailScreen", "🔄 Renderizando sección de premios, prizesState.size=${prizesState.size}, keys=${prizesState.keys}, allPrizesLoaded=$allPrizesLoaded")
                ArtistPrizesSection(
                    performerPrizes = performerPrizes,
                    prizesState = prizesState
                )
            }
        }
    }
}

/**
 * Header con botón de retroceso
 */
@Composable
private fun ArtistDetailHeader(onBack: () -> Unit) {
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
 * Sección de la foto del artista
 */
@Composable
private fun ArtistPhotoSection(musician: Musician) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Foto del artista (circular)
        val normalizedImageUrl = remember(musician.image) {
            ImageUrlHelper.normalizeImageUrl(musician.image, NetworkConstants.BASE_URL)
        }
        
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
                contentDescription = "Foto de ${musician.name}",
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
                    Log.e("ArtistDetailScreen", "Error cargando foto: $normalizedImageUrl", error.result.throwable)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
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
        } else {
            // Placeholder si la URL no es válida
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4C8A8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Imagen no disponible",
                    tint = MidGreen,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre del artista
        Text(
            text = musician.name,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * Sección de información personal del artista
 */
@Composable
private fun ArtistPersonalInfoSection(musician: Musician) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        // Fecha de nacimiento
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val birthDateFormatted = dateFormat.format(musician.birthDate)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fecha de nacimiento: ",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = birthDateFormatted,
                color = Yellow,
                fontSize = 16.sp
            )
        }
        
        // Año de nacimiento
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Año de nacimiento: ",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${musician.getBirthYear()}",
                color = Yellow,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Sección de descripción del artista
 */
@Composable
private fun ArtistDescriptionSection(musician: Musician) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Biografía",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = musician.description,
            color = LightGreen,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

/**
 * Sección de álbumes del artista
 */
@Composable
private fun ArtistAlbumsSection(
    albums: List<Album>?,
    onAddAlbum: () -> Unit = {}
) {
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
                text = "Álbumes",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onAddAlbum) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar álbum",
                    tint = Yellow
                )
            }
        }

        if (albums.isNullOrEmpty()) {
            Text(
                text = "Este artista no tiene álbumes registrados",
                color = LightGreen,
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            albums.forEach { album ->
                AlbumCard(album)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Tarjeta de álbum en la lista
 */
@Composable
private fun AlbumCard(album: Album) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Álbum",
                tint = Yellow,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Lanzado en ${album.getReleaseYear()}",
                    color = LightGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Sección de premios del artista
 * Muestra los premios cargados a través del ViewModel
 */
@Composable
private fun ArtistPrizesSection(
    performerPrizes: List<PerformerPrize>?,
    prizesState: Map<Int, PrizeState>
) {
    val prizes = performerPrizes ?: emptyList()
    
    // Observar el estado para forzar recomposición cuando cambie
    val prizesStateKeys = prizesState.keys.joinToString()
    val prizesStateSize = prizesState.size
    
    // Forzar la observación del estado usando LaunchedEffect para detectar cambios
    LaunchedEffect(prizesStateKeys, prizesStateSize) {
        Log.d("ArtistDetailScreen", "LaunchedEffect: prizesState cambió - tamaño: $prizesStateSize, keys: $prizesStateKeys")
    }
    
    // Log para debug
    Log.d("ArtistDetailScreen", "ArtistPrizesSection: Renderizando con ${prizes.size} premios")
    Log.d("ArtistDetailScreen", "ArtistPrizesSection: prizesState tiene ${prizesState.size} estados, keys=${prizesState.keys}")
    
    // Log detallado de cada performerPrize
    prizes.forEachIndexed { index, pp ->
        Log.d("ArtistDetailScreen", "performerPrize[$index]: id=${pp.id}, prizeId=${pp.prizeId}, prize=${pp.prize?.id}, premiationDate=${pp.premiationDate}")
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Premios (${prizes.size})",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (prizes.isEmpty()) {
            Text(
                text = "Este artista no tiene premios registrados",
                color = LightGreen,
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            // Renderizar cada premio usando items para mejor reactividad
            prizes.forEachIndexed { index, performerPrize ->
                // Obtener el ID del premio (debe usar la misma lógica que el ViewModel)
                // Intentar diferentes formas: prize.id, prizeId, o performerPrize.id como fallback
                val prizeId = performerPrize.prize?.id 
                    ?: performerPrize.prizeId
                    ?: performerPrize.id // Usar el mismo fallback que en el ViewModel
                    ?: run {
                        Log.w("ArtistDetailScreen", "No se pudo obtener prizeId del performerPrize id=${performerPrize.id}")
                        return@forEachIndexed
                    }
                
                Log.d("ArtistDetailScreen", "prizeId obtenido: $prizeId (de prize.id=${performerPrize.prize?.id}, prizeId=${performerPrize.prizeId}, performerPrize.id=${performerPrize.id})")
                
                // Obtener el estado del premio desde el ViewModel
                val prizeState = prizesState[prizeId]
                
                Log.d("ArtistDetailScreen", "Renderizando premio [$index] prizeId=$prizeId")
                Log.d("ArtistDetailScreen", "prizesState keys: ${prizesState.keys}, tamaño: ${prizesState.size}")
                Log.d("ArtistDetailScreen", "prizeState para $prizeId: isLoading=${prizeState?.isLoading}, hasPrize=${prizeState?.prize != null}, error=${prizeState?.error}")
                
                // Log adicional para debug
                if (prizeState == null) {
                    Log.w("ArtistDetailScreen", "⚠️⚠️⚠️ prizeState es NULL para prizeId=$prizeId. Esto significa que loadPrizes no se ejecutó o falló")
                } else if (prizeState.prize != null) {
                    Log.d("ArtistDetailScreen", "✅✅✅ prizeState TIENE premio: ${prizeState.prize.name}")
                }
                
                // Renderizar el estado del premio
                when {
                    prizeState == null -> {
                        // Estado no existe aún, mostrar loading
                        Log.d("ArtistDetailScreen", "⚠️ Estado no existe para $prizeId, mostrando loading")
                        PrizeCardLoading()
                    }
                    prizeState.isLoading -> {
                        // Mostrar indicador de carga mientras se obtiene el premio
                        Log.d("ArtistDetailScreen", "⏳ Premio $prizeId está cargando")
                        PrizeCardLoading()
                    }
                    prizeState.prize != null -> {
                        // Mostrar el premio con toda su información
                        Log.d("ArtistDetailScreen", "✅ Mostrando premio $prizeId: ${prizeState.prize.name}")
                        PrizeCard(
                            prize = prizeState.prize,
                            premiationDate = performerPrize.premiationDate
                        )
                    }
                    prizeState.error != null -> {
                        // Mostrar error si no se pudo cargar
                        Log.e("ArtistDetailScreen", "❌ Error cargando premio $prizeId: ${prizeState.error}")
                        PrizeCardError(
                            performerPrize = performerPrize,
                            errorMessage = prizeState.error
                        )
                    }
                    else -> {
                        // Estado inicial, mostrar loading
                        Log.d("ArtistDetailScreen", "⚠️ Estado inicial para $prizeId, mostrando loading")
                        PrizeCardLoading()
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Tarjeta de premio con información completa
 */
@Composable
private fun PrizeCard(prize: Prize, premiationDate: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Nombre del premio
            Text(
                text = prize.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Organización
            Text(
                text = "Organización: ${prize.organization}",
                color = Yellow,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Fecha de premiación (si existe)
            premiationDate?.let { date ->
                Text(
                    text = "Premiado el: ${formatPremiationDate(date)}",
                    color = LightGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Descripción
            Text(
                text = prize.description,
                color = LightGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Tarjeta de carga para premio
 */
@Composable
private fun PrizeCardLoading() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Yellow,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Tarjeta de error para premio
 */
@Composable
private fun PrizeCardError(
    performerPrize: PerformerPrize,
    errorMessage: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Premio ID: ${performerPrize.prizeId ?: performerPrize.prize?.id ?: "Desconocido"}",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = errorMessage ?: "No se pudo cargar la información del premio",
                color = LightGreen,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/**
 * Formatea la fecha de premiación
 */
private fun formatPremiationDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
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
        // Botón de retroceso
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
        
        // Indicador de carga
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Yellow)
            Text(
                text = "Cargando artista...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
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
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidGreen)
    ) {
        // Botón de retroceso
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
        
        // Mensaje de error
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error al cargar artista",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = message,
                color = LightGreen,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

