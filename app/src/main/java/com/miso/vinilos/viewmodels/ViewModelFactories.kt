package com.miso.vinilos.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.miso.vinilos.VinilosApplication
import com.miso.vinilos.model.repository.AlbumRepository
import com.miso.vinilos.model.repository.CollectorRepository
import com.miso.vinilos.model.repository.MusicianRepository
import com.miso.vinilos.model.repository.PrizeRepository

/**
 * Factory functions para crear ViewModels con inyección de dependencias
 * Siguiendo el patrón recomendado por Google para Jetpack Compose
 */

/**
 * Crea un AlbumViewModel con todas sus dependencias inyectadas
 * Usa el patrón ViewModelFactory de Compose para inyectar el repositorio
 *
 * @return AlbumViewModel configurado con Room cache
 */
@Composable
fun createAlbumViewModel(): AlbumViewModel {
    val application = LocalContext.current.applicationContext as VinilosApplication
    return viewModel(
        factory = viewModelFactory {
            initializer {
                AlbumViewModel(
                    repository = AlbumRepository.getInstance(
                        application = application,
                        albumsDao = application.database.albumsDao()
                    )
                )
            }
        }
    )
}

/**
 * Crea un CollectorViewModel con todas sus dependencias inyectadas
 *
 * @return CollectorViewModel configurado con Room cache
 */
@Composable
fun createCollectorViewModel(): CollectorViewModel {
    val application = LocalContext.current.applicationContext as VinilosApplication
    return viewModel(
        factory = viewModelFactory {
            initializer {
                CollectorViewModel(
                    repository = CollectorRepository.getInstance(
                        application = application,
                        collectorsDao = application.database.collectorsDao()
                    ),
                    albumRepository = AlbumRepository.getInstance(
                        application = application,
                        albumsDao = application.database.albumsDao()
                    )
                )
            }
        }
    )
}

/**
 * Crea un MusicianViewModel con todas sus dependencias inyectadas
 *
 * @return MusicianViewModel configurado con Room cache
 */
@Composable
fun createMusicianViewModel(): MusicianViewModel {
    val application = LocalContext.current.applicationContext as VinilosApplication
    return viewModel(
        factory = viewModelFactory {
            initializer {
                MusicianViewModel(
                    repository = MusicianRepository.getInstance(
                        application = application,
                        musiciansDao = application.database.musiciansDao()
                    ),
                    prizeRepository = PrizeRepository.getInstance()
                )
            }
        }
    )
}
