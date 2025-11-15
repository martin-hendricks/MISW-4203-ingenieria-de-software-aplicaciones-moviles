package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName

/**
 * Enumeración de sellos discográficos disponibles
 */
enum class RecordLabel(val displayName: String) {
    @SerializedName("Sony Music")
    SONY("Sony Music"),
    
    @SerializedName("EMI")
    EMI("EMI"),
    
    @SerializedName("Discos Fuentes")
    FUENTES("Discos Fuentes"),
    
    @SerializedName("Elektra")
    ELEKTRA("Elektra"),
    
    @SerializedName("Fania Records")
    FANIA("Fania Records");

}
