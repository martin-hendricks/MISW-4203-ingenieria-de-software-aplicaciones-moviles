# Guía: Testing de Corrutinas en Android - Solución Implementada

## 📋 Resumen del Problema

### Problema Original
Los tests E2E estaban fallando porque las corrutinas en los ViewModels se ejecutaban de forma asíncrona en hilos secundarios. Esto causaba que:
- Los tests verificaran elementos de UI antes de que las corrutinas completaran la carga
- `waitForIdle()` NO esperaba a que las corrutinas terminaran
- Los tests eran flaky (a veces pasaban, a veces fallaban)

### Causa Raíz
```kotlin
// En los ViewModels
viewModelScope.launch(dispatcher) {  // <- Hilo secundario, no bloqueante
    _uiState.value = AlbumUiState.Loading
    // ... operaciones asíncronas
}

// En los tests
composeTestRule.setContent { /* ... */ }
composeTestRule.waitForIdle()  // ❌ NO espera corrutinas del ViewModel
// Assertions fallan porque los datos aún no se cargaron
```

---

## ✅ Solución Implementada

### 1. TestDispatcherRule

Creamos un JUnit Rule que reemplaza el dispatcher Main con un `TestDispatcher`:

**Archivo**: `app/src/androidTest/java/com/miso/vinilos/rules/TestDispatcherRule.kt`

```kotlin
class TestDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)  // Reemplazar Main dispatcher
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()  // Restaurar después del test
    }

    fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()  // Ejecutar todas las corrutinas pendientes
    }
}
```

### 2. Configuración en Tests

**Antes:**
```kotlin
class AlbumListE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createTestViewModel(): AlbumViewModel {
        return AlbumViewModel(testRepository, Dispatchers.Unconfined)  // ❌ Problemático
    }
}
```

**Después:**
```kotlin
class AlbumListE2ETest {
    @get:Rule(order = 0)  // ⭐ Ejecutar PRIMERO
    val testDispatcherRule = TestDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createTestViewModel(): AlbumViewModel {
        return AlbumViewModel(testRepository, Dispatchers.Main)  // ✅ Controlado por TestDispatcherRule
    }
}
```

### 3. Uso en Tests

**Antes:**
```kotlin
@Test
fun testSuccessfulLoading() = runTest {
    composeTestRule.setContent { /* ... */ }
    composeTestRule.waitForIdle()  // ❌ No espera corrutinas

    // Assertions - pueden fallar
    CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
}
```

**Después:**
```kotlin
@Test
fun testSuccessfulLoading() = runTest {
    composeTestRule.setContent { /* ... */ }

    // ⭐ CLAVE: Esperar a que las corrutinas se completen
    testDispatcherRule.advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Assertions - ahora son confiables
    CustomMatchers.verifyAlbumIsVisible(composeTestRule, "Abbey Road")
}
```

---

## 📁 Archivos Actualizados

### ✅ Completados
1. **TestDispatcherRule.kt** - Rule personalizado para testing de corrutinas
2. **AlbumListE2ETest.kt** - Tests de listado de álbumes
3. **ArtistListE2ETest.kt** - Tests de listado de artistas
4. **CollectorListE2ETest.kt** - Tests de listado de coleccionistas

### 📝 Pendientes de Actualizar
Los siguientes archivos siguen el mismo patrón:
- AlbumDetailE2ETest.kt
- ArtistDetailE2ETest.kt
- CollectorDetailE2ETest.kt
- CreateAlbumE2ETest.kt
- AssociateAlbumToArtistE2ETest.kt
- SimpleE2ETest.kt
- BasicE2ETest.kt

---

## 🔧 Cómo Aplicar a Tests Adicionales

Para actualizar un test nuevo o existente:

### Paso 1: Agregar imports
```kotlin
import com.miso.vinilos.rules.TestDispatcherRule
import kotlinx.coroutines.Dispatchers
```

### Paso 2: Agregar el rule con order = 0
```kotlin
@get:Rule(order = 0)
val testDispatcherRule = TestDispatcherRule()
```

### Paso 3: Cambiar Dispatchers.Unconfined a Dispatchers.Main
```kotlin
// Antes
return ViewModel(repository, Dispatchers.Unconfined)

// Después
return ViewModel(repository, Dispatchers.Main)
```

