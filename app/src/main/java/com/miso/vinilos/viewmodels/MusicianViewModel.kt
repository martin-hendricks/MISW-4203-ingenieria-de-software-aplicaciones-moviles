package com.miso.vinilos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.data.PerformerPrize
import com.miso.vinilos.model.data.Prize
import com.miso.vinilos.model.repository.MusicianRepository
import com.miso.vinilos.model.repository.PrizeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Estado de la UI para la lista de músicos
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface MusicianUiState {
    object Loading : MusicianUiState
    data class Success(val musicians: List<Musician>) : MusicianUiState
    data class Error(val message: String, val canRetry: Boolean = true) : MusicianUiState
    object Empty : MusicianUiState
}

/**
 * Estado de la UI para el detalle de un músico
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface MusicianDetailUiState {
    object Loading : MusicianDetailUiState
    data class Success(val musician: Musician) : MusicianDetailUiState
    data class Error(val message: String, val canRetry: Boolean = true) : MusicianDetailUiState
}

/**
 * Estado de la UI para los premios cargados
 */
data class PrizeState(
    val prize: Prize? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Estado de la UI para agregar álbum a músico
 */
sealed interface AddAlbumToMusicianUiState {
    object Idle : AddAlbumToMusicianUiState
    object Loading : AddAlbumToMusicianUiState
    data class Success(val musicianId: Int) : AddAlbumToMusicianUiState
    data class Error(val message: String) : AddAlbumToMusicianUiState
}

/**
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de músicos
 * Sigue el patrón MVVM de Android Architecture Guidelines
 * Incluye carga paralela de premios para optimizar el rendimiento
 *
 * @param repository Repositorio de músicos (debe ser inyectado)
 * @param prizeRepository Repositorio de premios (debe ser inyectado)
 * @param dispatcher Dispatcher de coroutines (inyectable para testing)
 */
class MusicianViewModel(
    private val repository: MusicianRepository,
    private val prizeRepository: PrizeRepository,
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
    private val _uiState = MutableStateFlow<MusicianUiState>(MusicianUiState.Loading)
    val uiState: StateFlow<MusicianUiState> = _uiState.asStateFlow()

    /**
     * Estado del detalle de un músico, inicializado en Loading
     */
    private val _musicianDetailState = MutableStateFlow<MusicianDetailUiState>(MusicianDetailUiState.Loading)
    val musicianDetailState: StateFlow<MusicianDetailUiState> = _musicianDetailState.asStateFlow()
    
    /**
     * Estado de los premios cargados (mapeado por ID del premio)
     */
    private val _prizesState = MutableStateFlow<Map<Int, PrizeState>>(emptyMap())
    val prizesState: StateFlow<Map<Int, PrizeState>> = _prizesState.asStateFlow()
    
    /**
     * Estado de agregar álbum a músico
     */
    private val _addAlbumToMusicianState = MutableStateFlow<AddAlbumToMusicianUiState>(AddAlbumToMusicianUiState.Idle)
    val addAlbumToMusicianState: StateFlow<AddAlbumToMusicianUiState> = _addAlbumToMusicianState.asStateFlow()
    
    /**
     * Carga la lista de músicos desde el repositorio con retry automático
     * Este método inicia automáticamente la carga al crear el ViewModel
     *
     * @param retryCount Número de intentos de reintento (para uso interno)
     */
    fun loadMusicians(retryCount: Int = 0) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = MusicianUiState.Loading

            try {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    repository.getMusicians()
                        .onSuccess { musicians ->
                            if (musicians.isEmpty()) {
                                _uiState.value = MusicianUiState.Empty
                            } else {
                                _uiState.value = MusicianUiState.Success(musicians)
                            }
                        }
                        .onFailure { exception ->
                            handleLoadError(exception, retryCount)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    delay(RETRY_DELAY_MS)
                    loadMusicians(retryCount + 1)
                } else {
                    _uiState.value = MusicianUiState.Error(
                        "Tiempo de espera agotado después de $MAX_RETRY_ATTEMPTS intentos",
                        canRetry = true
                    )
                }
            }
        }
    }

    /**
     * Maneja los errores durante la carga de músicos
     */
    private suspend fun handleLoadError(exception: Throwable, retryCount: Int) {
        val shouldRetry = retryCount < MAX_RETRY_ATTEMPTS && isRetryableError(exception)

        if (shouldRetry) {
            delay(RETRY_DELAY_MS * (retryCount + 1))
            loadMusicians(retryCount + 1)
        } else {
            val errorMessage = when {
                exception.message?.contains("Unable to resolve host") == true ->
                    "No se puede conectar al servidor"
                exception.message?.contains("Failed to connect") == true ->
                    "Error de conexión. Verifica tu conexión de red"
                exception.message?.contains("timeout") == true ->
                    "Tiempo de espera agotado"
                retryCount >= MAX_RETRY_ATTEMPTS ->
                    "Error después de $MAX_RETRY_ATTEMPTS intentos: ${exception.message}"
                else ->
                    "Error: ${exception.message ?: "Error desconocido al cargar músicos"}"
            }
            _uiState.value = MusicianUiState.Error(errorMessage, canRetry = true)
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
     * Refresca la lista de músicos
     * Útil para pull-to-refresh o cuando se necesita recargar los datos
     */
    fun refreshMusicians() {
        loadMusicians()
    }

    /**
     * Carga el detalle de un músico específico desde el repositorio
     * Este método se debe llamar al navegar a la pantalla de detalle
     *
     * @param musicianId ID del músico a cargar
     */
    fun loadMusicianDetail(musicianId: Int) {
        viewModelScope.launch(dispatcher) {
            _musicianDetailState.value = MusicianDetailUiState.Loading

            repository.getMusician(musicianId)
                .onSuccess { musician ->
                    _musicianDetailState.value = MusicianDetailUiState.Success(musician)
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("Unable to resolve host") == true ->
                            "No se puede conectar al servidor. Verifica que el backend esté corriendo en localhost:3000"
                        exception.message?.contains("Failed to connect") == true ->
                            "Error de conexión. Verifica tu conexión de red"
                        exception.message?.contains("timeout") == true ->
                            "Tiempo de espera agotado. El servidor no responde"
                        exception.message?.contains("no encontrado") == true ->
                            "El músico no fue encontrado"
                        else ->
                            "Error: ${exception.message ?: "Error desconocido al cargar el músico"}"
                    }
                    _musicianDetailState.value = MusicianDetailUiState.Error(errorMessage)
                }
        }
    }

    /**
     * Carga los premios asociados a un músico usando el servicio /prizes/{id}
     * Optimizado para cargar todos los premios en paralelo
     * Este método se llama cuando se necesita mostrar los premios del artista
     *
     * @param performerPrizes Lista de performerPrizes del músico
     */
    fun loadPrizes(performerPrizes: List<PerformerPrize>) {
        viewModelScope.launch(dispatcher) {
            android.util.Log.d("MusicianViewModel", "loadPrizes: INICIO - Cargando ${performerPrizes.size} premios en PARALELO")

            if (performerPrizes.isEmpty()) {
                android.util.Log.d("MusicianViewModel", "loadPrizes: No hay premios para cargar")
                return@launch
            }
            
            try {
                // Cargar todos los premios en paralelo usando async
                withTimeout(NETWORK_TIMEOUT_MS) {
                    val prizeJobs = performerPrizes.mapNotNull { performerPrize ->
                        // Obtener el ID del premio
                        val prizeId = performerPrize.prize?.id
                            ?: performerPrize.prizeId
                            ?: performerPrize.id
                            ?: run {
                                android.util.Log.w("MusicianViewModel", "loadPrizes: No se pudo obtener prizeId")
                                return@mapNotNull null
                            }

                        // Verificar si ya está cargado
                        val existingState = _prizesState.value[prizeId]
                        if (existingState?.prize != null) {
                            android.util.Log.d("MusicianViewModel", "loadPrizes: Premio $prizeId ya cargado")
                            return@mapNotNull null
                        }

                        // Marcar como cargando
                        val loadingMap = HashMap(_prizesState.value)
                        loadingMap[prizeId] = PrizeState(isLoading = true)
                        _prizesState.value = loadingMap

                        // Crear tarea asíncrona para cargar el premio
                        async(Dispatchers.IO) {
                            android.util.Log.d("MusicianViewModel", "async: Premio $prizeId ejecutándose en thread=${Thread.currentThread().name}")
                            delay(3000) // TEMPORAL: delay para visualizar en Profiler
                            val result = prizeRepository.getPrize(prizeId)
                            android.util.Log.d("MusicianViewModel", "async: Premio $prizeId completado en thread=${Thread.currentThread().name}")
                            prizeId to result
                        }
                    }

                    // Esperar a que se completen todas las cargas en paralelo
                    val results = prizeJobs.awaitAll()

                    // Procesar los resultados
                    val updatedMap = HashMap(_prizesState.value)
                    results.forEach { (prizeId, result) ->
                        result.onSuccess { prize ->
                            android.util.Log.d("MusicianViewModel", "loadPrizes: ✅ Premio $prizeId cargado: ${prize.name}")
                            updatedMap[prizeId] = PrizeState(prize = prize, isLoading = false)
                        }.onFailure { exception ->
                            android.util.Log.e("MusicianViewModel", "loadPrizes: ❌ Error premio $prizeId", exception)
                            val errorMessage = when {
                                exception.message?.contains("Unable to resolve host") == true -> "Sin conexión"
                                exception.message?.contains("Failed to connect") == true -> "Error de conexión"
                                exception.message?.contains("timeout") == true -> "Tiempo agotado"
                                else -> exception.message ?: "Error desconocido"
                            }
                            updatedMap[prizeId] = PrizeState(isLoading = false, error = errorMessage)
                        }
                    }
                    
                    // Actualizar el estado una sola vez con todos los resultados
                    _prizesState.value = updatedMap
                    android.util.Log.d("MusicianViewModel", "loadPrizes: FIN - ${updatedMap.size} premios procesados")
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("MusicianViewModel", "loadPrizes: Timeout al cargar premios", e)
                // Marcar todos como error por timeout
                val errorMap = HashMap(_prizesState.value)
                performerPrizes.forEach { performerPrize ->
                    val prizeId = performerPrize.prize?.id ?: performerPrize.prizeId ?: performerPrize.id
                    prizeId?.let {
                        errorMap[it] = PrizeState(isLoading = false, error = "Tiempo agotado")
                    }
                }
                _prizesState.value = errorMap
            }
        }
    }
    
    /**
     * Limpia el estado de los premios
     * Útil cuando se navega fuera de la pantalla de detalle
     */
    fun clearPrizesState() {
        _prizesState.value = emptyMap()
    }
    
    /**
     * Agrega un álbum a un músico
     * @param musicianId ID del músico
     * @param albumId ID del álbum
     */
    fun addAlbumToMusician(musicianId: Int, albumId: Int) {
        viewModelScope.launch(dispatcher) {
            _addAlbumToMusicianState.value = AddAlbumToMusicianUiState.Loading
            
            try {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    repository.addAlbumToMusician(musicianId, albumId)
                        .onSuccess {
                            _addAlbumToMusicianState.value = AddAlbumToMusicianUiState.Success(musicianId)
                            // Refrescar el detalle del músico desde la red para mostrar el nuevo álbum
                            refreshMusicianDetail(musicianId)
                        }
                        .onFailure { exception ->
                            val errorMessage = when {
                                exception.message?.contains("Unable to resolve host") == true ->
                                    "No se puede conectar al servidor"
                                exception.message?.contains("Failed to connect") == true ->
                                    "Error de conexión. Verifica tu conexión de red"
                                exception.message?.contains("timeout") == true ->
                                    "Tiempo de espera agotado"
                                else ->
                                    exception.message ?: "Error desconocido al agregar álbum"
                            }
                            _addAlbumToMusicianState.value = AddAlbumToMusicianUiState.Error(errorMessage)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                _addAlbumToMusicianState.value = AddAlbumToMusicianUiState.Error("Tiempo de espera agotado")
            }
        }
    }
    
    /**
     * Refresca el detalle de un músico desde la red, forzando la actualización
     * Útil después de agregar un álbum para obtener los datos más recientes
     * @param musicianId ID del músico a refrescar
     */
    fun refreshMusicianDetail(musicianId: Int) {
        viewModelScope.launch(dispatcher) {
            _musicianDetailState.value = MusicianDetailUiState.Loading

            try {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    repository.refreshMusician(musicianId)
                        .onSuccess { musician ->
                            _musicianDetailState.value = MusicianDetailUiState.Success(musician)
                        }
                        .onFailure { exception ->
                            val errorMessage = when {
                                exception.message?.contains("Unable to resolve host") == true ->
                                    "No se puede conectar al servidor. Verifica que el backend esté corriendo en localhost:3000"
                                exception.message?.contains("Failed to connect") == true ->
                                    "Error de conexión. Verifica tu conexión de red"
                                exception.message?.contains("timeout") == true ->
                                    "Tiempo de espera agotado. El servidor no responde"
                                exception.message?.contains("no encontrado") == true ->
                                    "El músico no fue encontrado"
                                else ->
                                    "Error: ${exception.message ?: "Error desconocido al cargar el músico"}"
                            }
                            _musicianDetailState.value = MusicianDetailUiState.Error(errorMessage)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                _musicianDetailState.value = MusicianDetailUiState.Error("Tiempo de espera agotado")
            }
        }
    }
    
    /**
     * Limpia el estado de agregar álbum a músico
     */
    fun clearAddAlbumToMusicianState() {
        _addAlbumToMusicianState.value = AddAlbumToMusicianUiState.Idle
    }

    init {
        // Cargar músicos automáticamente cuando se crea el ViewModel
        loadMusicians()
    }
}

