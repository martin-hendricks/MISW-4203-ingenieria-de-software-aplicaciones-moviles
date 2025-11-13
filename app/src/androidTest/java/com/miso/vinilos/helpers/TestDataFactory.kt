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
     * Género de prueba por defecto
     */
    val DEFAULT_GENRE = Genre.ROCK

    /**
     * Discográfica de prueba por defecto
     */
    val DEFAULT_RECORD_LABEL = RecordLabel.SONY
    
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
            genre = DEFAULT_GENRE,
            recordLabel = DEFAULT_RECORD_LABEL,
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

    /**
     * Crea un comentario de prueba
     */
    fun createTestComment(id: Int = 1, description: String = "Gran álbum!", rating: Int = 5): Comment {
        return Comment(
            id = id,
            description = description,
            rating = rating
        )
    }

    /**
     * Crea una lista de comentarios de prueba
     */
    fun createTestComments(): List<Comment> {
        return listOf(
            createTestComment(1, "Excelente álbum, una obra maestra del rock", 5),
            createTestComment(2, "Muy bueno, aunque prefiero sus trabajos anteriores", 4),
            createTestComment(3, "Definitivamente uno de mis álbumes favoritos", 5)
        )
    }

    /**
     * Crea un álbum completo con todos los detalles para pruebas de detalle
     */
    fun createTestAlbumWithFullDetails(
        id: Int = 1,
        name: String = "Abbey Road",
        cover: String = "https://example.com/abbey-road.jpg",
        description: String = "El último álbum grabado por The Beatles"
    ): Album {
        val calendar = Calendar.getInstance()
        calendar.set(1969, Calendar.SEPTEMBER, 26)

        return Album(
            id = id,
            name = name,
            cover = cover,
            releaseDate = calendar.time,
            description = description,
            genre = DEFAULT_GENRE,
            recordLabel = DEFAULT_RECORD_LABEL,
            tracks = createTestTracks(),
            performers = listOf(createTestPerformer(1, "The Beatles")),
            comments = createTestComments()
        )
    }

    /**
     * Crea un álbum sin tracks para pruebas
     */
    fun createAlbumWithoutTracks(): Album {
        return createTestAlbum(
            id = 9,
            name = "Album Without Tracks",
            cover = "https://example.com/no-tracks.jpg",
            description = "Álbum sin canciones registradas",
            tracks = null
        )
    }

    /**
     * Crea un álbum sin comentarios para pruebas
     */
    fun createAlbumWithoutComments(): Album {
        return createTestAlbum(
            id = 10,
            name = "Album Without Comments",
            cover = "https://example.com/no-comments.jpg",
            description = "Álbum sin comentarios",
            tracks = createTestTracks(),
            performers = listOf(createTestPerformer(1, "The Beatles"))
        )
    }
    
    /**
     * Crea un músico de prueba
     */
    fun createTestMusician(
        id: Int = 1,
        name: String = "John Lennon",
        image: String = "https://example.com/john-lennon.jpg",
        description: String = "Músico y compositor británico",
        birthDate: Calendar = Calendar.getInstance().apply {
            set(1940, Calendar.OCTOBER, 9)
        },
        albums: List<Album>? = null,
        performerPrizes: List<com.miso.vinilos.model.data.PerformerPrize>? = null
    ): com.miso.vinilos.model.data.Musician {
        return com.miso.vinilos.model.data.Musician(
            id = id,
            name = name,
            image = image,
            description = description,
            birthDate = birthDate.time,
            albums = albums,
            performerPrizes = performerPrizes
        )
    }
    
    /**
     * Crea una lista de músicos de prueba
     */
    fun createTestMusicians(): List<com.miso.vinilos.model.data.Musician> {
        val calendar1 = Calendar.getInstance().apply {
            set(1940, Calendar.OCTOBER, 9)
        }
        val calendar2 = Calendar.getInstance().apply {
            set(1942, Calendar.JUNE, 18)
        }
        val calendar3 = Calendar.getInstance().apply {
            set(1943, Calendar.JANUARY, 9)
        }
        
        return listOf(
            createTestMusician(
                id = 1,
                name = "John Lennon",
                image = "https://example.com/john-lennon.jpg",
                description = "Músico y compositor británico, miembro de The Beatles",
                birthDate = calendar1,
                albums = listOf(createTestAlbum(1, "Abbey Road"))
            ),
            createTestMusician(
                id = 2,
                name = "Paul McCartney",
                image = "https://example.com/paul-mccartney.jpg",
                description = "Músico y compositor británico, miembro de The Beatles",
                birthDate = calendar2,
                albums = listOf(createTestAlbum(1, "Abbey Road"))
            ),
            createTestMusician(
                id = 3,
                name = "David Gilmour",
                image = "https://example.com/david-gilmour.jpg",
                description = "Guitarrista y vocalista de Pink Floyd",
                birthDate = calendar3,
                albums = listOf(createTestAlbum(2, "The Dark Side of the Moon"))
            )
        )
    }
    
    /**
     * Crea un músico completo con todos los detalles para pruebas de detalle
     */
    fun createTestMusicianWithFullDetails(
        id: Int = 1,
        name: String = "John Lennon",
        image: String = "https://example.com/john-lennon.jpg",
        description: String = "Músico y compositor británico, miembro de The Beatles"
    ): Musician {
        val calendar = Calendar.getInstance().apply {
            set(1940, Calendar.OCTOBER, 9)
        }
        
        val prize = Prize(
            id = 1,
            name = "Grammy Award",
            description = "Premio Grammy",
            organization = "Recording Academy"
        )
        
        val performerPrize = PerformerPrize(
            id = 1,
            premiationDate = "1970-01-01",
            prize = prize,
            prizeId = 1
        )
        
        return Musician(
            id = id,
            name = name,
            image = image,
            description = description,
            birthDate = calendar.time,
            albums = listOf(
                createTestAlbum(1, "Abbey Road"),
                createTestAlbum(4, "Sgt. Pepper's Lonely Hearts Club Band")
            ),
            performerPrizes = listOf(performerPrize)
        )
    }
    
    /**
     * Crea un coleccionista de prueba
     */
    fun createTestCollector(
        id: Int = 1,
        name: String = "Juan Pérez",
        telephone: String = "+57 300 123 4567",
        email: String = "juan.perez@example.com",
        image: String? = "https://example.com/juan-perez.jpg",
        comments: List<Any>? = null,
        favoritePerformers: List<Performer>? = null,
        collectorAlbums: List<CollectorAlbum>? = null
    ): Collector {
        return Collector(
            id = id,
            name = name,
            telephone = telephone,
            email = email,
            image = image,
            comments = comments,
            favoritePerformers = favoritePerformers,
            collectorAlbums = collectorAlbums
        )
    }
    
    /**
     * Crea una lista de coleccionistas de prueba
     */
    fun createTestCollectors(): List<Collector> {
        return listOf(
            createTestCollector(
                id = 1,
                name = "Juan Pérez",
                telephone = "+57 300 123 4567",
                email = "juan.perez@example.com",
                image = "https://example.com/juan-perez.jpg",
                favoritePerformers = listOf(createTestPerformer(1, "The Beatles"))
            ),
            createTestCollector(
                id = 2,
                name = "María García",
                telephone = "+57 310 987 6543",
                email = "maria.garcia@example.com",
                image = "https://example.com/maria-garcia.jpg",
                favoritePerformers = listOf(createTestPerformer(2, "Pink Floyd"))
            ),
            createTestCollector(
                id = 3,
                name = "Carlos Rodríguez",
                telephone = "+57 320 555 1234",
                email = "carlos.rodriguez@example.com",
                image = "https://example.com/carlos-rodriguez.jpg",
                favoritePerformers = listOf(createTestPerformer(3, "Led Zeppelin"))
            )
        )
    }
    
    /**
     * Crea un coleccionista completo con todos los detalles para pruebas de detalle
     */
    fun createTestCollectorWithFullDetails(
        id: Int = 1,
        name: String = "Juan Pérez",
        telephone: String = "+57 300 123 4567",
        email: String = "juan.perez@example.com"
    ): Collector {
        val collectorAlbum1 = CollectorAlbum(
            id = 1,
            price = 50000,
            status = "ACTIVE",
            album = null,
            albumId = 1
        )
        
        val collectorAlbum2 = CollectorAlbum(
            id = 2,
            price = 60000,
            status = "ACTIVE",
            album = null,
            albumId = 2
        )
        
        return Collector(
            id = id,
            name = name,
            telephone = telephone,
            email = email,
            image = "https://example.com/juan-perez.jpg",
            comments = null,
            favoritePerformers = listOf(
                createTestPerformer(1, "The Beatles"),
                createTestPerformer(2, "Pink Floyd")
            ),
            collectorAlbums = listOf(collectorAlbum1, collectorAlbum2)
        )
    }
}
