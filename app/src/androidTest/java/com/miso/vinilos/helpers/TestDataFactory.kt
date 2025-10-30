package com.miso.vinilos.helpers

import com.miso.vinilos.model.data.*
import java.util.*

/**
 * Factory para crear datos de prueba para las pruebas E2E
 * 
 * Proporciona métodos estáticos para generar entidades de prueba
 * que coincidan con el esquema del backend
 */
object TestDataFactory {
    
    /**
     * Crea un género de prueba
     */
    fun createTestGenre(): Genre {
        return Genre.ROCK
    }
    
    /**
     * Crea una discográfica de prueba
     */
    fun createTestRecordLabel(): RecordLabel {
        return RecordLabel.SONY
    }
    
    /**
     * Crea un performer de prueba
     */
    fun createTestPerformer(id: Int = 1, name: String = "The Beatles"): Performer {
        return Performer(
            id = id, 
            name = name,
            image = "https://example.com/performer-$id.jpg",
            description = "Descripción del performer $name"
        )
    }
    
    /**
     * Crea una lista de performers de prueba
     */
    fun createTestPerformers(): List<Performer> {
        return listOf(
            createTestPerformer(1, "The Beatles"),
            createTestPerformer(2, "Pink Floyd"),
            createTestPerformer(3, "Led Zeppelin")
        )
    }
    
    /**
     * Crea un track de prueba
     */
    fun createTestTrack(id: Int = 1, name: String = "Track 1", duration: String = "3:45"): Track {
        return Track(id = id, name = name, duration = duration)
    }
    
    /**
     * Crea una lista de tracks de prueba
     */
    fun createTestTracks(): List<Track> {
        return listOf(
            createTestTrack(1, "Come Together", "4:20"),
            createTestTrack(2, "Something", "3:03"),
            createTestTrack(3, "Maxwell's Silver Hammer", "3:27")
        )
    }
    
    /**
     * Crea un álbum de prueba completo
     */
    fun createTestAlbum(
        id: Int = 1,
        name: String = "Abbey Road",
        cover: String = "https://example.com/abbey-road.jpg",
        description: String = "El último álbum grabado por The Beatles",
        performers: List<Performer>? = createTestPerformers(),
        tracks: List<Track>? = createTestTracks()
    ): Album {
        val calendar = Calendar.getInstance()
        calendar.set(1969, Calendar.SEPTEMBER, 26) // 26 de septiembre de 1969
        
        return Album(
            id = id,
            name = name,
            cover = cover,
            releaseDate = calendar.time,
            description = description,
            genre = createTestGenre(),
            recordLabel = createTestRecordLabel(),
            tracks = tracks,
            performers = performers,
            comments = null
        )
    }
    
    /**
     * Crea una lista de álbumes de prueba
     * Nota: Evitamos duplicar performers para facilitar los tests
     */
    fun createTestAlbums(): List<Album> {
        return listOf(
            createTestAlbum(
                id = 1,
                name = "Abbey Road",
                cover = "https://example.com/abbey-road.jpg",
                description = "El último álbum grabado por The Beatles",
                performers = listOf(createTestPerformer(1, "The Beatles"))
            ),
            createTestAlbum(
                id = 2,
                name = "The Dark Side of the Moon",
                cover = "https://example.com/dark-side.jpg",
                description = "Uno de los álbumes más influyentes de Pink Floyd",
                performers = listOf(createTestPerformer(2, "Pink Floyd"))
            ),
            createTestAlbum(
                id = 3,
                name = "Led Zeppelin IV",
                cover = "https://example.com/led-zeppelin-iv.jpg",
                description = "Cuarto álbum de estudio de Led Zeppelin",
                performers = listOf(createTestPerformer(3, "Led Zeppelin"))
            ),
            createTestAlbum(
                id = 4,
                name = "Sgt. Pepper's Lonely Hearts Club Band",
                cover = "https://example.com/sgt-pepper.jpg",
                description = "Álbum conceptual de Queen",
                performers = listOf(createTestPerformer(4, "Queen"))
            ),
            createTestAlbum(
                id = 5,
                name = "Wish You Were Here",
                cover = "https://example.com/wish-you-were-here.jpg",
                description = "Álbum icónico de David Bowie",
                performers = listOf(createTestPerformer(5, "David Bowie"))
            )
        )
    }
    
    /**
     * Crea un álbum sin performers (para probar el caso "Artista desconocido")
     */
    fun createAlbumWithoutPerformers(): Album {
        return createTestAlbum(
            id = 6,
            name = "Unknown Artist Album",
            cover = "https://example.com/unknown.jpg",
            description = "Álbum sin información de artista",
            performers = null
        )
    }
    
    /**
     * Crea un álbum con performers vacíos (para probar el caso "Artista desconocido")
     */
    fun createAlbumWithEmptyPerformers(): Album {
        return createTestAlbum(
            id = 7,
            name = "Empty Performers Album",
            cover = "https://example.com/empty.jpg",
            description = "Álbum con lista vacía de performers",
            performers = emptyList()
        )
    }
    
    /**
     * Crea un álbum con un solo performer
     */
    fun createAlbumWithSinglePerformer(): Album {
        return createTestAlbum(
            id = 8,
            name = "Solo Artist Album",
            cover = "https://example.com/solo.jpg",
            description = "Álbum de un solo artista",
            performers = listOf(createTestPerformer(4, "Bob Dylan"))
        )
    }
}
