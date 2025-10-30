# HU001 - Consultar Catálogo de Álbumes
## Escenarios de Pruebas E2E

### Historia de Usuario
**Como** usuario visitante
**Quiero** navegar el catálogo de álbumes
**Para** escoger los que más me interesan

---

## Escenarios de Prueba

### E2E-HU001-01: Carga Exitosa del Catálogo de Álbumes
**Descripción:** Verifica que el catálogo de álbumes se carga y muestra correctamente cuando el servicio responde exitosamente.

**Validaciones:**
- Se muestra el título "Álbumes"
- Los álbumes se visualizan con su información completa (nombre, imagen, artista)
- La barra de navegación inferior está presente
- Los datos coinciden con la respuesta del API

**Método de prueba:** `testSuccessfulAlbumListLoading()`

---

### E2E-HU001-02: Visualización del Estado de Carga
**Descripción:** Verifica que se muestra un indicador visual mientras se cargan los álbumes del catálogo.

**Validaciones:**
- Se muestra un indicador de carga circular
- Se muestra el texto "Cargando..."
- El indicador es visible antes de que aparezcan los datos

**Método de prueba:** `testLoadingStateDisplay()`

---

### E2E-HU001-03: Manejo de Errores en la Carga
**Descripción:** Verifica que se muestra un mensaje de error apropiado cuando el servicio falla al cargar el catálogo.

**Validaciones:**
- Se muestra un mensaje de error descriptivo
- Se presenta un botón "Reintentar"
- El indicador de carga desaparece
- El mensaje de error es claro para el usuario

**Método de prueba:** `testErrorStateDisplay()`

---

### E2E-HU001-04: Funcionalidad de Reintentar Después de Error
**Descripción:** Verifica que el usuario puede reintentar cargar el catálogo después de un error y que la aplicación recupera los datos exitosamente.

**Validaciones:**
- El botón "Reintentar" es funcional
- Al hacer clic en "Reintentar", se vuelve a cargar el catálogo
- Los álbumes se muestran correctamente después del reintento
- La transición entre estado de error y éxito funciona correctamente

**Método de prueba:** `testErrorRetryFunctionality()`

---

### E2E-HU001-05: Visualización de Catálogo Vacío
**Descripción:** Verifica el comportamiento de la aplicación cuando el servicio retorna un catálogo sin álbumes.

**Validaciones:**
- Se muestra el título "Álbumes"
- No se muestran álbumes en la lista
- La barra de navegación permanece visible
- No se muestra mensaje de error (diferente del escenario E2E-HU001-03)

**Método de prueba:** `testEmptyListHandling()`

---

### E2E-HU001-06: Visualización Detallada de Álbumes
**Descripción:** Verifica que cada álbum en el catálogo muestra correctamente todos sus elementos visuales (nombre, imagen, artistas).

**Validaciones:**
- El nombre del álbum es visible
- La imagen del álbum se carga y muestra
- Los nombres de los artistas/performers son visibles
- El formato de presentación es consistente para todos los álbumes

**Método de prueba:** `testAlbumItemDisplay()`

---

### E2E-HU001-07: Navegación en el Catálogo (Desplazamiento)
**Descripción:** Verifica que el usuario puede desplazarse por el catálogo para ver todos los álbumes disponibles.

**Validaciones:**
- Los álbumes iniciales son visibles
- Es posible hacer scroll en la lista
- Los álbumes adicionales se hacen visibles al desplazarse
- El scroll funciona de manera fluida

**Método de prueba:** `testAlbumListScrolling()`

---

### E2E-HU001-08: Visualización de Álbum sin Artista Conocido
**Descripción:** Verifica que se maneja correctamente la visualización de álbumes que no tienen información de artista.

**Validaciones:**
- El álbum se muestra correctamente
- Se indica "Artista desconocido" cuando no hay información de performers
- El resto de la información del álbum es visible
- La aplicación no presenta errores

**Método de prueba:** `testUnknownArtistDisplay()`

---

### E2E-HU001-09: Visualización de Álbum con Múltiples Artistas
**Descripción:** Verifica que se muestran correctamente los álbumes que tienen múltiples artistas asociados.

**Validaciones:**
- El álbum con múltiples performers se muestra
- Al menos uno de los performers es visible
- El formato de presentación maneja múltiples artistas apropiadamente

**Método de prueba:** `testMultiplePerformersDisplay()`

---

### E2E-HU001-10: Presencia de Barra de Navegación
**Descripción:** Verifica que la barra de navegación inferior está presente y permite acceder a otras secciones de la aplicación.

**Validaciones:**
- La barra de navegación inferior es visible
- Las pestañas "Álbumes", "Artistas", "Coleccionistas" y "Perfil" están presentes
- Las pestañas son interactuables

**Método de prueba:** `testNavigationBarPresent()`

---

### E2E-HU001-11: Visualización para Usuario Coleccionista
**Descripción:** Verifica que la pantalla del catálogo se muestra correctamente para usuarios con rol de coleccionista.

**Validaciones:**
- La pantalla se carga correctamente
- El catálogo de álbumes es visible
- La navegación está presente
- La interfaz es funcional para este tipo de usuario

**Método de prueba:** `testCollectorRoleAddButtonVisible()`

---

### E2E-HU001-12: Visualización para Usuario Visitante
**Descripción:** Verifica que la pantalla del catálogo se muestra correctamente para usuarios con rol de visitante.

**Validaciones:**
- La pantalla se carga correctamente
- El catálogo de álbumes es visible
- La navegación está presente
- La interfaz es funcional para este tipo de usuario

**Método de prueba:** `testVisitorRoleAddButtonHidden()`

---

## Escenarios Básicos de Infraestructura

### E2E-BASIC-01: Inicio Exitoso de la Aplicación
**Descripción:** Verifica que la aplicación se inicia correctamente sin errores fatales.

**Validaciones:**
- La aplicación se lanza sin crashes
- La raíz de la UI existe
- No hay errores en tiempo de inicialización

**Archivo:** `BasicE2ETest.kt` - `testAppStartsSuccessfully()`

---

### E2E-BASIC-02: Navegación Entre Pestañas
**Descripción:** Verifica que el usuario puede navegar entre las diferentes secciones de la aplicación.

**Validaciones:**
- La navegación entre pestañas es funcional
- No hay crashes al cambiar de pestaña
- La interfaz responde correctamente

**Archivo:** `BasicE2ETest.kt` - `testNavigationBetweenTabs()`

---

## Información Técnica

**Framework de Pruebas:** Jetpack Compose Testing API
**Ubicación:** `app/src/androidTest/java/com/miso/vinilos/`
**Archivo Principal:** `AlbumListE2ETest.kt`
**Total de Escenarios:** 14 escenarios de prueba
**Resultado de Ejecución:** ✓ 19 pruebas ejecutadas - 0 fallidas

**Comando de Ejecución:**
```bash
./gradlew.bat connectedAndroidTest
```

---

**Última actualización:** 2025-10-29
**Estado:** ✓ Todas las pruebas pasando
