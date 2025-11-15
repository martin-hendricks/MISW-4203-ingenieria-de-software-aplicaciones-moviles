package com.miso.vinilos.model.repository

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.CollectorCreateDTO
import com.miso.vinilos.model.database.dao.CollectorsDao
import com.miso.vinilos.model.network.CollectorApiService
import com.miso.vinilos.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar las operaciones relacionadas con coleccionistas
 * Implementa el patrón Repository para separar la lógica de datos
 * Incluye caché local con Room para mejorar el rendimiento
 *
 * @param application Contexto de la aplicación para acceder a recursos del sistema
 * @param collectorsDao DAO para operaciones de caché local
 * @param apiService Servicio API para coleccionistas (inyectable para testing)
 */
class CollectorRepository(
    private val application: Application,
    private val collectorsDao: CollectorsDao,
    private val apiService: CollectorApiService = RetrofitClient.createService()
) {
    
    /**
     * Obtiene todos los coleccionistas usando estrategia cache-first
     * Primero busca en caché local, si no hay datos consulta la red
     * @return Result con la lista de coleccionistas o error
     */
    suspend fun getCollectors(): Result<List<Collector>> {
        // Intentar obtener datos del caché local (ejecutar en IO dispatcher)
        val cached = withContext(Dispatchers.IO) {
            collectorsDao.getCollectors()
        }

        return if (cached.isNullOrEmpty()) {
            // Si no hay caché, verificar conectividad
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_WIFI &&
                cm.activeNetworkInfo?.type != ConnectivityManager.TYPE_MOBILE) {
                // Sin conexión, retornar lista vacía
                Log.d("CollectorRepository", "getCollectors: Sin conexión, retornando lista vacía")
                Result.success(emptyList())
            } else {
                // Con conexión, obtener de la red
                try {
                    val response = apiService.getCollectors()
                    Log.d("CollectorRepository", "getCollectors: Response code=${response.code()}, isSuccessful=${response.isSuccessful}")
                    if (response.isSuccessful && response.body() != null) {
                        val collectors = response.body()!!
                        // Log detallado de cada coleccionista para ver qué campos vienen
                        collectors.forEachIndexed { index, collector ->
                            Log.d("CollectorRepository", "Collector[$index]: id=${collector.id}, name=${collector.name}, image=${collector.image}, email=${collector.email}, telephone=${collector.telephone}")
                        }
                        // Guardar en caché para uso futuro
                        collectorsDao.insertAll(collectors)
                        Result.success(collectors)
                    } else {
                        Log.e("CollectorRepository", "getCollectors: Error - code=${response.code()}, message=${response.message()}, body=${response.errorBody()?.string()}")
                        Result.failure(Exception("Error al obtener coleccionistas: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    Log.e("CollectorRepository", "getCollectors: Excepción", e)
                    Result.failure(e)
                }
            }
        } else {
            // Retornar datos del caché
            Log.d("CollectorRepository", "getCollectors: Retornando ${cached.size} coleccionistas del caché")
            Result.success(cached)
        }
    }
    
    /**
     * Obtiene un coleccionista específico por ID usando estrategia cache-first
     * @param id ID del coleccionista
     * @return Result con el coleccionista o error
     */
    suspend fun getCollector(id: Int): Result<Collector> {
        // Intentar obtener del caché local (ejecutar en IO dispatcher)
        val cached = withContext(Dispatchers.IO) {
            collectorsDao.getCollector(id)
        }

        return if (cached != null) {
            // Retornar del caché si existe
            Log.d("CollectorRepository", "getCollector($id): Retornando del caché")
            Result.success(cached)
        } else {
            // Si no está en caché, obtener de la red
            try {
                val response = apiService.getCollector(id)
                Log.d("CollectorRepository", "getCollector($id): Response code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val collector = response.body()!!
                    Log.d("CollectorRepository", "getCollector($id): Collector parseado exitosamente - id=${collector.id}, name=${collector.name}")
                    Log.d("CollectorRepository", "getCollector($id): collectorAlbums: ${collector.collectorAlbums?.size ?: 0} álbumes")
                    Log.d("CollectorRepository", "getCollector($id): favoritePerformers: ${collector.favoritePerformers?.size ?: 0} performers")
                    collector.collectorAlbums?.forEachIndexed { idx, ca ->
                        Log.d("CollectorRepository", "collectorAlbum[$idx]: id=${ca.id}, albumId=${ca.albumId}, album=${ca.album}, album?.id=${ca.album?.id}, price=${ca.price}, status=${ca.status}")
                    }
                    // Guardar en caché para uso futuro
                    collectorsDao.insert(collector)
                    Result.success(collector)
                } else {
                    Log.e("CollectorRepository", "getCollector($id): Error - code=${response.code()}, message=${response.message()}, body=${response.errorBody()?.string()}")
                    Result.failure(Exception("Coleccionista no encontrado"))
                }
            } catch (e: Exception) {
                Log.e("CollectorRepository", "getCollector($id): Excepción", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Crea un nuevo coleccionista
     * @param collector Datos del coleccionista a crear
     * @return Result con el coleccionista creado o error
     */
    suspend fun createCollector(collector: CollectorCreateDTO): Result<Collector> {
        return try {
            val response = apiService.createCollector(collector)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear coleccionista: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza un coleccionista existente
     * @param id ID del coleccionista a actualizar
     * @param collector Nuevos datos del coleccionista
     * @return Result con el coleccionista actualizado o error
     */
    suspend fun updateCollector(id: Int, collector: CollectorCreateDTO): Result<Collector> {
        return try {
            val response = apiService.updateCollector(id, collector)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar coleccionista: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un coleccionista
     * @param id ID del coleccionista a eliminar
     * @return Result indicando éxito o error
     */
    suspend fun deleteCollector(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteCollector(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar coleccionista: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var instance: CollectorRepository? = null

        /**
         * Obtiene la instancia singleton del repositorio
         * @param application Contexto de la aplicación
         * @param collectorsDao DAO de coleccionistas
         * @param customInstance Instancia personalizada para testing
         */
        fun getInstance(
            application: Application,
            collectorsDao: CollectorsDao,
            customInstance: CollectorRepository? = null
        ): CollectorRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: CollectorRepository(application, collectorsDao).also { instance = it }
            }
        }
    }
}

