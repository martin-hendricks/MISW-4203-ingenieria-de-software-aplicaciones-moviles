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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la UI para la lista de músicos
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface MusicianUiState {
    object Loading : MusicianUiState
    data class Success(val musicians: List<Musician>) : MusicianUiState
    data class Error(val message: String) : MusicianUiState
}

/**
 * Estado de la UI para el detalle de un músico
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface MusicianDetailUiState {
    object Loading : MusicianDetailUiState
    data class Success(val musician: Musician) : MusicianDetailUiState
    data class Error(val message: String) : MusicianDetailUiState
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
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de músicos
 * Sigue el patrón MVVM de Android Architecture Guidelines
 *
 * @param repository Repositorio de músicos (inyectable para testing)
 * @param prizeRepository Repositorio de premios (inyectable para testing)
 * @param dispatcher Dispatcher de coroutines (inyectable para testing)
 */
class MusicianViewModel(
    private val repository: MusicianRepository = MusicianRepository.getInstance(),
    private val prizeRepository: PrizeRepository = PrizeRepository.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
    
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
     * Carga la lista de músicos desde el repositorio
     * Este método inicia automáticamente la carga al crear el ViewModel
     */
    fun loadMusicians() {
        viewModelScope.launch(dispatcher) {
            _uiState.value = MusicianUiState.Loading

            repository.getMusicians()
                .onSuccess { musicians ->
                    _uiState.value = MusicianUiState.Success(musicians)
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("Unable to resolve host") == true ->
                            "No se puede conectar al servidor. Verifica que el backend esté corriendo en localhost:3000"
                        exception.message?.contains("Failed to connect") == true ->
                            "Error de conexión. Verifica tu conexión de red"
                        exception.message?.contains("timeout") == true ->
                            "Tiempo de espera agotado. El servidor no responde"
                        else ->
                            "Error: ${exception.message ?: "Error desconocido al cargar músicos"}"
                    }
                    _uiState.value = MusicianUiState.Error(errorMessage)
                }
        }
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
     * Reinicia el estado del detalle del músico
     * Útil cuando se navega fuera de la pantalla de detalle
     */
    fun clearMusicianDetail() {
        _musicianDetailState.value = MusicianDetailUiState.Loading
    }

    /**
     * Carga los premios asociados a un músico usando el servicio /prizes/{id}
     * Este método se llama cuando se necesita mostrar los premios del artista
     *
     * @param performerPrizes Lista de performerPrizes del músico
     */
    fun loadPrizes(performerPrizes: List<PerformerPrize>) {
        viewModelScope.launch {
            android.util.Log.d("MusicianViewModel", "loadPrizes: INICIO - Cargando ${performerPrizes.size} premios")
            
            // Log detallado de cada performerPrize para debug
            performerPrizes.forEachIndexed { idx, pp ->
                android.util.Log.d("MusicianViewModel", "loadPrizes: performerPrize[$idx]: id=${pp.id}, prizeId=${pp.prizeId}, prize=${pp.prize}, prize?.id=${pp.prize?.id}, premiationDate=${pp.premiationDate}")
                // Log del objeto prize completo si existe
                if (pp.prize != null) {
                    android.util.Log.d("MusicianViewModel", "loadPrizes: performerPrize[$idx].prize completo: id=${pp.prize.id}, name=${pp.prize.name}")
                } else {
                    android.util.Log.w("MusicianViewModel", "loadPrizes: performerPrize[$idx].prize es NULL")
                }
            }
            
            // Siempre cargar desde el servicio /prizes/{id} para asegurar datos completos
            performerPrizes.forEach { performerPrize ->
                android.util.Log.d("MusicianViewModel", "loadPrizes: Procesando performerPrize id=${performerPrize.id}, prizeId=${performerPrize.prizeId}, prize.id=${performerPrize.prize?.id}, prize=${performerPrize.prize}")
                
                // Obtener el ID del premio (puede venir en prize.id, prizeId, o el id del performerPrize podría ser el prizeId)
                // Intentar diferentes formas de obtener el prizeId
                val prizeId = performerPrize.prize?.id 
                    ?: performerPrize.prizeId
                    ?: performerPrize.id // Posiblemente el id del performerPrize es el mismo que el prizeId
                    ?: run {
                        android.util.Log.w("MusicianViewModel", "loadPrizes: ❌ No se pudo obtener prizeId del performerPrize id=${performerPrize.id}, prize=${performerPrize.prize}, prizeId=${performerPrize.prizeId}")
                        return@forEach
                    }
                
                android.util.Log.d("MusicianViewModel", "loadPrizes: prizeId obtenido: $prizeId (de prize.id=${performerPrize.prize?.id}, prizeId=${performerPrize.prizeId}, performerPrize.id=${performerPrize.id})")
                
                android.util.Log.d("MusicianViewModel", "loadPrizes: ✅ prizeId=$prizeId obtenido correctamente")
                
                // Verificar si ya está cargado para evitar cargas duplicadas
                val existingState = _prizesState.value[prizeId]
                if (existingState?.prize != null) {
                    android.util.Log.d("MusicianViewModel", "loadPrizes: Premio $prizeId ya está cargado, omitiendo")
                    return@forEach
                }
                
                // Marcar como cargando - crear una nueva instancia del Map para forzar emisión
                android.util.Log.d("MusicianViewModel", "loadPrizes: Marcando prizeId=$prizeId como loading")
                val loadingMap = HashMap(_prizesState.value)
                loadingMap[prizeId] = PrizeState(isLoading = true)
                _prizesState.value = loadingMap
                
                // Cargar el premio desde el servicio /prizes/{id}
                android.util.Log.d("MusicianViewModel", "loadPrizes: Llamando a prizeRepository.getPrize($prizeId)")
                val result = withContext(Dispatchers.IO) {
                    prizeRepository.getPrize(prizeId)
                }
                
                result.onSuccess { prize ->
                    android.util.Log.d("MusicianViewModel", "loadPrizes: ✅ Premio $prizeId cargado exitosamente: ${prize.name}")
                    
                    // Actualizar el estado - crear una nueva instancia del Map para forzar emisión
                    val successMap = HashMap(_prizesState.value)
                    successMap[prizeId] = PrizeState(prize = prize, isLoading = false)
                    _prizesState.value = successMap
                    android.util.Log.d("MusicianViewModel", "loadPrizes: Estado actualizado para $prizeId, total premios: ${_prizesState.value.size}")
                }.onFailure { exception ->
                    android.util.Log.e("MusicianViewModel", "loadPrizes: ❌ Error cargando premio $prizeId", exception)
                    val errorMessage = when {
                        exception.message?.contains("Unable to resolve host") == true ->
                            "No se puede conectar al servidor"
                        exception.message?.contains("Failed to connect") == true ->
                            "Error de conexión"
                        exception.message?.contains("timeout") == true ->
                            "Tiempo de espera agotado"
                        exception.message?.contains("no encontrado") == true ->
                            "Premio no encontrado"
                        else ->
                            exception.message ?: "Error desconocido al cargar el premio"
                    }
                    
                    // Actualizar el estado de error - crear una nueva instancia del Map
                    val errorMap = HashMap(_prizesState.value)
                    errorMap[prizeId] = PrizeState(isLoading = false, error = errorMessage)
                    _prizesState.value = errorMap
                    android.util.Log.e("MusicianViewModel", "loadPrizes: Estado de error actualizado para $prizeId")
                }
            }
            
            android.util.Log.d("MusicianViewModel", "loadPrizes: FIN - Estado final: ${_prizesState.value.size} premios, keys: ${_prizesState.value.keys}")
        }
    }
    
    /**
     * Limpia el estado de los premios
     * Útil cuando se navega fuera de la pantalla de detalle
     */
    fun clearPrizesState() {
        _prizesState.value = emptyMap()
    }

    init {
        // Cargar músicos automáticamente cuando se crea el ViewModel
        loadMusicians()
    }
}

