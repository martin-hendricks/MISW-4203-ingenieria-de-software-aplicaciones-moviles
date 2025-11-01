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
        
        val reportsDir = File("${project.buildDir}/reports/androidTests/connected/debug")
        val screenshotsDir = File(reportsDir, "screenshots")
        screenshotsDir.mkdirs()
        
        println("📱 Copiando screenshots del dispositivo...")
        println("   ADB: $adb")
        println("   Destino: ${screenshotsDir.absolutePath}")
        
        // Intentar copiar desde almacenamiento externo
        val externalScreenshotPath = "/sdcard/Pictures/screenshots"
        val process = ProcessBuilder(adb, "pull", externalScreenshotPath, screenshotsDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            println("⚠️  No se encontraron screenshots en almacenamiento externo")
            println("   Intentando almacenamiento interno...")
            
            // Intentar copiar desde almacenamiento interno
            val internalScreenshotPath = "/data/data/com.miso.vinilos/files/screenshots"
            
            // Primero copiar a un lugar accesible en el dispositivo
            ProcessBuilder(adb, "shell", "run-as", "com.miso.vinilos", "cp", "-r", internalScreenshotPath, "/sdcard/Download/screenshots-temp")
                .start()
                .waitFor()
            
            // Luego copiar desde ahí
            ProcessBuilder(adb, "pull", "/sdcard/Download/screenshots-temp", screenshotsDir.absolutePath)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            
            // Limpiar archivos temporales
            ProcessBuilder(adb, "shell", "rm", "-rf", "/sdcard/Download/screenshots-temp")
                .start()
                .waitFor()
        }
        
        val screenshotFiles = screenshotsDir.listFiles { file -> 
            file.name.endsWith(".png", ignoreCase = true)
        }
        
        if (screenshotFiles != null && screenshotFiles.isNotEmpty()) {
            println("✅ ${screenshotFiles.size} screenshots copiadas exitosamente")
            screenshotFiles.forEach { file ->
                println("   - ${file.name}")
            }
        } else {
            println("⚠️  No se encontraron screenshots para copiar")
            println("   Asegúrate de haber ejecutado los tests primero")
        }
    }
}

// Hacer que copyScreenshotsToReports se ejecute después de connectedAndroidTest
// Usamos afterEvaluate para que el task esté disponible cuando se configure
afterEvaluate {
    tasks.named("connectedAndroidTest") {
        finalizedBy("copyScreenshotsToReports")
    }
    
    // También configurar para el task específico de debug si existe
    tasks.findByName("connectedDebugAndroidTest")?.let {
        it.finalizedBy("copyScreenshotsToReports")
    }
}
