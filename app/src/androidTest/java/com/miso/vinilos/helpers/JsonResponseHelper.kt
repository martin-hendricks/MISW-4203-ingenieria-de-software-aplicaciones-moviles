package com.miso.vinilos.helpers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.Collector
import okhttp3.mockwebserver.MockResponse

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
     * Crea una respuesta exitosa con una lista de músicos
     */
    fun createMusiciansSuccessResponse(musicians: List<Musician>): MockResponse {
        val json = gson.toJson(musicians)
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta exitosa con un solo músico
     */
    fun createMusicianSuccessResponse(musician: Musician): MockResponse {
        val json = gson.toJson(musician)
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta de lista vacía de músicos
     */
    fun createEmptyMusiciansResponse(): MockResponse {
        val json = gson.toJson(emptyList<Musician>())
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta exitosa con una lista de coleccionistas
     */
    fun createCollectorsSuccessResponse(collectors: List<Collector>): MockResponse {
        val json = gson.toJson(collectors)
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta exitosa con un solo coleccionista
     */
    fun createCollectorSuccessResponse(collector: Collector): MockResponse {
        val json = gson.toJson(collector)
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta de lista vacía de coleccionistas
     */
    fun createEmptyCollectorsResponse(): MockResponse {
        val json = gson.toJson(emptyList<Collector>())
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta exitosa 201 (Created) para POST requests
     * Usado para crear nuevos recursos como álbumes
     */
    fun createCreatedResponse(resource: Any): MockResponse {
        val json = gson.toJson(resource)
        return MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", "application/json")
            .setBody(json)
    }
    
    /**
     * Crea una respuesta exitosa 200 (OK) para POST requests que no crean recursos
     * Usado para operaciones como asociar álbum a artista
     */
    fun createOkResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{}")
    }
}
