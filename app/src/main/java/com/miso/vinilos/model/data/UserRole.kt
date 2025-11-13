package com.miso.vinilos.model.data

/**
 * Enumeración de roles de usuario disponibles en la aplicación
 */
enum class UserRole(val displayName: String, val description: String) {
    VISITOR("Visitante", "Solo puede ver contenido"),
    COLLECTOR("Coleccionista", "Puede agregar y gestionar álbumes");
    
    companion object
}

