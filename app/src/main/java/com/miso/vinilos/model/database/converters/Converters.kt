package com.miso.vinilos.model.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.miso.vinilos.model.data.*
import java.util.Date

/**
 * Type Converters para Room
 * Convierte tipos complejos a tipos que Room puede almacenar en SQLite
 */
class Converters {

    private val gson = Gson()

    // Converters para Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Converters para Genre enum
    @TypeConverter
    fun fromGenre(genre: Genre?): String? {
        return genre?.name
    }

    @TypeConverter
    fun toGenre(value: String?): Genre? {
        return value?.let { Genre.valueOf(it) }
    }

    // Converters para RecordLabel enum
    @TypeConverter
    fun fromRecordLabel(recordLabel: RecordLabel?): String? {
        return recordLabel?.name
    }

    @TypeConverter
    fun toRecordLabel(value: String?): RecordLabel? {
        return value?.let { RecordLabel.valueOf(it) }
    }

    // Converters para List<Track>
    @TypeConverter
    fun fromTrackList(value: List<Track>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTrackList(value: String?): List<Track>? {
        val listType = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Converters para List<Performer>
    @TypeConverter
    fun fromPerformerList(value: List<Performer>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPerformerList(value: String?): List<Performer>? {
        val listType = object : TypeToken<List<Performer>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Converters para List<Comment>
    @TypeConverter
    fun fromCommentList(value: List<Comment>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCommentList(value: String?): List<Comment>? {
        val listType = object : TypeToken<List<Comment>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Converters para List<CollectorAlbum>
    @TypeConverter
    fun fromCollectorAlbumList(value: List<CollectorAlbum>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCollectorAlbumList(value: String?): List<CollectorAlbum>? {
        val listType = object : TypeToken<List<CollectorAlbum>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Converters para List<Album>
    @TypeConverter
    fun fromAlbumList(value: List<Album>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAlbumList(value: String?): List<Album>? {
        val listType = object : TypeToken<List<Album>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Converters para List<PerformerPrize>
    @TypeConverter
    fun fromPerformerPrizeList(value: List<PerformerPrize>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPerformerPrizeList(value: String?): List<PerformerPrize>? {
        val listType = object : TypeToken<List<PerformerPrize>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Converters para List<Any> (para los comentarios en Collector)
    @TypeConverter
    fun fromAnyList(value: List<Any>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAnyList(value: String?): List<Any>? {
        val listType = object : TypeToken<List<Any>>() {}.type
        return gson.fromJson(value, listType)
    }
}
