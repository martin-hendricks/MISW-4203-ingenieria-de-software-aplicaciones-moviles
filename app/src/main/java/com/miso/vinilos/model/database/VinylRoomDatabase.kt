package com.miso.vinilos.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.Comment
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.database.converters.Converters
import com.miso.vinilos.model.database.dao.AlbumsDao
import com.miso.vinilos.model.database.dao.CollectorsDao
import com.miso.vinilos.model.database.dao.CommentsDao
import com.miso.vinilos.model.database.dao.MusiciansDao

/**
 * Base de datos Room para la aplicación Vinilos
 * Singleton que contiene todas las entidades y DAOs
 */
@Database(
    entities = [Album::class, Collector::class, Musician::class, Comment::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VinylRoomDatabase : RoomDatabase() {

    /**
     * DAO para operaciones de álbumes
     */
    abstract fun albumsDao(): AlbumsDao

    /**
     * DAO para operaciones de coleccionistas
     */
    abstract fun collectorsDao(): CollectorsDao

    /**
     * DAO para operaciones de músicos
     */
    abstract fun musiciansDao(): MusiciansDao

    /**
     * DAO para operaciones de comentarios
     */
    abstract fun commentsDao(): CommentsDao

    companion object {
        /**
         * Singleton prevents multiple instances of database opening at the same time.
         */
        @Volatile
        private var INSTANCE: VinylRoomDatabase? = null

        /**
         * Obtiene la instancia singleton de la base de datos
         * @param context Contexto de la aplicación
         * @return Instancia de VinylRoomDatabase
         */
        fun getDatabase(context: Context): VinylRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VinylRoomDatabase::class.java,
                    "vinyls_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
