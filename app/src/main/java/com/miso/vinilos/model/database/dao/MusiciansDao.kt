package com.miso.vinilos.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.miso.vinilos.model.data.Musician

/**
 * DAO (Data Access Object) para la entidad Musician
 * Define las operaciones de base de datos para músicos
 */
@Dao
interface MusiciansDao {

    /**
     * Obtiene todos los músicos de la base de datos
     * @return Lista de todos los músicos
     */
    @Query("SELECT * FROM musicians_table")
    fun getMusicians(): List<Musician>

    /**
     * Obtiene un músico específico por su ID
     * @param musicianId ID del músico a buscar
     * @return El músico encontrado o null
     */
    @Query("SELECT * FROM musicians_table WHERE id = :musicianId")
    fun getMusician(musicianId: Int): Musician?

    /**
     * Inserta un nuevo músico en la base de datos
     * Si el músico ya existe (mismo ID), se reemplaza
     * @param musician Músico a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(musician: Musician)

    /**
     * Inserta múltiples músicos en la base de datos
     * @param musicians Lista de músicos a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(musicians: List<Musician>)

    /**
     * Elimina todos los músicos de la base de datos
     * @return Número de filas eliminadas
     */
    @Query("DELETE FROM musicians_table")
    suspend fun deleteAll(): Int
}
