package com.miso.vinilos.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry

/**
 * Utilidades para manejo de corrutinas y operaciones asíncronas
 */
object CoroutineUtils {

    /**
     * Ejecuta una operación con retry automático usando backoff exponencial
     *
     * @param maxAttempts Número máximo de intentos
     * @param initialDelay Delay inicial entre reintentos (en ms)
     * @param maxDelay Delay máximo entre reintentos (en ms)
     * @param factor Factor multiplicador para backoff exponencial
     * @param block Bloque de código a ejecutar
     * @return Resultado de la operación
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                // Si es el último intento, lanzar la excepción
                if (attempt == maxAttempts - 2) throw e

                // Log del error y esperar antes de reintentar
                android.util.Log.w("CoroutineUtils", "Intento ${attempt + 1} falló, reintentando en ${currentDelay}ms", e)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // Último intento
    }

    /**
     * Crea un Flow con retry automático y manejo de errores
     *
     * @param maxRetries Número máximo de reintentos
     * @param block Bloque que produce el valor
     * @return Flow con retry automático
     */
    fun <T> flowWithRetry(
        maxRetries: Long = 3,
        block: suspend () -> T
    ): Flow<Result<T>> = flow {
        try {
            val result = block()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.retry(maxRetries) { cause ->
        android.util.Log.w("CoroutineUtils", "Flow retry debido a: ${cause.message}")
        delay(1000)
        isRetryableException(cause)
    }.catch { e ->
        emit(Result.failure(e))
    }

    /**
     * Determina si una excepción es recuperable
     */
    private fun isRetryableException(throwable: Throwable): Boolean {
        return throwable.message?.let { message ->
            message.contains("timeout", ignoreCase = true) ||
            message.contains("Failed to connect", ignoreCase = true) ||
            message.contains("SocketTimeout", ignoreCase = true) ||
            message.contains("UnknownHost", ignoreCase = true)
        } ?: false
    }

    /**
     * Ejecuta múltiples operaciones y retorna solo las exitosas
     * Útil cuando se necesita cargar múltiples recursos y algunos pueden fallar
     *
     * @param operations Lista de operaciones a ejecutar
     * @return Lista de resultados exitosos
     */
    suspend fun <T> executeAllIgnoreFailures(
        operations: List<suspend () -> T>
    ): List<T> {
        return operations.mapNotNull { operation ->
            try {
                operation()
            } catch (e: Exception) {
                android.util.Log.w("CoroutineUtils", "Operación falló, ignorando: ${e.message}")
                null
            }
        }
    }
}

