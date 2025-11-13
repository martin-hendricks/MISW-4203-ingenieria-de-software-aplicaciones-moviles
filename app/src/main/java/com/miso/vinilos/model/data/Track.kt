package com.miso.vinilos.model.data

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

}

