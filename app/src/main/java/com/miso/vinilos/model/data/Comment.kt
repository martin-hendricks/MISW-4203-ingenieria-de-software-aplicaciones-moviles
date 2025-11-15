package com.miso.vinilos.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Entidad que representa un comentario sobre un álbum
 */
@Entity(tableName = "comments_table")
data class Comment(
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("rating")
    val rating: Int
)

