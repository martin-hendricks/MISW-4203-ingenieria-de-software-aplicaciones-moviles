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

}

