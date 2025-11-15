package com.miso.vinilos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.Album
import com.miso.vinilos.model.data.Collector
import com.miso.vinilos.model.data.CollectorAlbum
import com.miso.vinilos.model.repository.AlbumRepository
import com.miso.vinilos.model.repository.CollectorRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * Estado de la UI para el detalle de un coleccionista
 * Representa los diferentes estados posibles: Loading, Success, Error
 */
sealed interface CollectorDetailUiState {
    object Loading : CollectorDetailUiState
    data class Success(val collector: Collector) : CollectorDetailUiState
    data class Error(val message: String) : CollectorDetailUiState
}

/**
 * Estado de carga de un álbum individual
 */
data class AlbumState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para gestionar el estado y la lógica de negocio de la lista de coleccionistas
 * Sigue el patrón MVVM de Android Architecture Guidelines
 *
 * @param repository Repositorio de coleccionistas (debe ser inyectado)
 * @param albumRepository Repositorio de álbumes (debe ser inyectado)
 * @param dispatcher Dispatcher de coroutines (inyectable para testing)
 */
class CollectorViewModel(
    private val repository: CollectorRepository,
    private val albumRepository: AlbumRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
    
    /**
     * Estado actual de la UI, inicializado en Loading
     */
    private val _uiState = MutableStateFlow<CollectorUiState>(CollectorUiState.Loading)
    val uiState: StateFlow<CollectorUiState> = _uiState.asStateFlow()
    
    /**
     * Estado del detalle de un coleccionista, inicializado en Loading
     */
    private val _collectorDetailState = MutableStateFlow<CollectorDetailUiState>(CollectorDetailUiState.Loading)
    val collectorDetailState: StateFlow<CollectorDetailUiState> = _collectorDetailState.asStateFlow()
    
    /**
     * Estado de los álbumes cargados individualmente
     * Key: albumId, Value: AlbumState
     */
    private val _albumsState = MutableStateFlow<Map<Int, AlbumState>>(emptyMap())
    val albumsState: StateFlow<Map<Int, AlbumState>> = _albumsState.asStateFlow()
    
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
    
    /**
     * Carga el detalle de un coleccionista específico desde el repositorio
     * Este método se debe llamar al navegar a la pantalla de detalle
     *
     * @param collectorId ID del coleccionista a cargar
     */
    fun loadCollectorDetail(collectorId: Int) {
        viewModelScope.launch(dispatcher) {
            _collectorDetailState.value = CollectorDetailUiState.Loading
            try {
                val result = repository.getCollector(collectorId)
                result.onSuccess { collector ->
                    _collectorDetailState.value = CollectorDetailUiState.Success(collector)
                }.onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("Unable to resolve host") == true ->
                            "No se puede conectar al servidor"
                        exception.message?.contains("Failed to connect") == true ->
                            "Error de conexión"
                        exception.message?.contains("timeout") == true ->
                            "Tiempo de espera agotado"
                        exception.message?.contains("no encontrado") == true ->
                            "Coleccionista no encontrado"
                        else ->
                            exception.message ?: "Error desconocido al cargar el coleccionista"
                    }
                    _collectorDetailState.value = CollectorDetailUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _collectorDetailState.value = CollectorDetailUiState.Error(
                    e.message ?: "Error desconocido al cargar el coleccionista"
                )
            }
        }
    }
    
    /**
     * Limpia el estado del detalle del coleccionista
     * Útil cuando se sale de la pantalla de detalle
     */
    fun clearCollectorDetail() {
        _collectorDetailState.value = CollectorDetailUiState.Loading
    }
    
    /**
     * Carga los detalles de los álbumes usando el servicio /albums/{id}
     * Este método se llama cuando se necesita mostrar los álbumes del coleccionista
     * Similar a como se cargan los premios en MusicianViewModel
     * 
     * @param collectorAlbums Lista de álbumes del coleccionista
     */
    fun loadAlbums(collectorAlbums: List<CollectorAlbum>) {
        viewModelScope.launch {
            android.util.Log.d("CollectorViewModel", "loadAlbums: INICIO - Cargando ${collectorAlbums.size}")
            
            // Log detallado de cada collectorAlbum para debug
            collectorAlbums.forEachIndexed { idx, ca ->
                android.util.Log.d("CollectorViewModel", "loadAlbums: collectorAlbum[$idx]: id=${ca.id}, albumId=${ca.albumId}, album=${ca.album}, album?.id=${ca.album?.id}, price=${ca.price}, status=${ca.status}")
                // Log del objeto album completo si existe
                if (ca.album != null) {
                    android.util.Log.d("CollectorViewModel", "loadAlbums: collectorAlbum[$idx].album completo: id=${ca.album.id}, name=${ca.album.name}, cover=${ca.album.cover}")
                } else {
                    android.util.Log.w("CollectorViewModel", "loadAlbums: collectorAlbum[$idx].album es NULL")
                }
            }
            
            // Siempre cargar desde el servicio /albums/{id} para asegurar datos completos
            collectorAlbums.forEach { collectorAlbum ->
                android.util.Log.d("CollectorViewModel", "loadAlbums: Procesando collectorAlbum id=${collectorAlbum.id}, albumId=${collectorAlbum.albumId}, album.id=${collectorAlbum.album?.id}, album=${collectorAlbum.album}")
                
                // Obtener el ID del álbum (similar a como se obtiene prizeId en loadPrizes)
                // Intentar diferentes formas de obtener el albumId
                val albumId = collectorAlbum.album?.id 
                    ?: collectorAlbum.albumId
                    ?: collectorAlbum.id // Posiblemente el id del collectorAlbum es el mismo que el albumId
                    ?: run {
                        android.util.Log.w("CollectorViewModel", "loadAlbums: ❌ No se pudo obtener albumId del collectorAlbum id=${collectorAlbum.id}, album=${collectorAlbum.album}, albumId=${collectorAlbum.albumId}")
                        return@forEach
                    }
                
                android.util.Log.d("CollectorViewModel", "loadAlbums: albumId obtenido: $albumId (de album.id=${collectorAlbum.album?.id}, albumId=${collectorAlbum.albumId}, collectorAlbum.id=${collectorAlbum.id})")
                
                android.util.Log.d("CollectorViewModel", "loadAlbums: ✅ albumId=$albumId obtenido correctamente")
                
                // Verificar si ya está cargado para evitar cargas duplicadas
                val existingState = _albumsState.value[albumId]
                if (existingState?.album != null) {
                    android.util.Log.d("CollectorViewModel", "loadAlbums: Álbum $albumId ya está cargado, omitiendo")
                    return@forEach
                }
                
                android.util.Log.d("CollectorViewModel", "loadAlbums: Marcando albumId=$albumId como loading")
                // Marcar como cargando
                val loadingMap = HashMap(_albumsState.value)
                loadingMap[albumId] = AlbumState(isLoading = true)
                _albumsState.value = loadingMap
                
                android.util.Log.d("CollectorViewModel", "loadAlbums: Llamando a albumRepository.getAlbum($albumId)")
                // Cargar el álbum desde el API
                val result = withContext(Dispatchers.IO) {
                    albumRepository.getAlbum(albumId)
                }
                
                result.onSuccess { album ->
                    android.util.Log.d("CollectorViewModel", "loadAlbums: ✅ Álbum $albumId cargado exitosamente: ${album.name}")
                    
                    val successMap = HashMap(_albumsState.value)
                    successMap[albumId] = AlbumState(album = album, isLoading = false)
                    _albumsState.value = successMap
                    android.util.Log.d("CollectorViewModel", "loadAlbums: Estado actualizado para $albumId, total álbumes: ${_albumsState.value.size}")
                }.onFailure { exception ->
                    android.util.Log.e("CollectorViewModel", "loadAlbums: ❌ Error cargando álbum $albumId", exception)
                    val errorMessage = when {
                        exception.message?.contains("Unable to resolve host") == true ->
                            "No se puede conectar al servidor"
                        exception.message?.contains("Failed to connect") == true ->
                            "Error de conexión"
                        exception.message?.contains("timeout") == true ->
                            "Tiempo de espera agotado"
                        exception.message?.contains("no encontrado") == true ->
                            "Álbum no encontrado"
                        else ->
                            exception.message ?: "Error desconocido al cargar el álbum"
                    }
                    
                    val errorMap = HashMap(_albumsState.value)
                    errorMap[albumId] = AlbumState(isLoading = false, error = errorMessage)
                    _albumsState.value = errorMap
                    android.util.Log.e("CollectorViewModel", "loadAlbums: Estado de error actualizado para $albumId")
                }
            }
            
            android.util.Log.d("CollectorViewModel", "loadAlbums: FIN - Estado final: ${_albumsState.value.size} álbumes, keys: ${_albumsState.value.keys}")
        }
    }
    
    /**
     * Limpia el estado de los álbumes cargados
     */
    fun clearAlbumsState() {
        _albumsState.value = emptyMap()
    }
    
    init {
        loadCollectors()
    }
}

