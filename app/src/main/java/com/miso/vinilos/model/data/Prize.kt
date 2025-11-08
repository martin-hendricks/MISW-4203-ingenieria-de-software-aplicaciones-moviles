package com.miso.vinilos.model.data

import com.google.gson.annotations.SerializedName

/**
 * Entidad que representa un premio
 * Corresponde a la entidad Prize del backend
 */
data class Prize(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("organization")
    val organization: String,
    
    @SerializedName("performerPrizes")
    val performerPrizes: List<Any>? = null
)

/**
 * DTO para crear un nuevo premio
 */
data class PrizeCreateDTO(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("organization")
    val organization: String
)

/**
 * Entidad que representa la relación entre un performer y un premio
 * Corresponde a la entidad PerformerPrize del backend
 */
data class PerformerPrize(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("premiationDate")
    val premiationDate: String?,
    
    @SerializedName("prize")
    val prize: Prize?,
    
    @SerializedName("prizeId")
    val prizeId: Int? = null
)

