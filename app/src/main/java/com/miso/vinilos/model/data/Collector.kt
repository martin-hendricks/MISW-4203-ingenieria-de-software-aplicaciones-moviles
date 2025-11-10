package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName

/**
 * Entidad que representa un coleccionista
 * Corresponde a la entidad Collector del backend
 */
data class Collector(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("telephone")
    val telephone: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("image")
    val image: String? = null,
    
    @SerializedName("comments")
    val comments: List<Any>? = null,
    
    @SerializedName("favoritePerformers")
    val favoritePerformers: List<Performer>? = null,
    
    @SerializedName("collectorAlbums")
    val collectorAlbums: List<CollectorAlbum>? = null
)

/**
 * Entidad que representa un álbum en la colección de un coleccionista
 * Corresponde a la entidad CollectorAlbum del backend
 */
data class CollectorAlbum(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("price")
    val price: Int,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("album")
    val album: Album? = null,
    
    @SerializedName("albumId")
    val albumId: Int? = null
)

/**
 * DTO para crear un nuevo coleccionista
 */
data class CollectorCreateDTO(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("telephone")
    val telephone: String,
    
    @SerializedName("email")
    val email: String
)

