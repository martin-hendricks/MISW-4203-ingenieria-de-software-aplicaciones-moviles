package com.miso.vinilos.utils

import android.util.Log

/**
 * Helper para normalizar y validar URLs de imágenes
 */
object ImageUrlHelper {
    
    private const val TAG = "ImageUrlHelper"
    
    /**
     * Normaliza y valida una URL de imagen
     * - Limpia espacios en blanco
     * - Convierte URLs relativas a absolutas si es necesario
     * - Maneja URLs vacías o nulas
     * - Codifica caracteres especiales si es necesario
     * 
     * @param imageUrl URL original de la imagen
     * @param baseUrl URL base del API (opcional, para convertir URLs relativas)
     * @return URL normalizada y validada, o null si la URL no es válida
     */
    fun normalizeImageUrl(imageUrl: String?, baseUrl: String? = null): String? {
        if (imageUrl.isNullOrBlank()) {
            Log.d(TAG, "URL de imagen vacía o nula")
            return null
        }
        
        // Limpiar espacios en blanco
        var cleanedUrl = imageUrl.trim()
        
        // Si la URL está vacía después de limpiar, retornar null
        if (cleanedUrl.isEmpty()) {
            return null
        }
        
        // Si la URL ya es absoluta (http/https), validarla y retornarla sin modificar
        if (cleanedUrl.startsWith("http://") || cleanedUrl.startsWith("https://")) {
            try {
                val url = java.net.URL(cleanedUrl)
                // Validar que sea una URL válida
                url.toURI() // Esto lanza excepción si la URL no es válida
                Log.d(TAG, "URL absoluta válida (sin modificar): $cleanedUrl")
                return cleanedUrl
            } catch (e: Exception) {
                Log.e(TAG, "URL absoluta inválida: $cleanedUrl", e)
                // Intentar corregir espacios en la URL absoluta
                try {
                    val fixedUrl = cleanedUrl.replace(" ", "%20")
                    java.net.URL(fixedUrl).toURI()
                    Log.d(TAG, "URL absoluta corregida: $fixedUrl")
                    return fixedUrl
                } catch (e2: Exception) {
                    Log.e(TAG, "No se pudo corregir la URL absoluta: $cleanedUrl", e2)
                    return null
                }
            }
        }
        
        // Si la URL es relativa y tenemos una baseUrl, convertirla a absoluta
        if (!cleanedUrl.startsWith("file://")) {
            if (baseUrl != null && baseUrl.isNotEmpty()) {
                // Asegurar que baseUrl termine con /
                val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                // Asegurar que cleanedUrl no empiece con /
                val normalizedPath = if (cleanedUrl.startsWith("/")) cleanedUrl.substring(1) else cleanedUrl
                cleanedUrl = "$normalizedBaseUrl$normalizedPath"
                Log.d(TAG, "URL relativa convertida a absoluta: $cleanedUrl")
            } else {
                Log.w(TAG, "URL relativa sin baseUrl proporcionada: $cleanedUrl")
                // Retornar null si no podemos convertirla
                return null
            }
        }
        
        // Validar que la URL tenga un formato básico válido
        try {
            val url = java.net.URL(cleanedUrl)
            val protocol = url.protocol
            if (protocol !in listOf("http", "https", "file")) {
                Log.w(TAG, "Protocolo no soportado: $protocol")
                return null
            }
            url.toURI() // Validar formato completo
            Log.d(TAG, "URL validada: $cleanedUrl")
            return cleanedUrl
        } catch (e: Exception) {
            Log.e(TAG, "URL inválida: $cleanedUrl", e)
            // Intentar corregir espacios
            try {
                val fixedUrl = cleanedUrl.replace(" ", "%20")
                java.net.URL(fixedUrl).toURI()
                Log.d(TAG, "URL corregida: $fixedUrl")
                return fixedUrl
            } catch (e2: Exception) {
                Log.e(TAG, "No se pudo corregir la URL: $cleanedUrl", e2)
            }
            return null
        }
    }
    
    /**
     * Verifica si una URL es válida para cargar imágenes
     */
    fun isValidImageUrl(url: String?): Boolean {
        return normalizeImageUrl(url) != null
    }
}

