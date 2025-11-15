package com.miso.vinilos.examples

/**
 * EJEMPLOS DE USO DE LAS MEJORAS DE CORRUTINAS
 *
 * Este archivo contiene ejemplos de cómo usar las nuevas características
 * implementadas en los ViewModels mejorados.
 */

// ============================================================================
// EJEMPLO 1: Uso de AlbumViewModel con estados mejorados
// ============================================================================

/*
@Composable
fun AlbumListScreen(viewModel: AlbumViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Pull to refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshAlbums() }
    )

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        when (val state = uiState) {
            is AlbumUiState.Loading -> {
                LoadingIndicator()
            }

            is AlbumUiState.Success -> {
                LazyColumn {
                    items(state.albums) { album ->
                        AlbumItem(album = album)
                    }
                }
            }

            is AlbumUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = if (state.canRetry) {
                        { viewModel.retryLoadAlbums() }
                    } else null
                )
            }

            is AlbumUiState.Empty -> {
                EmptyState(
                    message = "No hay álbumes disponibles",
                    icon = Icons.Default.Album
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
*/

// ============================================================================
// EJEMPLO 2: Uso de MusicianViewModel con carga paralela de premios
// ============================================================================

/*
@Composable
fun MusicianDetailScreen(
    musicianId: Int,
    viewModel: MusicianViewModel = viewModel()
) {
    val musicianState by viewModel.musicianDetailState.collectAsState()
    val prizesState by viewModel.prizesState.collectAsState()

    LaunchedEffect(musicianId) {
        viewModel.loadMusicianDetail(musicianId)
    }

    when (val state = musicianState) {
        is MusicianDetailUiState.Loading -> {
            LoadingIndicator()
        }

        is MusicianDetailUiState.Success -> {
            val musician = state.musician

            // Iniciar carga paralela de premios
            LaunchedEffect(musician.performerPrizes) {
                viewModel.loadPrizes(musician.performerPrizes)
            }

            Column {
                MusicianHeader(musician)

                // Mostrar información del músico
                MusicianInfo(musician)

                // Mostrar premios con estados individuales
                Text("Premios", style = MaterialTheme.typography.headlineSmall)

                musician.performerPrizes.forEach { performerPrize ->
                    val prizeId = performerPrize.prize?.id ?: performerPrize.prizeId
                    prizeId?.let { id ->
                        val prizeState = prizesState[id]

                        when {
                            prizeState?.isLoading == true -> {
                                PrizeLoadingItem()
                            }
                            prizeState?.prize != null -> {
                                PrizeItem(
                                    prize = prizeState.prize,
                                    premiationDate = performerPrize.premiationDate
                                )
                            }
                            prizeState?.error != null -> {
                                PrizeErrorItem(error = prizeState.error)
                            }
                        }
                    }
                }
            }
        }

        is MusicianDetailUiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = if (state.canRetry) {
                    { viewModel.loadMusicianDetail(musicianId) }
                } else null
            )
        }
    }
}
*/

// ============================================================================
// EJEMPLO 3: Componente de Error Reutilizable
// ============================================================================

/*
@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reintentar"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}
*/

// ============================================================================
// EJEMPLO 4: Componente de Estado Vacío
// ============================================================================

/*
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Vacío",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
*/

// ============================================================================
// EJEMPLO 5: Uso de Use Cases (Opcional - Para lógica compleja)
// ============================================================================

/*
class AlbumViewModelWithUseCase(
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbums() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = AlbumUiState.Loading

            getAlbumsUseCase()
                .onSuccess { albums ->
                    if (albums.isEmpty()) {
                        _uiState.value = AlbumUiState.Empty
                    } else {
                        _uiState.value = AlbumUiState.Success(albums)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = AlbumUiState.Error(
                        message = exception.message ?: "Error desconocido",
                        canRetry = true
                    )
                }
        }
    }
}
*/

// ============================================================================
// EJEMPLO 6: Testing con Dispatchers Inyectables
// ============================================================================

/*
class AlbumViewModelTest {

    @Test
    fun `loadAlbums should emit success state when repository returns data`() = runTest {
        // Arrange
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testDispatcherProvider = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
            override val unconfined = testDispatcher
        }

        val mockRepository = mockk<AlbumRepository>()
        val testAlbums = listOf(
            Album(id = 1, name = "Test Album", ...)
        )
        coEvery { mockRepository.getAlbums() } returns Result.success(testAlbums)

        val viewModel = AlbumViewModel(
            repository = mockRepository,
            dispatcher = testDispatcher
        )

        // Act
        viewModel.loadAlbums()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is AlbumUiState.Success)
        assertEquals(testAlbums, (state as AlbumUiState.Success).albums)
    }
}
*/

// ============================================================================
// EJEMPLO 7: Uso de CoroutineUtils
// ============================================================================

/*
class MyRepository {

    suspend fun getDataWithRetry(): Result<Data> {
        return try {
            // Ejecutar con retry automático
            val data = CoroutineUtils.retryWithBackoff(
                maxAttempts = 3,
                initialDelay = 1000L,
                maxDelay = 5000L
            ) {
                apiService.getData()
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDataFlow(): Flow<Result<Data>> {
        // Flow con retry automático
        return CoroutineUtils.flowWithRetry {
            apiService.getData()
        }
    }

    suspend fun loadMultipleResources(): List<Resource> {
        val operations = listOf(
            { apiService.getResource1() },
            { apiService.getResource2() },
            { apiService.getResource3() }
        )

        // Ejecuta todas y retorna solo las exitosas
        return CoroutineUtils.executeAllIgnoreFailures(operations)
    }
}
*/

// ============================================================================
// NOTAS IMPORTANTES
// ============================================================================

/*
1. DISPATCHERS:
   - Dispatchers.Main: Para actualizar la UI
   - Dispatchers.IO: Para operaciones de red y base de datos
   - Dispatchers.Default: Para procesamiento CPU intensivo

2. TIMEOUT:
   - Todas las operaciones de red tienen timeout de 30 segundos
   - Se pueden configurar en las constantes del ViewModel

3. RETRY:
   - Máximo 3 intentos por defecto
   - Usa backoff exponencial (1s, 2s, 4s)
   - Solo reintenta errores recuperables (timeout, conexión)

4. ESTADOS:
   - Loading: Cargando datos
   - Success: Datos cargados correctamente
   - Error: Error con mensaje y opción de retry
   - Empty: Sin datos disponibles (nuevo)

5. CARGA PARALELA:
   - Usa async/await para operaciones independientes
   - Mejora significativa en rendimiento
   - Especialmente útil para cargar premios de músicos

6. TESTING:
   - Usa TestDispatcherProvider para tests
   - Inyecta repositorios mock
   - Usa runTest para tests de coroutinas
*/

