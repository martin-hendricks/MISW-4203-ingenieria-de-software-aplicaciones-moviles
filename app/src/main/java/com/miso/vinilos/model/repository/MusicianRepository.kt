package com.miso.vinilos.model.repository

import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.MusicianCreateDTO
import com.miso.vinilos.model.network.MusicianApiService
import com.miso.vinilos.model.network.RetrofitClient

/**
 * Repositorio para manejar las operaciones relacionadas con músicos
 * Implementa el patrón Repository para separar la lógica de datos
 *
 * @param apiService Servicio API para músicos (inyectable para testing)
 */
class MusicianRepository(
    private val apiService: MusicianApiService = RetrofitClient.createService()
) {
    
    /**
     * Obtiene todos los músicos
     * @return Result con la lista de músicos o error
     */
    suspend fun getMusicians(): Result<List<Musician>> {
        return try {
            val response = apiService.getMusicians()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener músicos: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene un músico específico por ID
     * @param id ID del músico
     * @return Result con el músico o error
     */
    suspend fun getMusician(id: Int): Result<Musician> {
        return try {
            val response = apiService.getMusician(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Músico no encontrado"))
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
    
    companion object {
        @Volatile
        private var instance: MusicianRepository? = null

        /**
         * Obtiene la instancia singleton del repositorio
         * Para testing, se puede pasar una instancia personalizada
         */
        fun getInstance(customInstance: MusicianRepository? = null): MusicianRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: MusicianRepository().also { instance = it }
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