### Paso 4: Agregar advanceUntilIdle() antes de assertions
```kotlin
// Después de setContent
composeTestRule.setContent { /* ... */ }

// ⭐ AGREGAR ESTAS LÍNEAS
testDispatcherRule.advanceUntilIdle()
composeTestRule.waitForIdle()

// Ahora hacer assertions
CustomMatchers.verify...()
```

---

## 🎯 Cuándo Usar `advanceUntilIdle()`

### ✅ USAR en estos casos:

1. **Después de setContent**
```kotlin
composeTestRule.setContent { /* ... */ }
testDispatcherRule.advanceUntilIdle()  // ✅
composeTestRule.waitForIdle()
```

2. **Después de clicks que disparan corrutinas**
```kotlin
composeTestRule.onNodeWithText("Reintentar").performClick()
testDispatcherRule.advanceUntilIdle()  // ✅
composeTestRule.waitForIdle()
```

3. **Después de navegación**
```kotlin
composeTestRule.onNodeWithText("Artistas").performClick()
testDispatcherRule.advanceUntilIdle()  // ✅
composeTestRule.waitForIdle()
```

### ❌ NO USAR en estos casos:

1. **Entre múltiples assertions (una vez que ya cargó)**
```kotlin
testDispatcherRule.advanceUntilIdle()
composeTestRule.waitForIdle()

CustomMatchers.verifyAlbumIsVisible(...)
// ❌ NO hace falta aquí
CustomMatchers.verifyPerformerIsVisible(...)
```

2. **Para operaciones puramente de UI (scroll, etc.)**
```kotlin
composeTestRule.onNode(...).performScrollTo()
// ❌ NO hace falta advanceUntilIdle aquí
```

---

## 📊 Comparación: Antes vs Después

| Aspecto | Antes (Dispatchers.Unconfined) | Después (TestDispatcherRule) |
|---------|--------------------------------|------------------------------|
| **Timing** | Impredecible, race conditions | Determinístico, controlado |
| **Flakiness** | Tests fallan aleatoriamente | Tests consistentes |
| **Debugging** | Difícil encontrar el problema | Claro cuándo esperar |
| **Mantenibilidad** | Código frágil | Código robusto |
| **Best Practice** | ❌ No recomendado | ✅ Recomendado por Google |

---

## 🐛 Debugging: Si los Tests Siguen Fallando

### 1. Verificar Order de los Rules
```kotlin
@get:Rule(order = 0)  // TestDispatcherRule DEBE ser 0
val testDispatcherRule = TestDispatcherRule()

@get:Rule(order = 1)
val composeTestRule = createAndroidComposeRule<ComponentActivity>()
```

### 2. Verificar que usas Dispatchers.Main
```kotlin
// ❌ MAL
return ViewModel(repository, Dispatchers.Unconfined)
return ViewModel(repository, Dispatchers.IO)

// ✅ BIEN
return ViewModel(repository, Dispatchers.Main)
```

### 3. Agregar logs para debug
```kotlin
testDispatcherRule.advanceUntilIdle()
println("⏰ Coroutines completed")
composeTestRule.waitForIdle()
println("🎨 Compose idle")
```

### 4. Aumentar timeout si es necesario
```kotlin
composeTestRule.waitUntil(timeoutMillis = 10_000) {
    // condición
}
```

---

## 📚 Referencias

- [Android Testing with Coroutines](https://developer.android.com/kotlin/coroutines/test)
- [TestDispatcher Documentation](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Compose Testing Guide](https://developer.android.com/jetpack/compose/testing)

---

## ✅ Checklist para Nuevos Tests

- [ ] Agregar `TestDispatcherRule` con `order = 0`
- [ ] Usar `Dispatchers.Main` en ViewModels de test
- [ ] Llamar `advanceUntilIdle()` después de operaciones asíncronas
- [ ] Llamar `waitForIdle()` después de `advanceUntilIdle()`
- [ ] Verificar que tests pasan consistentemente (correr 5+ veces)

---

**Fecha**: 2025-01-23
**Autor**: Claude + Equipo Vinilos
**Versión**: 1.0
