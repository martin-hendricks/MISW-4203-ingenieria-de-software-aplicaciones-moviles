# ingenieria-de-software-aplicaciones-moviles# Vinilos - Aplicación Móvil

Aplicación Android para la gestión y consulta de un catálogo de álbumes musicales. Desarrollada con Jetpack Compose siguiendo las mejores prácticas de arquitectura MVVM.

## Tabla de Contenidos

- [Descripción](#descripción)
- [Requisitos Previos](#requisitos-previos)
- [Configuración del Backend](#configuración-del-backend)
- [Compilación del Proyecto](#compilación-del-proyecto)
- [Instalación del APK](#instalación-del-apk)
  - [Windows](#windows)
  - [macOS](#macos)
  - [Linux](#linux)
- [Ejecución de Pruebas](#ejecución-de-pruebas)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Historias de Usuario Implementadas](#historias-de-usuario-implementadas)

---

## Descripción

Vinilos es una aplicación móvil Android que permite a los usuarios:
- Consultar un catálogo de álbumes musicales
- Ver detalles completos de cada álbum (información, canciones, comentarios)
- Navegar entre diferentes secciones (Álbumes, Artistas, Coleccionistas, Perfil)

La aplicación consume datos de un API REST backend y está diseñada siguiendo el patrón de arquitectura MVVM (Model-View-ViewModel) con Jetpack Compose.

---

## Requisitos Previos

Antes de compilar y ejecutar la aplicación, asegúrate de tener instalado:

- **Android Studio** (versión Hedgehog 2023.1.1 o superior)
- **JDK 21** o superior
- **Android SDK** con:
  - API Level 35 (Android 15) - para compilación
  - API Level 21 (Android 5.0) o superior - versión mínima soportada
  - Build Tools 35.0.0 o superior
  - Platform Tools (incluye `adb`)
- **Git** para clonar el repositorio

### Verificación de Android SDK

El Android SDK normalmente se encuentra en:
- **Windows:** `C:\Users\<Usuario>\AppData\Local\Android\Sdk`
- **macOS:** `~/Library/Android/sdk`
- **Linux:** `~/Android/Sdk`

---

## Configuración del Backend

**IMPORTANTE:** La aplicación requiere que el backend esté ejecutándose para funcionar correctamente.

### Repositorio Backend
https://github.com/TheSoftwareDesignLab/BackVynils

### Pasos para ejecutar el backend:

1. Clonar el repositorio del backend:
   ```bash
   git clone https://github.com/TheSoftwareDesignLab/BackVynils.git
   cd BackVynils
   ```

2. Ejecutar con Docker Compose:
   ```bash
   docker-compose up
   ```

3. El backend estará disponible en: `http://localhost:3000`

Para detener el servidor:
```bash
docker-compose down
```

**Requisitos previos:**
- Docker y Docker Compose instalados en tu sistema
- Para instalar Docker Desktop: https://www.docker.com/products/docker-desktop

### Configuración de la URL del API en la aplicación

La aplicación está configurada para conectarse al backend. Si necesitas cambiar la URL, edita el archivo:
```
app/src/main/java/com/miso/vinilos/config/NetworkConfig.kt
```

---

## Compilación del Proyecto

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd MISW-4203-ingenieria-de-software-aplicaciones-moviles
```

### 2. Compilar el APK de Debug

```bash
./gradlew assembleDebug
```

**Windows (PowerShell/CMD):**
```powershell
.\gradlew.bat assembleDebug
```

### 3. Ubicación del APK generado

El APK se generará en:
```
app/build/outputs/apk/debug/app-debug.apk
```

**Tamaño aproximado:** ~18 MB

---

### APK Pre-compilado

Si tienes problemas para compilar el proyecto, puedes descargar el APK ya compilado desde:

**Listado de APKs disponibles:** https://github.com/martin-hendricks/MISW-4203-ingenieria-de-software-aplicaciones-moviles/wiki/Listados-APK

Este listado incluye versiones de los APKs de las diferentes entregas del proyecto.

---

## Instalación del APK

### Requisitos previos para la instalación

- Un dispositivo Android físico con depuración USB habilitada, O
- Un emulador Android ejecutándose
- ADB (Android Debug Bridge) instalado y configurado

### Verificar dispositivos conectados

```bash
adb devices
```

Deberías ver algo como:
```
List of devices attached
emulator-5554    device
```

---

### Windows

#### Opción 1: Usando la ruta completa de ADB

```powershell
"C:\Users\<TuUsuario>\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

Reemplaza `<TuUsuario>` con tu nombre de usuario de Windows.

#### Opción 2: Agregar ADB al PATH

1. Abre PowerShell como Administrador
2. Ejecuta:
   ```powershell
   $env:Path += ";C:\Users\<TuUsuario>\AppData\Local\Android\Sdk\platform-tools"
   ```
3. Luego podrás usar:
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

#### Opción 3: Desde Android Studio

1. Abre el proyecto en Android Studio
2. Conecta tu dispositivo o inicia el emulador
3. Haz clic en el botón "Run" (▶️)
4. Selecciona el dispositivo destino

---

### macOS

#### Configurar ADB en el PATH (recomendado)

1. Abre Terminal
2. Edita tu archivo de perfil:
   ```bash
   nano ~/.zshrc
   # o si usas bash:
   nano ~/.bash_profile
   ```

3. Agrega la siguiente línea:
   ```bash
   export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
   ```

4. Guarda y cierra (Ctrl+X, Y, Enter)
5. Recarga el perfil:
   ```bash
   source ~/.zshrc
   # o
   source ~/.bash_profile
   ```

#### Instalar el APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Usando la ruta completa (alternativa)

```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Linux

#### Configurar ADB en el PATH (recomendado)

1. Abre Terminal
2. Edita tu archivo `.bashrc` o `.zshrc`:
   ```bash
   nano ~/.bashrc
   ```

3. Agrega la siguiente línea:
   ```bash
   export PATH="$PATH:$HOME/Android/Sdk/platform-tools"
   ```

4. Guarda y recarga:
   ```bash
   source ~/.bashrc
   ```

#### Configurar permisos USB (si usas dispositivo físico)

1. Crea el archivo de reglas udev:
   ```bash
   sudo nano /etc/udev/rules.d/51-android.rules
   ```

2. Agrega (ajusta según tu dispositivo):
   ```
   SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
   ```

3. Recarga las reglas:
   ```bash
   sudo udevadm control --reload-rules
   sudo udevadm trigger
   ```

#### Instalar el APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Usando la ruta completa (alternativa)

```bash
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Comandos Útiles de ADB

### Verificar dispositivos conectados
```bash
adb devices
```

### Iniciar la aplicación
```bash
adb shell am start -n com.miso.vinilos/.MainActivity
```

### Desinstalar la aplicación
```bash
adb uninstall com.miso.vinilos
```

### Ver logs en tiempo real
```bash
adb logcat | grep "Vinilos"
```

### Reinstalar la aplicación (forzar)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Ejecución de Pruebas

### Pruebas Unitarias

```bash
./gradlew test
```

### Pruebas de Instrumentación (E2E)

**IMPORTANTE:** Asegúrate de que el backend esté ejecutándose antes de ejecutar las pruebas E2E.

```bash
./gradlew connectedAndroidTest
```

### Ejecutar pruebas específicas

```bash
# Pruebas de HU001 (Catálogo de Álbumes)
./gradlew connectedAndroidTest --tests "com.miso.vinilos.AlbumListE2ETest"

# Pruebas de HU002 (Detalle de Álbum)
./gradlew connectedAndroidTest --tests "com.miso.vinilos.AlbumDetailE2ETest"

# Prueba específica
./gradlew connectedAndroidTest --tests "com.miso.vinilos.AlbumDetailE2ETest.testSuccessfulAlbumDetailLoading"
```

### Ver reportes de pruebas

Los reportes HTML se generan en:
```
app/build/reports/androidTests/connected/index.html

o

app/build/reports/androidTests/connected/debug/index.html

```

---

## Estructura del Proyecto

```
app/src/
├── main/
│   ├── java/com/miso/vinilos/
│   │   ├── config/              # Configuración de red y Retrofit
│   │   ├── model/
│   │   │   ├── data/           # Modelos de datos
│   │   │   └── repository/     # Repositorios
│   │   ├── viewmodels/         # ViewModels (lógica de UI)
│   │   └── views/
│   │       ├── navigation/     # Navegación de la app
│   │       ├── screens/        # Pantallas Compose
│   │       └── theme/          # Tema y colores
│   └── res/                    # Recursos (layouts, strings, etc.)
├── androidTest/                # Pruebas de instrumentación E2E
│   └── java/com/miso/vinilos/
│       ├── AlbumListE2ETest.kt
│       ├── AlbumDetailE2ETest.kt
│       ├── helpers/            # Helpers para pruebas
│       ├── matchers/           # Matchers personalizados
│       └── rules/              # Reglas de prueba (MockWebServer)
└── test/                       # Pruebas unitarias
```

---

## Historias de Usuario Implementadas

### HU001 - Consultar Catálogo de Álbumes
**Como** usuario visitante
**Quiero** navegar el catálogo de álbumes
**Para** escoger los que más me interesan

- ✓ Visualización del catálogo completo
- ✓ Estados de carga y error
- ✓ Scroll y navegación
- ✓ 14 pruebas E2E

**Documentación:** [HU001-Escenarios-Pruebas-E2E.md](docs/HU001-Escenarios-Pruebas-E2E.md)

### HU002 - Consultar Detalle de Álbum
**Como** usuario visitante o coleccionista
**Quiero** consultar el detalle completo de un álbum
**Para** conocer toda su información (descripción, canciones, comentarios, artistas, etc.)

- ✓ Visualización de detalle completo
- ✓ Información del álbum (cover, nombre, artistas)
- ✓ Detalles (descripción, género, sello, fecha)
- ✓ Lista de canciones con duraciones
- ✓ Comentarios con ratings
- ✓ Manejo de casos edge (sin tracks, sin comentarios, etc.)
- ✓ 14 pruebas E2E

**Documentación:** [HU002-Escenarios-Pruebas-E2E.md](docs/HU002-Escenarios-Pruebas-E2E.md)

---

## Tecnologías Utilizadas

- **Kotlin** 1.9.0
- **Jetpack Compose** - UI moderna y declarativa
- **Material Design 3** - Componentes de UI
- **Retrofit** - Cliente HTTP para API REST
- **OkHttp** - Logging de peticiones HTTP
- **Coil** - Carga de imágenes
- **Coroutines** - Programación asíncrona
- **Navigation Compose** - Navegación entre pantallas
- **ViewModel y LiveData** - Arquitectura MVVM
- **MockWebServer** - Pruebas E2E con mock de API
- **Espresso y Compose Testing** - Framework de pruebas

---

## Configuración de Red

La aplicación está configurada para conectarse a:
- **Base URL:** `http://10.0.2.2:3000/` (para emulador Android)
- Para dispositivo físico, usar la IP de tu computadora

### Cambiar la URL del backend

Edita `app/src/main/java/com/miso/vinilos/config/NetworkConfig.kt`:
```kotlin
const val BASE_URL = "http://TU_IP:3000/"
```

---

## Solución de Problemas

### El APK no se instala
- Verifica que el dispositivo/emulador esté conectado: `adb devices`
- Desinstala la versión anterior: `adb uninstall com.miso.vinilos`
- Intenta instalar de nuevo con `-r`: `adb install -r app-debug.apk`

### La app no carga datos
- Verifica que el backend esté ejecutándose: `http://localhost:3000`
- Revisa la configuración de red en `NetworkConfig.kt`
- Para emulador: usa `10.0.2.2` en lugar de `localhost`
- Para dispositivo físico: usa la IP de tu computadora

### Las pruebas E2E fallan
- Asegúrate de que el backend esté ejecutándose
- Verifica que no haya otros servicios usando el puerto 3000
- Limpia el proyecto: `./gradlew clean`
- Reinicia el emulador

---

## Contribuciones

Este proyecto es parte del curso de Ingeniería de Software para Aplicaciones Móviles de la Universidad de los Andes.

---

## Licencia

Este proyecto es de uso académico.