package com.miso.vinilos.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.miso.vinilos.model.data.Comment

/**
 * DAO (Data Access Object) para la entidad Comment
 * Define las operaciones de base de datos para comentarios
 */
@Dao
interface CommentsDao {

    /**
     * Obtiene todos los comentarios de la base de datos
     * @return Lista de todos los comentarios o null si la tabla está vacía
     */
    @Query("SELECT * FROM comments_table")
    fun getComments(): List<Comment>?

    /**
     * Obtiene un comentario específico por su ID
     * @param commentId ID del comentario a buscar
     * @return El comentario encontrado o null
     */
    @Query("SELECT * FROM comments_table WHERE id = :commentId")
    fun getComment(commentId: Int): Comment?

    /**
     * Inserta un nuevo comentario en la base de datos
     * Si el comentario ya existe (mismo ID), se reemplaza
     * @param comment Comentario a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    /**
     * Inserta múltiples comentarios en la base de datos
     * @param comments Lista de comentarios a insertar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<Comment>)

    /**
     * Elimina todos los comentarios de la base de datos
     * @return Número de filas eliminadas
     */
    @Query("DELETE FROM comments_table")
    suspend fun deleteAll(): Int
}
