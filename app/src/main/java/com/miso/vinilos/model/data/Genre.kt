package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName

/**
 * Enumeración de géneros musicales disponibles
 */
enum class Genre(val displayName: String) {
    @SerializedName("Classical")
    CLASSICAL("Classical"),
    
    @SerializedName("Salsa")
    SALSA("Salsa"),
    
    @SerializedName("Rock")
    ROCK("Rock"),
    
    @SerializedName("Folk")
    FOLK("Folk");

    companion object {
    }
}
