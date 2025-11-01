package com.miso.vinilos.config

import com.google.gson.GsonBuilder
import com.miso.vinilos.model.network.AlbumApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit configurable para pruebas E2E
 * 
 * Permite configurar la URL base para apuntar a MockWebServer
 * en lugar del servidor real
 */
object TestRetrofitClient {
    
    /**
     * Crea un cliente Retrofit configurado para pruebas
     * 
     * @param baseUrl URL base del servidor mock (obtenida de MockWebServerRule)
     * @return Instancia de Retrofit configurada para pruebas
     */
    fun createTestClient(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // Alinear el formato de fechas con el usado por la app y los helpers de test
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Crea un servicio API para pruebas
     * 
     * @param baseUrl URL base del servidor mock
     * @return Instancia de AlbumApiService configurada para pruebas
     */
    fun createTestApiService(baseUrl: String): AlbumApiService {
        val retrofit = createTestClient(baseUrl)
        return retrofit.create(AlbumApiService::class.java)
    }
}
