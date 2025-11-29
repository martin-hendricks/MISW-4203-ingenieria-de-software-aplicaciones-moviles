package com.miso.vinilos.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miso.vinilos.model.data.UserRole
import com.miso.vinilos.viewmodels.ProfileViewModel

/**
 * Pantalla de perfil donde el usuario puede seleccionar su rol
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel
) {
    val currentRole by viewModel.userRole.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título eliminado para evitar redundancia con el menú de navegación inferior
        Text(
            text = "Selecciona tu rol",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Opciones de roles
        RoleOption(
            role = UserRole.VISITOR,
            icon = Icons.Default.Person,
            isSelected = currentRole == UserRole.VISITOR,
            onClick = { viewModel.selectRole(UserRole.VISITOR) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        RoleOption(
            role = UserRole.COLLECTOR,
            icon = Icons.Default.Star,
            isSelected = currentRole == UserRole.COLLECTOR,
            onClick = { viewModel.selectRole(UserRole.COLLECTOR) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Información del rol actual
        CurrentRoleInfo(currentRole)
    }
}

/**
 * Componente para mostrar una opción de rol
 */
@Composable
private fun RoleOption(
    role: UserRole,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del rol
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) 
                            MaterialTheme.colorScheme.tertiary
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = role.displayName,
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onTertiary
                    else 
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Información del rol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = role.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // Indicador de selección
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Muestra información sobre el rol actual
 */
@Composable
private fun CurrentRoleInfo(currentRole: UserRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Rol actual",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = currentRole.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (currentRole) {
                    UserRole.VISITOR -> "Como visitante, puedes explorar todos los álbumes, artistas y coleccionistas, pero no puedes agregar contenido nuevo."
                    UserRole.COLLECTOR -> "Como coleccionista, tienes acceso completo para agregar y gestionar álbumes en tu colección."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

