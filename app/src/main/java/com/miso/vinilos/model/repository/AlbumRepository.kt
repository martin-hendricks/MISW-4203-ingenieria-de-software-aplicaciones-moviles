@file:Suppress("SameParameterValue")

package com.miso.vinilos.model.repository

import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.AlbumCreateDTO
import com.miso.vinilos.model.data.Track
import com.miso.vinilos.model.network.AlbumApiService
import com.miso.vinilos.model.network.RetrofitClient

/**
 * Repositorio para manejar las operaciones relacionadas con álbumes
 * Implementa el patrón Repository para separar la lógica de datos
 *
 * @param apiService Servicio API para álbumes (inyectable para testing)
 */
class AlbumRepository(
    private val apiService: AlbumApiService = RetrofitClient.createService()
) {
    
    /**
     * Obtiene todos los álbumes
     * @return Result con la lista de álbumes o error
     */
    suspend fun getAlbums(): Result<List<Album>> {
        return try {
            val response = apiService.getAlbums()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener álbumes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene un álbum específico por ID
     * @param id ID del álbum
     * @return Result con el álbum o error
     */
    suspend fun getAlbum(id: Int): Result<Album> {
        return try {
            val response = apiService.getAlbum(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Álbum no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Crea un nuevo álbum
     * @param album Datos del álbum a crear
     * @return Result con el álbum creado o error
     */
    suspend fun createAlbum(album: AlbumCreateDTO): Result<Album> {
        return try {
            val response = apiService.createAlbum(album)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear álbum: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza un álbum existente
     * @param id ID del álbum a actualizar
     * @param album Nuevos datos del álbum
     * @return Result con el álbum actualizado o error
     */
    suspend fun updateAlbum(id: Int, album: AlbumCreateDTO): Result<Album> {
        return try {
            val response = apiService.updateAlbum(id, album)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar álbum: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los tracks de un álbum
     * @param albumId ID del álbum
     * @return Result con la lista de tracks o error
     */
    suspend fun getAlbumTracks(albumId: Int): Result<List<Track>> {
        return try {
            val response = apiService.getAlbumTracks(albumId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener tracks: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Agrega un track a un álbum
     * @param albumId ID del álbum
     * @param trackId ID del track
     * @return Result con el álbum actualizado o error
     */
    suspend fun addTrackToAlbum(albumId: Int, trackId: Int): Result<Album> {
        return try {
            val response = apiService.addTrackToAlbum(albumId, trackId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al agregar track: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var instance: AlbumRepository? = null

        /**
         * Obtiene la instancia singleton del repositorio
         * Para testing, se puede pasar una instancia personalizada
         */
        fun getInstance(customInstance: AlbumRepository? = null): AlbumRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: AlbumRepository().also { instance = it }
            }
        }

    }
}
