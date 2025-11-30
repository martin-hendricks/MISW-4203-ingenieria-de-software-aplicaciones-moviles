package com.miso.vinilos.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import com.miso.vinilos.views.theme.Yellow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miso.vinilos.model.data.AlbumCreateDTO
import com.miso.vinilos.model.data.Genre
import com.miso.vinilos.model.data.RecordLabel
import com.miso.vinilos.viewmodels.AlbumViewModel
import com.miso.vinilos.viewmodels.CreateAlbumUiState
import com.miso.vinilos.views.theme.DarkGreen
import java.util.*
import java.util.TimeZone

/**
 * Pantalla para crear un nuevo álbum
 * Implementa el patrón MVVM con Jetpack Compose
 *
 * @param albumViewModel ViewModel que gestiona el estado de los álbumes
 * @param onBack Callback que se ejecuta cuando se cancela o completa la creación
 */
@Composable
fun CreateAlbumScreen(
    albumViewModel: AlbumViewModel,
    onBack: () -> Unit
) {
    val createAlbumState by albumViewModel.createAlbumUiState.collectAsStateWithLifecycle()
    
    // Observar el estado de creación
    LaunchedEffect(createAlbumState) {
        if (createAlbumState is CreateAlbumUiState.Success) {
            // El ViewModel ya llama a refreshAlbums() después de crear exitosamente
            // Esperar un momento para que el refresh se inicie
            kotlinx.coroutines.delay(300)
            // Navegar de vuelta después de crear exitosamente
            onBack()
        }
    }

    Scaffold(
        containerColor = DarkGreen, // Fondo explícito con buen contraste
        topBar = {
            // Barra superior personalizada con componentes estables
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Crear Nuevo Álbum",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        when (val currentState = createAlbumState) {
            is CreateAlbumUiState.Idle -> {
                CreateAlbumForm(
                    albumViewModel = albumViewModel,
                    onBack = onBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is CreateAlbumUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Creando álbum...",
                            color = Color.White // Color explícito para buen contraste
                        )
                    }
                }
            }
            is CreateAlbumUiState.Success -> {
                // Este estado se maneja en LaunchedEffect para navegar
            }
            is CreateAlbumUiState.Error -> {
                CreateAlbumForm(
                    albumViewModel = albumViewModel,
                    onBack = onBack,
                    errorMessage = currentState.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Formulario para crear un álbum
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAlbumForm(
    albumViewModel: AlbumViewModel,
    onBack: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    // Estados del formulario
    var name by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<Genre?>(null) }
    var selectedRecordLabel by remember { mutableStateOf<RecordLabel?>(null) }
    
    // Estado para la fecha (formato dd/mm/aaaa)
    var dateText by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGreen) // Fondo explícito para asegurar buen contraste
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mensaje de error
        errorMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Campo: Nombre
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del Álbum", color = Color.White) },
            placeholder = { Text("Ingrese el nombre del álbum", color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF4A4940),
                unfocusedContainerColor = Color(0xFF4A4940),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Yellow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        // Campo: Portada (URL)
        OutlinedTextField(
            value = cover,
            onValueChange = { cover = it },
            label = { Text("URL de la Portada", color = Color.White) },
            placeholder = { Text("https://ejemplo.com/portada.jpg", color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF4A4940),
                unfocusedContainerColor = Color(0xFF4A4940),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Yellow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        // Campo: Fecha de lanzamiento
        OutlinedTextField(
            value = dateText,
            onValueChange = { },
            label = { Text("Fecha de Lanzamiento", color = Color.White) },
            placeholder = { Text("dd/mm/aaaa", color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePickerDialog = true },
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showDatePickerDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Calendario",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF4A4940),
                unfocusedContainerColor = Color(0xFF4A4940),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Yellow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        // Campo: Género
        var genreExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedGenre?.displayName ?: "",
                onValueChange = { },
                label = { Text("Género", color = Color.White) },
                placeholder = { Text("Seleccione un género", color = Color.White.copy(alpha = 0.6f)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { genreExpanded = !genreExpanded }) {
                        Icon(
                            imageVector = if (genreExpanded) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = if (genreExpanded) "Ocultar" else "Mostrar",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { genreExpanded = !genreExpanded },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF4A4940),
                    unfocusedContainerColor = Color(0xFF4A4940),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Yellow,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )
            DropdownMenu(
                expanded = genreExpanded,
                onDismissRequest = { genreExpanded = false },
                modifier = Modifier.fillMaxWidth(),
                containerColor = DarkGreen
            ) {
                Genre.entries.forEach { genre ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                genre.displayName,
                                color = Color.White
                            ) 
                        },
                        onClick = {
                            selectedGenre = genre
                            genreExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color.White,
                            leadingIconColor = Color.White,
                            trailingIconColor = Color.White
                        )
                    )
                }
            }
        }

        // Campo: Sello discográfico
        var recordLabelExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedRecordLabel?.displayName ?: "",
                onValueChange = { },
                label = { Text("Disquera", color = Color.White) },
                placeholder = { Text("Seleccione una disquera", color = Color.White.copy(alpha = 0.6f)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { recordLabelExpanded = !recordLabelExpanded }) {
                        Icon(
                            imageVector = if (recordLabelExpanded) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = if (recordLabelExpanded) "Ocultar" else "Mostrar",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { recordLabelExpanded = !recordLabelExpanded },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF4A4940),
                    unfocusedContainerColor = Color(0xFF4A4940),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Yellow,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )
            DropdownMenu(
                expanded = recordLabelExpanded,
                onDismissRequest = { recordLabelExpanded = false },
                modifier = Modifier.fillMaxWidth(),
                containerColor = DarkGreen
            ) {
                RecordLabel.entries.forEach { label ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                label.displayName,
                                color = Color.White
                            ) 
                        },
                        onClick = {
                            selectedRecordLabel = label
                            recordLabelExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color.White,
                            leadingIconColor = Color.White,
                            trailingIconColor = Color.White
                        )
                    )
                }
            }
        }

        // Campo: Descripción
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción", color = Color.White) },
            placeholder = { Text("Ingrese una descripción del álbum", color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF4A4940),
                unfocusedContainerColor = Color(0xFF4A4940),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Yellow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )


        Spacer(modifier = Modifier.height(24.dp))

        // Botón de crear
        Button(
            onClick = {
                // Validar campos
                if (validateForm(name, cover, dateText, description, selectedGenre, selectedRecordLabel)) {
                    // Convertir fecha a Date
                    val selectedDate = try {
                        val parts = dateText.split("/")
                        if (parts.size == 3) {
                            val day = parts[0].toInt()
                            val month = parts[1].toInt()
                            val year = parts[2].toInt()
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month - 1) // Mes es 0-indexed
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            calendar.time
                        } else {
                            Date() // Fallback a fecha actual
                        }
                    } catch (e: Exception) {
                        Date() // Fallback a fecha actual
                    }
                    
                    // Crear DTO
                    val albumDTO = AlbumCreateDTO(
                        name = name.trim(),
                        cover = cover.trim(),
                        releaseDate = selectedDate,
                        description = description.trim(),
                        genre = selectedGenre!!,
                        recordLabel = selectedRecordLabel!!
                    )
                    
                    // Llamar al ViewModel
                    albumViewModel.createAlbum(albumDTO)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Color.Black
            ),
            enabled = name.isNotBlank() && 
                     cover.isNotBlank() && 
                     dateText.isNotBlank() && 
                     description.isNotBlank() && 
                     selectedGenre != null && 
                     selectedRecordLabel != null
        ) {
            Text(
                text = "Crear Álbum",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
    
    // DatePicker Dialog - debe estar fuera del Column
    if (showDatePickerDialog) {
        Dialog(onDismissRequest = { showDatePickerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkGreen
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título
                    Text(
                        text = "Seleccionar fecha de lanzamiento",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // DatePicker con altura fija para evitar recorte
                    // Usar minimumInteractiveComponentSize para asegurar que los elementos clicables
                    // (como las flechas de cambio de mes) tengan al menos 48dp según recomendaciones de accesibilidad
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides 48.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                    
                    // Botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    // El DatePicker devuelve el timestamp en UTC a medianoche
                                    // Extraer la fecha directamente desde UTC para evitar desfases
                                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                    calendar.timeInMillis = millis
                                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                                    val month = calendar.get(Calendar.MONTH) + 1 // Mes es 0-indexed
                                    val year = calendar.get(Calendar.YEAR)
                                    dateText = String.format("%02d/%02d/%04d", day, month, year)
                                }
                                showDatePickerDialog = false
                            }
                        ) {
                            Text("OK", color = Yellow)
                        }
                    }
                }
            }
        }
    }

}

/**
 * Formatea la entrada de fecha mientras el usuario escribe
 */
private fun formatDateInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    return when {
        digits.isEmpty() -> ""
        digits.length <= 2 -> digits
        digits.length <= 4 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
        else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4, minOf(8, digits.length))}"
    }
}

/**
 * Valida que todos los campos requeridos estén completos
 */
private fun validateForm(
    name: String,
    cover: String,
    dateText: String,
    description: String,
    genre: Genre?,
    recordLabel: RecordLabel?
): Boolean {
    // Validar que la fecha sea válida (formato dd/mm/aaaa)
    val isValidDate = try {
        val parts = dateText.split("/")
        if (parts.size == 3) {
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            day in 1..31 && month in 1..12 && year > 1900 && year <= Calendar.getInstance().get(Calendar.YEAR) + 10
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
    
    return name.isNotBlank() &&
           cover.isNotBlank() &&
           dateText.isNotBlank() &&
           isValidDate &&
           description.isNotBlank() &&
           genre != null &&
           recordLabel != null
}

