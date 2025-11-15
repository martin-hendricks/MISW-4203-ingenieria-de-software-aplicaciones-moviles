package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName

/**
 * Entidad base que representa un performer (músico o banda)
 * Corresponde a la entidad Performer del backend
 */
data class Performer(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("image")
    val image: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("albums")
    val albums: List<Album>? = null
)
