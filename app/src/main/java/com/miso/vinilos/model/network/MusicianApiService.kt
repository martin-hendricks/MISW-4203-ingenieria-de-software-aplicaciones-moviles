package com.miso.vinilos.model.network

import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.MusicianCreateDTO
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define los endpoints de la API para músicos
 * Corresponde a los endpoints del MusicianController del backend
 */
interface MusicianApiService {
    
    /**
     * Obtiene la lista completa de músicos
     * GET /musicians
     */
    @GET("musicians")
    suspend fun getMusicians(): Response<List<Musician>>
    
    /**
     * Obtiene un músico específico por su ID
     * GET /musicians/{id}
     * 
     * @param id ID del músico
     */
    @GET("musicians/{id}")
    suspend fun getMusician(@Path("id") id: Int): Response<Musician>
    
    /**
     * Crea un nuevo músico
     * POST /musicians
     * 
     * @param musician Datos del músico a crear
     */
    @POST("musicians")
    suspend fun createMusician(@Body musician: MusicianCreateDTO): Response<Musician>
    
    /**
     * Actualiza un músico existente
     * PUT /musicians/{id}
     * 
     * @param id ID del músico a actualizar
     * @param musician Nuevos datos del músico
     */
    @PUT("musicians/{id}")
    suspend fun updateMusician(
        @Path("id") id: Int,
        @Body musician: MusicianCreateDTO
    ): Response<Musician>
    
    /**
     * Elimina un músico
     * DELETE /musicians/{id}
     * 
     * @param id ID del músico a eliminar
     */
    @DELETE("musicians/{id}")
    suspend fun deleteMusician(@Path("id") id: Int): Response<Unit>
}

