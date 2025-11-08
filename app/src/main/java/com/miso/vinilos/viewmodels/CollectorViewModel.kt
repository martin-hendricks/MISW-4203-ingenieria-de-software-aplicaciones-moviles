package com.miso.vinilos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.repository.CollectorRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la UI para la lista de coleccionistas
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface CollectorUiState {
    object Loading : CollectorUiState
    data class Success(val collectors: List<Collector>) : CollectorUiState
    data class Error(val message: String) : CollectorUiState
}

/**
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de coleccionistas
 * Sigue el patrón MVVM de Android Architecture Guidelines
 *
 * @param repository Repositorio de coleccionistas (inyectable para testing)
 * @param dispatcher Dispatcher de coroutines (inyectable para testing)
 */
class CollectorViewModel(
    private val repository: CollectorRepository = CollectorRepository.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
    
    /**
     * Estado actual de la UI, inicializado en Loading
     */
    private val _uiState = MutableStateFlow<CollectorUiState>(CollectorUiState.Loading)
    val uiState: StateFlow<CollectorUiState> = _uiState.asStateFlow()
    
    /**
     * Carga la lista de coleccionistas desde el repositorio
     * Este método inicia automáticamente la carga al crear el ViewModel
     */
    fun loadCollectors() {
        viewModelScope.launch(dispatcher) {
            _uiState.value = CollectorUiState.Loading
            try {
                val result = repository.getCollectors()
                result.onSuccess { collectors ->
                    _uiState.value = CollectorUiState.Success(collectors)
                }.onFailure { exception ->
                    _uiState.value = CollectorUiState.Error(
                        exception.message ?: "Error desconocido al cargar coleccionistas"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = CollectorUiState.Error(
                    e.message ?: "Error desconocido al cargar coleccionistas"
                )
            }
        }
    }
    
    /**
     * Refresca la lista de coleccionistas
     * Útil para pull-to-refresh o recargar manualmente
     */
    fun refreshCollectors() {
        loadCollectors()
    }
    
    init {
        loadCollectors()
    }
}

