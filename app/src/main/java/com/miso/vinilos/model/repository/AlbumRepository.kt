@file:Suppress("SameParameterValue")

package com.miso.vinilos.model.repository

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.AlbumCreateDTO
import com.miso.vinilos.model.data.Track
import com.miso.vinilos.model.database.dao.AlbumsDao
import com.miso.vinilos.model.network.AlbumApiService
import com.miso.vinilos.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar las operaciones relacionadas con álbumes
 * Implementa el patrón Repository para separar la lógica de datos
 * Incluye caché local con Room para mejorar el rendimiento
 *
 * @param application Contexto de la aplicación para acceder a recursos del sistema
 * @param albumsDao DAO para operaciones de caché local
 * @param apiService Servicio API para álbumes (inyectable para testing)
 */
class AlbumRepository(
    private val application: Application,
    private val albumsDao: AlbumsDao,
    private val apiService: AlbumApiService = RetrofitClient.createService()
) {

    /**
     * Obtiene todos los álbumes usando estrategia cache-first
     * Primero busca en caché local, si no hay datos consulta la red
     * @return Result con la lista de álbumes o error
     */
    suspend fun getAlbums(): Result<List<Album>> {
        // Intentar obtener datos del caché local (ejecutar en IO dispatcher)
        val cached = withContext(Dispatchers.IO) {
            albumsDao.getAlbums()
        }

        return if (cached.isNullOrEmpty()) {
            // Si no hay caché, verificar conectividad
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_WIFI &&
                cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_MOBILE) {
                // Sin conexión, retornar lista vacía
                Result.success(emptyList())
            } else {
                // Con conexión, obtener de la red y guardar en caché
                try {
                    val response = apiService.getAlbums()
                    if (response.isSuccessful && response.body() != null) {
                        val albums = response.body()!!
                        // Guardar en caché para uso futuro
                        albumsDao.insertAll(albums)
                        Result.success(albums)
                    } else {
                        Result.failure(Exception("Error al obtener álbumes: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } else {
            // Retornar datos del caché
            Result.success(cached)
        }
    }
    
    /**
     * Refresca la lista de álbumes desde la red, forzando la actualización del caché
     * Siempre consulta la red y actualiza el caché local
     * @return Result con la lista de álbumes o error
     */
    suspend fun refreshAlbums(): Result<List<Album>> {
        return try {
            // Verificar conectividad
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_WIFI &&
                cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_MOBILE) {
                // Sin conexión, retornar error
                Result.failure(Exception("No hay conexión a internet"))
            } else {
                // Siempre obtener de la red
                val response = apiService.getAlbums()
                if (response.isSuccessful && response.body() != null) {
                    val albums = response.body()!!
                    // Limpiar caché anterior y guardar nuevos datos
                    withContext(Dispatchers.IO) {
                        albumsDao.deleteAll()
                        albumsDao.insertAll(albums)
                    }
                    Result.success(albums)
                } else {
                    Result.failure(Exception("Error al obtener álbumes: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene un álbum específico por ID usando estrategia cache-first
     * @param id ID del álbum
     * @return Result con el álbum o error
     */
    suspend fun getAlbum(id: Int): Result<Album> {
        // Intentar obtener del caché local (ejecutar en IO dispatcher)
        val cached = withContext(Dispatchers.IO) {
            albumsDao.getAlbum(id)
        }

        return if (cached != null) {
            // Retornar del caché si existe
            Result.success(cached)
        } else {
            // Si no está en caché, obtener de la red
            try {
                val response = apiService.getAlbum(id)
                if (response.isSuccessful && response.body() != null) {
                    val album = response.body()!!
                    // Guardar en caché para uso futuro
                    albumsDao.insert(album)
                    Result.success(album)
                } else {
                    Result.failure(Exception("Álbum no encontrado"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
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
                val createdAlbum = response.body()!!
                // Insertar el nuevo álbum en el caché para que aparezca inmediatamente
                withContext(Dispatchers.IO) {
                    albumsDao.insert(createdAlbum)
                }
                Result.success(createdAlbum)
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
         * @param application Contexto de la aplicación
         * @param albumsDao DAO de álbumes
         * @param customInstance Instancia personalizada para testing
         */
        fun getInstance(
            application: Application,
            albumsDao: AlbumsDao,
            customInstance: AlbumRepository? = null
        ): AlbumRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: AlbumRepository(application, albumsDao).also { instance = it }
            }
        }
    }
}
