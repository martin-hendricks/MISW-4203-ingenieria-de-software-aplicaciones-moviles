package com.miso.vinilos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.Musician
import com.miso.vinilos.model.repository.MusicianRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de músicos
 * Sigue el patrón MVVM de Android Architecture Guidelines
 *
 * @param repository Repositorio de músicos (inyectable para testing)
 * @param dispatcher Dispatcher de coroutines (inyectable para testing)
 */
class MusicianViewModel(
    private val repository: MusicianRepository = MusicianRepository.getInstance(),
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

    init {
        // Cargar músicos automáticamente cuando se crea el ViewModel
        loadMusicians()
    }
}

