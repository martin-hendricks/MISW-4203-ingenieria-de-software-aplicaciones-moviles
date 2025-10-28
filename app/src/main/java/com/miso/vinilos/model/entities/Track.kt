package com.miso.vinilos.model.entities

import com.google.gson.annotations.SerializedName

/**
 * Entidad que representa un track o canción de un álbum
 */
data class Track(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("duration")
    val duration: String
) {
    /**
     * Convierte la duración de formato "mm:ss" a segundos
     */
    fun getDurationInSeconds(): Int {
        return try {
            val parts = duration.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toInt()
                val seconds = parts[1].toInt()
                minutes * 60 + seconds
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Formatea la duración para mostrar
     */
    fun getFormattedDuration(): String {
        return duration
    }
}

/**
 * DTO para crear un nuevo track
 */
data class TrackCreateDTO(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("duration")
    val duration: String
)
