package com.miso.vinilos.rules

import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import java.io.IOException

/**
 * JUnit rule para manejar el ciclo de vida de MockWebServer en las pruebas E2E
 * 
 * Esta regla se encarga de:
 * - Iniciar el servidor mock antes de cada prueba
 * - Detener el servidor mock después de cada prueba
 * - Proporcionar acceso a la URL base del servidor
 */
class MockWebServerRule : ExternalResource() {
    
    private val mockWebServer = MockWebServer()
    
    /**
     * URL base del servidor mock
     * Se puede usar para configurar Retrofit en las pruebas
     */
    val baseUrl: String
        get() = mockWebServer.url("/").toString()
    
    /**
     * Instancia del MockWebServer para configurar respuestas
     */
    val server: MockWebServer
        get() = mockWebServer
    
    @Throws(IOException::class)
    override fun before() {
        mockWebServer.start()
    }
    
    override fun after() {
        try {
            mockWebServer.shutdown()
        } catch (e: IOException) {
            // Log error but don't fail the test
            println("Error shutting down MockWebServer: ${e.message}")
        }
    }
}
