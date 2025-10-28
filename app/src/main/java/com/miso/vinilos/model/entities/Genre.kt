package com.miso.vinilos.model.entities

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
        /**
         * Obtiene el enum a partir del nombre de display
         */
        fun fromDisplayName(name: String): Genre? {
            return values().find { it.displayName == name }
        }
    }
}
