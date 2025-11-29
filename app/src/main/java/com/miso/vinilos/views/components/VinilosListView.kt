package com.miso.vinilos.views.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Componente reutilizable que muestra un título y una lista de elementos
 * 
 * @param T Tipo genérico (Album, Artist, Collector, etc.)
 * @param title Título que se muestra en la parte superior
 * @param items Lista de elementos a mostrar
 * @param modifier Modificadores opcionales
 * @param onPlusClick Callback opcional para el botón de agregar
 * @param itemContent Función composable para renderizar cada item
 */
@Composable
fun <T> VinilosListView(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    onPlusClick: (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header con título y botón
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Título
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        // Marcar como heading para navegación de TalkBack
                        heading()
                    }
            )
            
            // Botón Plus (solo si onPlusClick no es null)
            onPlusClick?.let {
                FilledIconButton(
                    onClick = it,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        // Descripción contextual para TalkBack
                        contentDescription = "Agregar nuevo elemento a $title"
                    )
                }
            }
        }
        
        // Lista
        LazyColumn(
            modifier = Modifier.semantics {
                // Descripción de la lista para TalkBack
                contentDescription = "Lista de $title, ${items.size} elementos"
            }
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}