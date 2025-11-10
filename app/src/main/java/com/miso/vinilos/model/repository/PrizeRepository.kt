package com.miso.vinilos.model.repository

import com.miso.vinilos.model.data.Prize
import com.miso.vinilos.model.data.PrizeCreateDTO
import com.miso.vinilos.model.network.PrizeApiService
import com.miso.vinilos.model.network.RetrofitClient

/**
 * Repositorio para manejar las operaciones relacionadas con premios
 * Implementa el patrón Repository para separar la lógica de datos
 *
 * @param apiService Servicio API para premios (inyectable para testing)
 */
class PrizeRepository(
    private val apiService: PrizeApiService = RetrofitClient.createService()
) {
    
    /**
     * Obtiene todos los premios
     * @return Result con la lista de premios o error
     */
    suspend fun getPrizes(): Result<List<Prize>> {
        return try {
            val response = apiService.getPrizes()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener premios: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene un premio específico por ID
     * @param id ID del premio
     * @return Result con el premio o error
     */
    suspend fun getPrize(id: Int): Result<Prize> {
        return try {
            val response = apiService.getPrize(id)
            android.util.Log.d("PrizeRepository", "getPrize($id): Response code=${response.code()}, isSuccessful=${response.isSuccessful()}")
            if (response.isSuccessful && response.body() != null) {
                val prize = response.body()!!
                android.util.Log.d("PrizeRepository", "getPrize($id): Premio parseado exitosamente - id=${prize.id}, name=${prize.name}")
                Result.success(prize)
            } else {
                android.util.Log.e("PrizeRepository", "getPrize($id): Error - code=${response.code()}, message=${response.message()}, body=${response.errorBody()?.string()}")
                Result.failure(Exception("Premio no encontrado: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("PrizeRepository", "getPrize($id): Excepción", e)
            Result.failure(e)
        }
    }
    
    /**
     * Crea un nuevo premio
     * @param prize Datos del premio a crear
     * @return Result con el premio creado o error
     */
    suspend fun createPrize(prize: PrizeCreateDTO): Result<Prize> {
        return try {
            val response = apiService.createPrize(prize)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear premio: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza un premio existente
     * @param id ID del premio a actualizar
     * @param prize Nuevos datos del premio
     * @return Result con el premio actualizado o error
     */
    suspend fun updatePrize(id: Int, prize: PrizeCreateDTO): Result<Prize> {
        return try {
            val response = apiService.updatePrize(id, prize)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar premio: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un premio
     * @param id ID del premio a eliminar
     * @return Result indicando éxito o error
     */
    suspend fun deletePrize(id: Int): Result<Unit> {
        return try {
            val response = apiService.deletePrize(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar premio: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var instance: PrizeRepository? = null

        /**
         * Obtiene la instancia singleton del repositorio
         * Para testing, se puede pasar una instancia personalizada
         */
        fun getInstance(customInstance: PrizeRepository? = null): PrizeRepository {
            if (customInstance != null) {
                return customInstance
            }
            return instance ?: synchronized(this) {
                instance ?: PrizeRepository().also { instance = it }
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

