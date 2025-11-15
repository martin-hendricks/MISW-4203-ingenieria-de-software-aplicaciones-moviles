package com.miso.vinilos.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Entidad que representa un músico
 * Corresponde a la entidad Musician del backend que extiende Performer
 * Incluye todos los campos de Performer más birthDate
 */
@Entity(tableName = "musicians_table")
data class Musician(
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("image")
    val image: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("birthDate")
    val birthDate: Date,
    
    @SerializedName("albums")
    val albums: List<Album>? = null,
    
    @SerializedName("performerPrizes")
    val performerPrizes: List<PerformerPrize>? = null
) {

    /**
     * Retorna el año de nacimiento del músico
     */
    fun getBirthYear(): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = birthDate
        return calendar.get(java.util.Calendar.YEAR)
    }
}

/**
 * DTO para crear un nuevo músico
 */
data class MusicianCreateDTO(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("image")
    val image: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("birthDate")
    val birthDate: Date
)

