package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName
import java.util.Calendar
import java.util.Date

/**
 * Entidad que representa un álbum musical
 * Corresponde a la entidad Album del backend
 */
data class Album(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("cover")
    val cover: String,
    
    @SerializedName("releaseDate")
    val releaseDate: Date,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("genre")
    val genre: Genre,
    
    @SerializedName("recordLabel")
    val recordLabel: RecordLabel,
    
    @SerializedName("tracks")
    val tracks: List<Track>? = null,
    
    @SerializedName("performers")
    val performers: List<Performer>? = null,
    
    @SerializedName("comments")
    val comments: List<Comment>? = null
) {
    /**
     * Retorna el año de lanzamiento del álbum
     */
    fun getReleaseYear(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = releaseDate
        return calendar.get(Calendar.YEAR)
    }
    
    /**
     * Retorna la cantidad de tracks del álbum
     */
    fun getTracksCount(): Int {
        return tracks?.size ?: 0
    }
    
    /**
     * Retorna la cantidad de comentarios del álbum
     */
    fun getCommentsCount(): Int {
        return comments?.size ?: 0
    }
}

/**
 * DTO para crear un nuevo álbum
 * No incluye ID ya que se genera automáticamente
 */
data class AlbumCreateDTO(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("cover")
    val cover: String,
    
    @SerializedName("releaseDate")
    val releaseDate: Date,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("genre")
    val genre: Genre,
    
    @SerializedName("recordLabel")
    val recordLabel: RecordLabel
)
