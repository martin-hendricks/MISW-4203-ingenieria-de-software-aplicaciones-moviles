package com.miso.vinilos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.repository.AlbumRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Estado de la UI para la lista de álbumes
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface AlbumUiState {
    object Loading : AlbumUiState
    data class Success(val albums: List<Album>) : AlbumUiState
    data class Error(val message: String, val canRetry: Boolean = true) : AlbumUiState
    object Empty : AlbumUiState
}

/**
 * Estado de la UI para el detalle de un álbum
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface AlbumDetailUiState {
    object Loading : AlbumDetailUiState
    data class Success(val album: Album) : AlbumDetailUiState
    data class Error(val message: String, val canRetry: Boolean = true) : AlbumDetailUiState
}

/**
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de álbumes
 * Sigue el patrón MVVM de Android Architecture Guidelines
 * Incluye manejo de retry automático, timeout y mejor gestión de errores
 *
 * @param repository Repositorio de álbumes (debe ser inyectado)
 * @param dispatcher Dispatcher de coroutines (inyectable para testing)
 */
class AlbumViewModel(
    private val repository: AlbumRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
    
    companion object {
        private const val NETWORK_TIMEOUT_MS = 30_000L // 30 segundos
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L // 1 segundo
    }

    /**
     * Estado actual de la UI, inicializado en Loading
     */
    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    /**
     * Estado del detalle de un álbum, inicializado en Loading
     */
    private val _albumDetailState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val albumDetailState: StateFlow<AlbumDetailUiState> = _albumDetailState.asStateFlow()
    
    /**
     * Indica si hay una operación de carga en progreso
     */
    private val _isRefreshing = MutableStateFlow(false)
    @Suppress("unused") // Se usa en la UI para pull-to-refresh
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Carga la lista de álbumes desde el repositorio con retry automático
     * Este método inicia automáticamente la carga al crear el ViewModel
     *
     * @param retryCount Número de intentos de reintento (para uso interno)
     */
    fun loadAlbums(retryCount: Int = 0) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = AlbumUiState.Loading

            try {
                // Aplicar timeout a la operación
                withTimeout(NETWORK_TIMEOUT_MS) {
                    repository.getAlbums()
                        .onSuccess { albums ->
                            if (albums.isEmpty()) {
                                _uiState.value = AlbumUiState.Empty
                            } else {
                                _uiState.value = AlbumUiState.Success(albums)
                            }
                        }
                        .onFailure { exception ->
                            handleLoadError(exception, retryCount)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    delay(RETRY_DELAY_MS)
                    loadAlbums(retryCount + 1)
                } else {
                    _uiState.value = AlbumUiState.Error(
                        "Tiempo de espera agotado después de $MAX_RETRY_ATTEMPTS intentos",
                        canRetry = true
                    )
                }
            }
        }
    }

    /**
     * Maneja los errores durante la carga de álbumes
     */
    private suspend fun handleLoadError(exception: Throwable, retryCount: Int) {
        val shouldRetry = retryCount < MAX_RETRY_ATTEMPTS && isRetryableError(exception)

        if (shouldRetry) {
            delay(RETRY_DELAY_MS * (retryCount + 1)) // Backoff exponencial
            loadAlbums(retryCount + 1)
        } else {
            val errorMessage = when {
                exception.message?.contains("Unable to resolve host") == true ->
                    "No se puede conectar al servidor. Verifica tu conexión de red"
                exception.message?.contains("Failed to connect") == true ->
                    "Error de conexión. Verifica tu conexión de red"
                exception.message?.contains("timeout") == true ->
                    "Tiempo de espera agotado. El servidor no responde"
                retryCount >= MAX_RETRY_ATTEMPTS ->
                    "Error después de $MAX_RETRY_ATTEMPTS intentos: ${exception.message}"
                else ->
                    "Error: ${exception.message ?: "Error desconocido al cargar álbumes"}"
            }
            _uiState.value = AlbumUiState.Error(errorMessage, canRetry = true)
        }
    }

    /**
     * Determina si un error es recuperable mediante retry
     */
    private fun isRetryableError(exception: Throwable): Boolean {
        return exception.message?.contains("timeout") == true ||
               exception.message?.contains("Failed to connect") == true ||
               exception.message?.contains("SocketTimeoutException") == true
    }
    
    /**
     * Refresca la lista de álbumes
     * Útil para pull-to-refresh o cuando se necesita recargar los datos
     */
    fun refreshAlbums() {
        viewModelScope.launch(dispatcher) {
            _isRefreshing.value = true
            try {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    repository.getAlbums()
                        .onSuccess { albums ->
                            if (albums.isEmpty()) {
                                _uiState.value = AlbumUiState.Empty
                            } else {
                                _uiState.value = AlbumUiState.Success(albums)
                            }
                        }
                        .onFailure { exception ->
                            // En refresh no hacemos retry, mostramos el error inmediatamente
                            val errorMessage = when {
                                exception.message?.contains("Unable to resolve host") == true ->
                                    "No se puede conectar al servidor"
                                exception.message?.contains("Failed to connect") == true ->
                                    "Error de conexión"
                                exception.message?.contains("timeout") == true ->
                                    "Tiempo de espera agotado"
                                else ->
                                    exception.message ?: "Error al refrescar"
                            }
                            _uiState.value = AlbumUiState.Error(errorMessage, canRetry = true)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.value = AlbumUiState.Error(
                    "Tiempo de espera agotado al refrescar",
                    canRetry = true
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Carga el detalle de un álbum específico desde el repositorio
     * Este método se debe llamar al navegar a la pantalla de detalle
     *
     * @param albumId ID del álbum a cargar
     */
    fun loadAlbumDetail(albumId: Int) {
        viewModelScope.launch(dispatcher) {
            _albumDetailState.value = AlbumDetailUiState.Loading

            try {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    repository.getAlbum(albumId)
                        .onSuccess { album ->
                            _albumDetailState.value = AlbumDetailUiState.Success(album)
                        }
                        .onFailure { exception ->
                            val errorMessage = when {
                                exception.message?.contains("Unable to resolve host") == true ->
                                    "No se puede conectar al servidor"
                                exception.message?.contains("Failed to connect") == true ->
                                    "Error de conexión. Verifica tu conexión de red"
                                exception.message?.contains("timeout") == true ->
                                    "Tiempo de espera agotado. El servidor no responde"
                                exception.message?.contains("no encontrado") == true ->
                                    "El álbum no fue encontrado"
                                else ->
                                    "Error: ${exception.message ?: "Error desconocido al cargar el álbum"}"
                            }
                            _albumDetailState.value = AlbumDetailUiState.Error(errorMessage, canRetry = true)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                _albumDetailState.value = AlbumDetailUiState.Error(
                    "Tiempo de espera agotado al cargar el álbum",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Reinicia el estado del detalle del álbum
     * Útil cuando se navega fuera de la pantalla de detalle
     */
    fun clearAlbumDetail() {
        _albumDetailState.value = AlbumDetailUiState.Loading
    }

    /**
     * Reintenta cargar los álbumes después de un error
     */
    @Suppress("unused") // Se usa en la UI para botones de retry
    fun retryLoadAlbums() {
        loadAlbums(retryCount = 0)
    }

    /**
     * Reintenta cargar el detalle de un álbum después de un error
     */
    @Suppress("unused") // Se usa en la UI para botones de retry
    fun retryLoadAlbumDetail(albumId: Int) {
        loadAlbumDetail(albumId)
    }

    init {
        // Cargar álbumes automáticamente cuando se crea el ViewModel
        loadAlbums()
    }
}

