package com.miso.vinilos.helpers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.miso.vinilos.model.data.Album
import okhttp3.mockwebserver.MockResponse
import java.util.*

/**
 * Helper para crear respuestas JSON para MockWebServer
 * 
 * Proporciona métodos para convertir objetos de prueba a JSON
 * y crear respuestas HTTP mock apropiadas
 */
object JsonResponseHelper {
    
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    /**
     * Crea una respuesta exitosa con una lista de álbumes
     */
    fun createAlbumsSuccessResponse(albums: List<Album>): MockResponse {
        val json = gson.toJson(albums)
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta exitosa con un solo álbum
     */
    fun createAlbumSuccessResponse(album: Album): MockResponse {
        val json = gson.toJson(album)
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta de lista vacía
     */
    fun createEmptyAlbumsResponse(): MockResponse {
        val json = gson.toJson(emptyList<Album>())
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta de error 500 (Error interno del servidor)
     */
    fun createServerErrorResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"error": "Internal Server Error"}""")
    }
    
    /**
     * Crea una respuesta de error 404 (No encontrado)
     */
    fun createNotFoundResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(404)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"error": "Not Found"}""")
    }
    
    /**
     * Crea una respuesta de error 400 (Bad Request)
     */
    fun createBadRequestResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"error": "Bad Request"}""")
    }
    
    /**
     * Crea una respuesta de timeout (simula conexión lenta)
     */
    fun createTimeoutResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(gson.toJson(TestDataFactory.createTestAlbums()))
            .setBodyDelay(10, java.util.concurrent.TimeUnit.SECONDS) // 10 segundos de delay
    }
    
    /**
     * Crea una respuesta de error de conexión (simula fallo de red)
     */
    fun createConnectionErrorResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(gson.toJson(TestDataFactory.createTestAlbums()))
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START)
    }
    
    /**
     * Crea una respuesta con JSON malformado
     */
    fun createMalformedJsonResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{ invalid json }")
    }
    
    /**
     * Crea una respuesta con datos parciales (simula respuesta incompleta)
     */
    fun createPartialResponse(): MockResponse {
        val partialJson = """
            [
                {
                    "id": 1,
                    "name": "Abbey Road",
                    "cover": "https://example.com/abbey-road.jpg",
                    "releaseDate": "1969-09-26T00:00:00.000Z",
                    "description": "El último álbum grabado por The Beatles",
                    "genre": {
                        "id": 1,
                        "name": "Rock"
                    },
                    "recordLabel": {
                        "id": 1,
                        "name": "Apple Records"
                    }
                }
            ]
        """.trimIndent()
        
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(partialJson)
    }
}
