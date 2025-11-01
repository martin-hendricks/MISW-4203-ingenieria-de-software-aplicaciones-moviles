package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName

/**
 * Entidad que representa un comentario sobre un álbum
 */
data class Comment(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("rating")
    val rating: Int
) {
    /**
     * Valida si el rating está en el rango permitido (1-5)
     */
    fun isValidRating(): Boolean {
        return rating in 1..5
    }
    
    /**
     * Retorna el rating como float para componentes de UI
     */
    fun getRatingAsFloat(): Float {
        return rating.toFloat()
    }
}

/**
 * DTO para crear un nuevo comentario
 */
data class CommentCreateDTO(
    @SerializedName("description")
    val description: String,
    
    @SerializedName("rating")
    val rating: Int
)
