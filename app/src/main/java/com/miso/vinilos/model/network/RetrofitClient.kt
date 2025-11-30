package com.miso.vinilos.model.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Objeto singleton que proporciona la instancia de Retrofit
 */
object RetrofitClient {
    
    /**
     * Cliente OkHttp con configuración de logging y timeouts
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(NetworkConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Gson configurado para manejar fechas en formato ISO 8601 con timezone offset
     * Usa DateTypeAdapter personalizado para formatear fechas como "2011-08-01T00:00:00-05:00"
     */
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .setLenient()
            .create()
    }
    
    /**
     * Instancia de Retrofit configurada con la URL base y convertidor Gson
     */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Función helper para crear servicios de API
     * Uso: val apiService = RetrofitClient.createService<ApiService>()
     */
    inline fun <reified T> createService(): T {
        return retrofit.create(T::class.java)
    }
}

