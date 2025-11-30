package com.miso.vinilos.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.UserRole
import com.miso.vinilos.views.components.VinilosListItem
import com.miso.vinilos.views.components.VinilosListView
import com.miso.vinilos.viewmodels.CollectorUiState
import com.miso.vinilos.viewmodels.CollectorViewModel
import com.miso.vinilos.viewmodels.ProfileViewModel

/**
 * Pantalla que muestra la lista de coleccionistas
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param collectorViewModel ViewModel que gestiona el estado de los coleccionistas
 * @param profileViewModel ViewModel que gestiona el perfil del usuario
 * @param onCollectorClick Callback que se ejecuta cuando se hace clic en un coleccionista
 */
@Composable
fun CollectorListScreen(
    collectorViewModel: CollectorViewModel,
    profileViewModel: ProfileViewModel,
    onCollectorClick: (Collector) -> Unit
) {
    // Observa el estado de la UI desde el ViewModel
    val uiState by collectorViewModel.uiState.collectAsStateWithLifecycle()
    val userRole by profileViewModel.userRole.collectAsStateWithLifecycle()
    
    // Renderiza según el estado actual
    when (val currentState = uiState) {
        is CollectorUiState.Loading -> {
            LoadingState()
        }
        is CollectorUiState.Success -> {
            CollectorsList(
                collectors = currentState.collectors,
                userRole = userRole,
                onCollectorClick = onCollectorClick,
                onAddCollector = {
                    // TODO: Navegar a pantalla de agregar coleccionista
                }
            )
        }
        is CollectorUiState.Error -> {
            ErrorState(
                message = currentState.message,
                onRetry = { collectorViewModel.refreshCollectors() }
            )
        }
    }
}

/**
 * Componente que muestra la lista de coleccionistas usando VinilosListView
 * 
 * @param collectors Lista de coleccionistas a mostrar
 * @param userRole Rol actual del usuario
 * @param onCollectorClick Callback cuando se selecciona un coleccionista
 * @param onAddCollector Callback cuando se presiona el botón de agregar
 */
@Composable
private fun CollectorsList(
    collectors: List<Collector>,
    userRole: UserRole,
    onCollectorClick: (Collector) -> Unit,
    onAddCollector: () -> Unit
) {
    // Log para debug - ver qué datos tienen los coleccionistas
    collectors.forEachIndexed { index, collector ->
        android.util.Log.d("CollectorListScreen", "Collector[$index]: id=${collector.id}, name=${collector.name}, image=${collector.image}, email=${collector.email}")
    }
    
    VinilosListView(
        title = "", // Título vacío para evitar redundancia con el menú de navegación inferior
        items = collectors,
        // Solo muestra el botón de agregar si el usuario es coleccionista
        onPlusClick = if (userRole == UserRole.COLLECTOR) {
            { onAddCollector() }
        } else {
            null
        }
    ) { collector ->
        // Usar la imagen del coleccionista, o cadena vacía si no tiene
        val imageUrlToUse = collector.image?.takeIf { it.isNotBlank() } ?: ""
        // Generar iniciales del nombre para el placeholder
        val initials = collector.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        
        VinilosListItem(
            imageUrl = imageUrlToUse,
            topLabel = collector.name,
            bottomLabel = getCollectorDescription(collector),
            isImageCircular = true,
            onClick = { onCollectorClick(collector) },
            placeholderInitials = initials
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
                text = "Cargando coleccionistas...",
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
                text = "Error al cargar coleccionistas",
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
 * Obtiene una descripción del coleccionista para mostrar en la lista
 * Muestra el email o teléfono si está disponible
 * 
 * @param collector Coleccionista del cual obtener la descripción
 * @return Cadena con la descripción o información del coleccionista
 */
private fun getCollectorDescription(collector: Collector): String {
    return when {
        collector.email.isNotEmpty() -> collector.email
        collector.telephone.isNotEmpty() -> collector.telephone
        else -> "Sin información de contacto"
    }
}

