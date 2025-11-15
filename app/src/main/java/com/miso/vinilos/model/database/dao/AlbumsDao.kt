package com.miso.vinilos.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.miso.vinilos.model.data.Album

/**
 * DAO (Data Access Object) para la entidad Album
 * Define las operaciones de base de datos para álbumes
 */
@Dao
interface AlbumsDao {

    /**
     * Obtiene todos los álbumes de la base de datos
     * @return Lista de todos los álbumes
     */
    @Query("SELECT * FROM albums_table")
    fun getAlbums(): List<Album>

    /**
     * Obtiene un álbum específico por su ID
     * @param albumId ID del álbum a buscar
     * @return El álbum encontrado o null
     */
    @Query("SELECT * FROM albums_table WHERE id = :albumId")
    fun getAlbum(albumId: Int): Album?

    /**
     * Inserta un nuevo álbum en la base de datos
     * Si el álbum ya existe (mismo ID), se ignora la inserción
     * @param album Álbum a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album)

    /**
     * Inserta múltiples álbumes en la base de datos
     * @param albums Lista de álbumes a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<Album>)

    /**
     * Elimina todos los álbumes de la base de datos
     * @return Número de filas eliminadas
     */
    @Query("DELETE FROM albums_table")
    suspend fun deleteAll(): Int
}
