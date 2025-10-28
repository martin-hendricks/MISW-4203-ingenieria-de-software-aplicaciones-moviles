package com.miso.vinilos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la UI para la lista de álbumes
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface AlbumUiState {
    object Loading : AlbumUiState
    data class Success(val albums: List<Album>) : AlbumUiState
    data class Error(val message: String) : AlbumUiState
}

/**
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de álbumes
 * Sigue el patrón MVVM de Android Architecture Guidelines
 */
class AlbumViewModel : ViewModel() {
    
    private val repository = AlbumRepository.getInstance()
    
    /**
     * Estado actual de la UI, inicializado en Loading
     */
    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()
    
    /**
     * Carga la lista de álbumes desde el repositorio
     * Este método inicia automáticamente la carga al crear el ViewModel
     */
    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            
            when (val result = repository.getAlbums()) {
                is kotlin.Result.Success -> {
                    _uiState.value = AlbumUiState.Success(result.value)
                }
                is kotlin.Result.Failure -> {
                    _uiState.value = AlbumUiState.Error(
                        result.exception.message ?: "Error desconocido al cargar álbumes"
                    )
                }
            }
        }
    }
    
    /**
     * Refresca la lista de álbumes
     * Útil para pull-to-refresh o cuando se necesita recargar los datos
     */
    fun refreshAlbums() {
        loadAlbums()
    }
    
    init {
        // Cargar álbumes automáticamente cuando se crea el ViewModel
        loadAlbums()
    }
}

