package com.miso.vinilos.model.network

import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.CollectorCreateDTO
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define los endpoints de la API para coleccionistas
 * Corresponde a los endpoints del CollectorController del backend
 */
interface CollectorApiService {
    
    /**
     * Obtiene la lista completa de coleccionistas
     * GET /collectors
     */
    @GET("collectors")
    suspend fun getCollectors(): Response<List<Collector>>
    
    /**
     * Obtiene un coleccionista específico por su ID
     * GET /collectors/{id}
     * 
     * @param id ID del coleccionista
     */
    @GET("collectors/{id}")
    suspend fun getCollector(@Path("id") id: Int): Response<Collector>
    
    /**
     * Crea un nuevo coleccionista
     * POST /collectors
     * 
     * @param collector Datos del coleccionista a crear
     */
    @POST("collectors")
    suspend fun createCollector(@Body collector: CollectorCreateDTO): Response<Collector>
    
    /**
     * Actualiza un coleccionista existente
     * PUT /collectors/{id}
     * 
     * @param id ID del coleccionista a actualizar
     * @param collector Nuevos datos del coleccionista
     */
    @PUT("collectors/{id}")
    suspend fun updateCollector(
        @Path("id") id: Int,
        @Body collector: CollectorCreateDTO
    ): Response<Collector>
    
    /**
     * Elimina un coleccionista
     * DELETE /collectors/{id}
     * 
     * @param id ID del coleccionista a eliminar
     */
    @DELETE("collectors/{id}")
    suspend fun deleteCollector(@Path("id") id: Int): Response<Unit>
}

