package com.miso.vinilos.model.network

/**
 * Constantes de configuración de red
 */
object NetworkConstants {
    /**
     * URL base del API
     * TODO: Cambiar a la URL de producción cuando esté disponible
     * Para emulador Android: 10.0.2.2 apunta a localhost de la máquina host
     * Para dispositivo físico: usar la IP de la máquina en la red local
     */
    const val BASE_URL = "http://10.0.2.2:3000/"
    
    /**
     * Timeout para las peticiones en segundos
     */
    const val TIMEOUT_SECONDS = 30L

}

