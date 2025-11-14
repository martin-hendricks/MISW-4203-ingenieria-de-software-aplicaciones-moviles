package com.miso.vinilos.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.miso.vinilos.model.data.Collector

/**
 * DAO (Data Access Object) para la entidad Collector
 * Define las operaciones de base de datos para coleccionistas
 */
@Dao
interface CollectorsDao {

    /**
     * Obtiene todos los coleccionistas de la base de datos
     * @return Lista de todos los coleccionistas
     */
    @Query("SELECT * FROM collectors_table")
    fun getCollectors(): List<Collector>

    /**
     * Obtiene un coleccionista específico por su ID
     * @param collectorId ID del coleccionista a buscar
     * @return El coleccionista encontrado o null
     */
    @Query("SELECT * FROM collectors_table WHERE id = :collectorId")
    fun getCollector(collectorId: Int): Collector?

    /**
     * Inserta un nuevo coleccionista en la base de datos
     * Si el coleccionista ya existe (mismo ID), se reemplaza
     * @param collector Coleccionista a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collector: Collector)

    /**
     * Inserta múltiples coleccionistas en la base de datos
     * @param collectors Lista de coleccionistas a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collectors: List<Collector>)

    /**
     * Elimina todos los coleccionistas de la base de datos
     * @return Número de filas eliminadas
     */
    @Query("DELETE FROM collectors_table")
    suspend fun deleteAll(): Int
}
