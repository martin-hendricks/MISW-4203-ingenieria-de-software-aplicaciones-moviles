package com.miso.vinilos.model.network

import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.AlbumCreateDTO
import com.miso.vinilos.model.data.Track
import com.miso.vinilos.model.data.TrackCreateDTO
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define los endpoints de la API para álbumes
 * Corresponde a los endpoints del AlbumController del backend
 */
interface AlbumApiService {
    
    /**
     * Obtiene la lista completa de álbumes
     * GET /albums
     */
    @GET("albums")
    suspend fun getAlbums(): Response<List<Album>>
    
    /**
     * Obtiene un álbum específico por su ID
     * GET /albums/{id}
     * 
     * @param id ID del álbum
     */
    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") id: Int): Response<Album>
    
    /**
     * Crea un nuevo álbum
     * POST /albums
     * 
     * @param album Datos del álbum a crear
     */
    @POST("albums")
    suspend fun createAlbum(@Body album: AlbumCreateDTO): Response<Album>
    
    /**
     * Actualiza un álbum existente
     * PUT /albums/{id}
     * 
     * @param id ID del álbum a actualizar
     * @param album Nuevos datos del álbum
     */
    @PUT("albums/{id}")
    suspend fun updateAlbum(
        @Path("id") id: Int,
        @Body album: AlbumCreateDTO
    ): Response<Album>
    
    /**
     * Elimina un álbum
     * DELETE /albums/{id}
     * 
     * @param id ID del álbum a eliminar
     */
    @DELETE("albums/{id}")
    suspend fun deleteAlbum(@Path("id") id: Int): Response<Unit>
    
    /**
     * Obtiene los tracks de un álbum específico
     * GET /albums/{albumId}/tracks
     * 
     * @param albumId ID del álbum
     */
    @GET("albums/{albumId}/tracks")
    suspend fun getAlbumTracks(@Path("albumId") albumId: Int): Response<List<Track>>
    
    /**
     * Agrega un track a un álbum
     * POST /albums/{albumId}/tracks/{trackId}
     * 
     * @param albumId ID del álbum
     * @param trackId ID del track
     */
    @POST("albums/{albumId}/tracks/{trackId}")
    suspend fun addTrackToAlbum(
        @Path("albumId") albumId: Int,
        @Path("trackId") trackId: Int
    ): Response<Album>
}
