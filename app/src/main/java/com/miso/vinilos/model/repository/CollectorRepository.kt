package com.miso.vinilos.model.repository

import android.util.Log
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.CollectorCreateDTO
import com.miso.vinilos.model.network.CollectorApiService
import com.miso.vinilos.model.network.RetrofitClient

/**
 * Repositorio para manejar las operaciones relacionadas con coleccionistas
 * Implementa el patrón Repository para separar la lógica de datos
 *
 * @param apiService Servicio API para coleccionistas (inyectable para testing)
 */
class CollectorRepository(
    private val apiService: CollectorApiService = RetrofitClient.createService()
) {
    
    /**
     * Obtiene todos los coleccionistas
     * @return Result con la lista de coleccionistas o error
     */
    suspend fun getCollectors(): Result<List<Collector>> {
        return try {
            val response = apiService.getCollectors()
            Log.d("CollectorRepository", "getCollectors: Response code=${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful && response.body() != null) {
                val collectors = response.body()!!
                // Log detallado de cada coleccionista para ver qué campos vienen
                collectors.forEachIndexed { index, collector ->
                    Log.d("CollectorRepository", "Collector[$index]: id=${collector.id}, name=${collector.name}, image=${collector.image}, email=${collector.email}, telephone=${collector.telephone}")
                }
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
    
    /**
     * Obtiene un coleccionista específico por ID
     * @param id ID del coleccionista
     * @return Result con el coleccionista o error
     */
    suspend fun getCollector(id: Int): Result<Collector> {
        return try {
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
         * Para testing, se puede pasar una instancia personalizada
         */
        fun getInstance(customInstance: CollectorRepository? = null): CollectorRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: CollectorRepository().also { instance = it }
            }
        }

        /**
         * Resetea la instancia singleton (útil para testing)
         */
        @Synchronized
        fun resetInstance() {
            instance = null
        }
    }
}

