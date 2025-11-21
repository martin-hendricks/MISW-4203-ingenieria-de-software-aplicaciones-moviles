package com.miso.vinilos.model.repository

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.MusicianCreateDTO
import com.miso.vinilos.model.database.dao.MusiciansDao
import com.miso.vinilos.model.network.MusicianApiService
import com.miso.vinilos.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar las operaciones relacionadas con músicos
 * Implementa el patrón Repository para separar la lógica de datos
 * Incluye caché local con Room para mejorar el rendimiento
 *
 * @param application Contexto de la aplicación para acceder a recursos del sistema
 * @param musiciansDao DAO para operaciones de caché local
 * @param apiService Servicio API para músicos (inyectable para testing)
 */
class MusicianRepository(
    private val application: Application,
    private val musiciansDao: MusiciansDao,
    private val apiService: MusicianApiService = RetrofitClient.createService()
) {

    /**
     * Obtiene todos los músicos usando estrategia cache-first
     * Primero busca en caché local, si no hay datos consulta la red
     * @return Result con la lista de músicos o error
     */
    suspend fun getMusicians(): Result<List<Musician>> {
        // Intentar obtener datos del caché local (ejecutar en IO dispatcher)
        val cached = withContext(Dispatchers.IO) {
            musiciansDao.getMusicians()
        }

        return if (cached.isNullOrEmpty()) {
            // Si no hay caché, verificar conectividad
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_WIFI &&
                cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_MOBILE) {
                // Sin conexión, retornar lista vacía
                Result.success(emptyList())
            } else {
                // Con conexión, obtener de la red
                try {
                    val response = apiService.getMusicians()
                    if (response.isSuccessful && response.body() != null) {
                        val musicians = response.body()!!
                        // Guardar en caché para uso futuro
                        musiciansDao.insertAll(musicians)
                        Result.success(musicians)
                    } else {
                        Result.failure(Exception("Error al obtener músicos: ${response.code()}"))
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
     * Obtiene un músico específico por ID usando estrategia cache-first
     * @param id ID del músico
     * @return Result con el músico o error
     */
    suspend fun getMusician(id: Int): Result<Musician> {
        // Intentar obtener del caché local (ejecutar en IO dispatcher)
        val cached = withContext(Dispatchers.IO) {
            musiciansDao.getMusician(id)
        }

        return if (cached != null) {
            // Retornar del caché si existe
            Result.success(cached)
        } else {
            // Si no está en caché, obtener de la red
            try {
                val response = apiService.getMusician(id)
                if (response.isSuccessful && response.body() != null) {
                    val musician = response.body()!!
                    // Guardar en caché para uso futuro
                    musiciansDao.insert(musician)
                    Result.success(musician)
                } else {
                    Result.failure(Exception("Músico no encontrado"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Refresca un músico específico desde la red, forzando la actualización del caché
     * Siempre consulta la red y actualiza el caché local
     * @param id ID del músico
     * @return Result con el músico o error
     */
    suspend fun refreshMusician(id: Int): Result<Musician> {
        return try {
            // Verificar conectividad
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_WIFI &&
                cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_MOBILE) {
                // Sin conexión, retornar error
                Result.failure(Exception("No hay conexión a internet"))
            } else {
                // Siempre obtener de la red
                val response = apiService.getMusician(id)
                if (response.isSuccessful && response.body() != null) {
                    val musician = response.body()!!
                    // Actualizar caché con los datos más recientes
                    withContext(Dispatchers.IO) {
                        musiciansDao.insert(musician)
                    }
                    Result.success(musician)
                } else {
                    Result.failure(Exception("Error al obtener músico: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Crea un nuevo músico
     * @param musician Datos del músico a crear
     * @return Result con el músico creado o error
     */
    suspend fun createMusician(musician: MusicianCreateDTO): Result<Musician> {
        return try {
            val response = apiService.createMusician(musician)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear músico: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza un músico existente
     * @param id ID del músico a actualizar
     * @param musician Nuevos datos del músico
     * @return Result con el músico actualizado o error
     */
    suspend fun updateMusician(id: Int, musician: MusicianCreateDTO): Result<Musician> {
        return try {
            val response = apiService.updateMusician(id, musician)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar músico: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un músico
     * @param id ID del músico a eliminar
     * @return Result indicando éxito o error
     */
    suspend fun deleteMusician(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteMusician(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar músico: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Agrega un álbum a un músico
     * @param musicianId ID del músico
     * @param albumId ID del álbum
     * @return Result indicando éxito o error
     */
    suspend fun addAlbumToMusician(musicianId: Int, albumId: Int): Result<Unit> {
        return try {
            val response = apiService.addAlbumToMusician(musicianId, albumId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al agregar álbum al músico: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var instance: MusicianRepository? = null

        /**
         * Obtiene la instancia singleton del repositorio
         * @param application Contexto de la aplicación
         * @param musiciansDao DAO de músicos
         * @param customInstance Instancia personalizada para testing
         */
        fun getInstance(
            application: Application,
            musiciansDao: MusiciansDao,
            customInstance: MusicianRepository? = null
        ): MusicianRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: MusicianRepository(application, musiciansDao).also { instance = it }
            }
        }
    }
}

