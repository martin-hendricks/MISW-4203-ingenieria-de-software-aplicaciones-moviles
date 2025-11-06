package com.miso.vinilos.model.network

import com.miso.vinilos.model.data.Prize
import com.miso.vinilos.model.data.PrizeCreateDTO
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define los endpoints de la API para premios
 * Corresponde a los endpoints del PrizeController del backend
 */
interface PrizeApiService {
    
    /**
     * Obtiene la lista completa de premios
     * GET /prizes
     */
    @GET("prizes")
    suspend fun getPrizes(): Response<List<Prize>>
    
    /**
     * Obtiene un premio específico por su ID
     * GET /prizes/{id}
     * 
     * @param id ID del premio
     */
    @GET("prizes/{id}")
    suspend fun getPrize(@Path("id") id: Int): Response<Prize>
    
    /**
     * Crea un nuevo premio
     * POST /prizes
     * 
     * @param prize Datos del premio a crear
     */
    @POST("prizes")
    suspend fun createPrize(@Body prize: PrizeCreateDTO): Response<Prize>
    
    /**
     * Actualiza un premio existente
     * PUT /prizes/{id}
     * 
     * @param id ID del premio a actualizar
     * @param prize Nuevos datos del premio
     */
    @PUT("prizes/{id}")
    suspend fun updatePrize(
        @Path("id") id: Int,
        @Body prize: PrizeCreateDTO
    ): Response<Prize>
    
    /**
     * Elimina un premio
     * DELETE /prizes/{id}
     * 
     * @param id ID del premio a eliminar
     */
    @DELETE("prizes/{id}")
    suspend fun deletePrize(@Path("id") id: Int): Response<Unit>
}

