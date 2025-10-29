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
 * Estado de la UI para el detalle de un álbum
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface AlbumDetailUiState {
    object Loading : AlbumDetailUiState
    data class Success(val album: Album) : AlbumDetailUiState
    data class Error(val message: String) : AlbumDetailUiState
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
     * Estado del detalle de un álbum, inicializado en Loading
     */
    private val _albumDetailState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val albumDetailState: StateFlow<AlbumDetailUiState> = _albumDetailState.asStateFlow()
    
    /**
     * Carga la lista de álbumes desde el repositorio
     * Este método inicia automáticamente la carga al crear el ViewModel
     */
    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            
            repository.getAlbums()
                .onSuccess { albums ->
                    _uiState.value = AlbumUiState.Success(albums)
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
                            "Error: ${exception.message ?: "Error desconocido al cargar álbumes"}"
                    }
                    _uiState.value = AlbumUiState.Error(errorMessage)
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

    /**
     * Carga el detalle de un álbum específico desde el repositorio
     * Este método se debe llamar al navegar a la pantalla de detalle
     *
     * @param albumId ID del álbum a cargar
     */
    fun loadAlbumDetail(albumId: Int) {
        viewModelScope.launch {
            _albumDetailState.value = AlbumDetailUiState.Loading

            repository.getAlbum(albumId)
                .onSuccess { album ->
                    _albumDetailState.value = AlbumDetailUiState.Success(album)
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
                            "El álbum no fue encontrado"
                        else ->
                            "Error: ${exception.message ?: "Error desconocido al cargar el álbum"}"
                    }
                    _albumDetailState.value = AlbumDetailUiState.Error(errorMessage)
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

    init {
        // Cargar álbumes automáticamente cuando se crea el ViewModel
        loadAlbums()
    }
}

