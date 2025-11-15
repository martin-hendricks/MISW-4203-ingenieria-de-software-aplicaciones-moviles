package com.miso.vinilos

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.miso.vinilos.model.database.VinylRoomDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Application class para configurar componentes globales de la app
 */
class VinilosApplication : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "VinilosApplication"
    }

    /**
     * Instancia lazy de la base de datos Room
     * Se inicializa solo cuando se accede por primera vez
     */
    val database by lazy { VinylRoomDatabase.getDatabase(this) }

    /**
     * Configura el ImageLoader de Coil con soporte mejorado para PNG y otros formatos
     * Coil 2.7.0 ya incluye soporte para PNG, JPG, GIF, WebP, etc. por defecto
     * Configurado para manejar mejor URLs externas como Wikipedia
     */
    override fun newImageLoader(): ImageLoader {
        // Configurar logging interceptor para debugging
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, "Coil HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Interceptor para agregar User-Agent (algunos servidores como Wikipedia lo requieren)
        val userAgentInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", "VinilosApp/1.0 (Android)")
                .build()
            chain.proceed(requestWithUserAgent)
        }
        
        // Configurar OkHttpClient para Coil con timeouts más largos y retry
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Timeout más largo para imágenes grandes
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true) // Reintentar en caso de fallo de conexión
            .build()
        
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .respectCacheHeaders(true) // Respetar headers de cache para mejor rendimiento
            .crossfade(true)
            .crossfade(300) // Duración del crossfade
            .allowHardware(false) // Deshabilitar hardware acceleration para mejor compatibilidad
            .build()
    }
}

