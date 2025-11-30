package com.miso.vinilos.model.network

import android.annotation.SuppressLint
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * TypeAdapter personalizado para serializar/deserializar fechas en formato ISO 8601
 * con timezone offset (ej: "2011-08-01T00:00:00-05:00")
 * 
 * El API espera fechas en formato: yyyy-MM-dd'T'HH:mm:ssXXX
 * donde XXX es el offset de timezone (ej: -05:00 para Colombia)
 */
class DateTypeAdapter : TypeAdapter<Date>() {
    
    // Formato para serializar (enviar al servidor)
    @SuppressLint("NewApi")
    private val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    
    // Formatos para deserializar (recibir del servidor) - múltiples formatos posibles
    private val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        @SuppressLint("NewApi")
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )
    
    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
        } else {
            // Formatear la fecha con timezone local
            val formatted = outputFormat.format(value)
            out.value(formatted)
        }
    }
    
    override fun read(`in`: JsonReader): Date? {
        val dateString = `in`.nextString() ?: return null
        
        // Intentar parsear con cada formato hasta que uno funcione
        for (format in inputFormats) {
            try {
                return format.parse(dateString)
            } catch (e: Exception) {
                // Continuar con el siguiente formato
            }
        }
        
        // Si ningún formato funciona, lanzar excepción
        throw IllegalArgumentException("No se pudo parsear la fecha: $dateString")
    }
}

