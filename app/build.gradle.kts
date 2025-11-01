plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"  // Para Room
}

android {
    namespace = "com.miso.vinilos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.miso.vinilos"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    
    // Coil for image loading
    implementation(libs.coil.compose)
    
    // Material Icons Extended
    implementation(libs.androidx.material.icons.extended)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // LiveData (opcional, si prefieres StateFlow no es necesario)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    
    // Room (persistencia local - solo si necesitas base de datos)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // DataStore (alternativa moderna a SharedPreferences)
    implementation(libs.androidx.datastore.preferences)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Screenshot testing - using custom implementation
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Task para copiar screenshots del dispositivo al directorio de reportes
tasks.register("copyScreenshotsToReports") {
    description = "Copia las screenshots del dispositivo al directorio de reportes de tests"
    group = "verification"
    
    doLast {
        val adbPath = project.findProperty("android.sdk.dir")?.toString()?.let { 
            "$it/platform-tools/adb" 
        } ?: run {
            // Intentar encontrar adb en ubicaciones comunes
            val homeDir = System.getProperty("user.home")
            val possiblePaths = listOf(
                "$homeDir/Library/Android/sdk/platform-tools/adb",
                "$homeDir/Android/Sdk/platform-tools/adb",
                System.getenv("ANDROID_HOME")?.let { "$it/platform-tools/adb" }
            )
            possiblePaths.firstOrNull { File(it).exists() } ?: "adb"
        }
        
        val adb = if (adbPath.endsWith("adb")) {
            // Si ya es solo "adb", asumir que está en PATH
            "adb"
        } else {
            adbPath
        }
        
        // Verificar que hay un dispositivo conectado
        val devicesProcess = ProcessBuilder(adb, "devices")
            .redirectErrorStream(true)
            .start()
        val devicesOutput = devicesProcess.inputStream.bufferedReader().readText()
        devicesProcess.waitFor()
        
        if (!devicesOutput.contains("device")) {
            println("⚠️  No hay dispositivos conectados.")
            println("   El directorio de screenshots se creará pero estará vacío.")
            println("   Ejecuta los tests con un dispositivo conectado para copiar las screenshots.")
            // No retornamos aquí, creamos el directorio de todas formas
        } else {
            println("✅ Dispositivo(s) conectado(s) detectado(s)")
        }
        
        // Crear el directorio de screenshots siempre, incluso si no hay dispositivo
        val reportsDir = File("${project.buildDir}/reports/androidTests/connected")
        reportsDir.mkdirs()
        
        // Buscar el directorio de debug o usar el primero disponible
        val debugDir = reportsDir.listFiles()?.firstOrNull { it.isDirectory && it.name == "debug" } 
            ?: reportsDir.listFiles()?.firstOrNull { it.isDirectory }
            ?: File(reportsDir, "debug")
        
        debugDir.mkdirs()
        val screenshotsDir = File(debugDir, "screenshots")
        screenshotsDir.mkdirs()
        
        println("📁 Directorio de screenshots creado: ${screenshotsDir.absolutePath}")
        
        println("📱 Copiando screenshots del dispositivo...")
        println("   ADB: $adb")
        println("   Destino: ${screenshotsDir.absolutePath}")
        
        // Primero verificar qué existe en el dispositivo
        val externalScreenshotPath = "/sdcard/Pictures/screenshots"
        
        // Verificar si el directorio existe en el dispositivo
        val checkProcess = ProcessBuilder(adb, "shell", "test", "-d", externalScreenshotPath)
            .redirectErrorStream(true)
            .start()
        val checkExitCode = checkProcess.waitFor()
        
        var copied = false
        
        if (checkExitCode == 0) {
            // Listar archivos para ver qué hay
            val listProcess = ProcessBuilder(adb, "shell", "ls", "-la", externalScreenshotPath)
                .redirectErrorStream(true)
                .start()
            val listOutput = listProcess.inputStream.bufferedReader().readText()
            listProcess.waitFor()
            
            println("📋 Contenido del directorio en dispositivo:")
            println(listOutput)
            
            // Intentar copiar desde almacenamiento externo
            println("📥 Copiando desde almacenamiento externo...")
            val pullProcess = ProcessBuilder(adb, "pull", externalScreenshotPath, screenshotsDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            // Capturar la salida para debug
            pullProcess.inputStream.bufferedReader().lines().forEach { line ->
                println("   $line")
            }
            
            val exitCode = pullProcess.waitFor()
            
            if (exitCode == 0) {
                copied = true
                println("✅ Copia desde almacenamiento externo exitosa")
            } else {
                println("⚠️  Error al copiar desde almacenamiento externo (código: $exitCode)")
            }
        } else {
            println("⚠️  Directorio externo no existe, intentando interno...")
        }
        
        if (!copied) {
            // Intentar copiar desde almacenamiento interno
            val internalScreenshotPath = "/data/data/com.miso.vinilos/files/screenshots"
            
            println("📥 Intentando copiar desde almacenamiento interno...")
            
            // Verificar si existe
            val checkInternalProcess = ProcessBuilder(adb, "shell", "run-as", "com.miso.vinilos", "test", "-d", internalScreenshotPath)
                .redirectErrorStream(true)
                .start()
            val checkInternalExitCode = checkInternalProcess.waitFor()
            
            if (checkInternalExitCode == 0) {
                // Listar archivos internos
                val listInternalProcess = ProcessBuilder(adb, "shell", "run-as", "com.miso.vinilos", "ls", "-la", internalScreenshotPath)
                    .redirectErrorStream(true)
                    .start()
                val listInternalOutput = listInternalProcess.inputStream.bufferedReader().readText()
                listInternalProcess.waitFor()
                
                println("📋 Contenido del directorio interno en dispositivo:")
                println(listInternalOutput)
                
                // Primero copiar a un lugar accesible en el dispositivo
                val copyProcess = ProcessBuilder(adb, "shell", "run-as", "com.miso.vinilos", "cp", "-r", internalScreenshotPath, "/sdcard/Download/screenshots-temp")
                    .redirectErrorStream(true)
                    .start()
                copyProcess.inputStream.bufferedReader().lines().forEach { line ->
                    println("   Copy: $line")
                }
                val copyExitCode = copyProcess.waitFor()
                
                if (copyExitCode == 0) {
                    // Luego copiar desde ahí
                    val pullInternalProcess = ProcessBuilder(adb, "pull", "/sdcard/Download/screenshots-temp", screenshotsDir.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                    
                    pullInternalProcess.inputStream.bufferedReader().lines().forEach { line ->
                        println("   Pull: $line")
                    }
                    val pullInternalExitCode = pullInternalProcess.waitFor()
                    
                    if (pullInternalExitCode == 0) {
                        copied = true
                        println("✅ Copia desde almacenamiento interno exitosa")
                    }
                    
                    // Limpiar archivos temporales
                    ProcessBuilder(adb, "shell", "rm", "-rf", "/sdcard/Download/screenshots-temp")
                        .start()
                        .waitFor()
                } else {
                    println("⚠️  Error al copiar archivos internos al storage temporal")
                }
            } else {
                println("⚠️  Directorio interno no existe o no se puede acceder")
            }
        }
        
        // Verificar qué archivos se copiaron
        val screenshotFiles = screenshotsDir.listFiles { file -> 
            file.isFile && file.name.endsWith(".png", ignoreCase = true)
        } ?: emptyArray()
        
        if (screenshotFiles.isNotEmpty()) {
            println("✅ ${screenshotFiles.size} screenshots copiadas exitosamente")
            screenshotFiles.forEach { file ->
                println("   - ${file.name} (${file.length()} bytes)")
            }
        } else {
            println("⚠️  No se encontraron screenshots para copiar")
            println("   Revisa los logs del test para ver dónde se guardaron las screenshots")
            println("   Busca mensajes que contengan 'ScreenshotTestRule' en los logs")
        }
    }
}

// Hacer que copyScreenshotsToReports se ejecute después de connectedAndroidTest
// Usamos afterEvaluate para que el task esté disponible cuando se configure
afterEvaluate {
    // Configurar para todos los tasks de tests conectados
    tasks.matching { task ->
        task.name.startsWith("connected") && task.name.contains("AndroidTest")
    }.configureEach {
        finalizedBy("copyScreenshotsToReports")
    }
    
    // También configurar específicamente los tasks principales
    try {
        tasks.named("connectedAndroidTest") {
            finalizedBy("copyScreenshotsToReports")
        }
    } catch (e: Exception) {
        // El task puede no existir, eso está bien
    }
    
    try {
        tasks.named("connectedDebugAndroidTest") {
            finalizedBy("copyScreenshotsToReports")
        }
    } catch (e: Exception) {
        // El task puede no existir, eso está bien
    }
}
