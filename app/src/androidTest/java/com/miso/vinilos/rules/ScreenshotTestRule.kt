package com.miso.vinilos.rules

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regla de prueba que permite capturar screenshots en puntos específicos durante la ejecución.
 * 
 * IMPORTANTE: Las screenshots deben tomarse durante el test, NO automáticamente al final
 * porque la Activity puede estar siendo destruida cuando finished() se ejecuta.
 * 
 * Las screenshots se guardan en: /sdcard/Pictures/screenshots/ o /data/data/[package]/files/screenshots/
 */
class ScreenshotTestRule : TestWatcher() {
    
    private var currentTestDescription: Description? = null
    private var composeTestRule: AndroidComposeTestRule<*, *>? = null
    
    /**
     * Configura la regla con la regla de Compose para acceso a la actividad
     */
    fun setComposeTestRule(rule: AndroidComposeTestRule<*, *>) {
        this.composeTestRule = rule
    }
    
    override fun starting(description: Description) {
        super.starting(description)
        currentTestDescription = description
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        // NO capturamos screenshot aquí porque la Activity puede estar siendo destruida
        // Las screenshots deben tomarse explícitamente durante el test
        currentTestDescription = null
    }

    /**
     * Toma un screenshot con el nombre del test
     */
    private fun takeScreenshot(description: Description, suffix: String = "") {
        val baseFilename = "${description.testClass.simpleName}-${description.methodName}"
        val filename = if (suffix.isNotEmpty()) "$baseFilename-$suffix" else baseFilename
        captureScreenshot(filename)
    }
    
    /**
     * Captura un screenshot de la actividad actual de forma segura
     */
    private fun captureScreenshot(filename: String) {
        try {
            // Primero esperar a que Compose esté idle para asegurar que todo esté renderizado
            composeTestRule?.waitForIdle()
            
            val activity = getActivity() ?: run {
                android.util.Log.w("ScreenshotTestRule", "No activity available for screenshot")
                return
            }
            
            // Verificar que la actividad no esté siendo destruida
            if (activity.isFinishing || activity.isDestroyed) {
                android.util.Log.w("ScreenshotTestRule", "Activity is finishing or destroyed, skipping screenshot")
                return
            }
            
            // Usar CountDownLatch para sincronizar la captura
            val latch = CountDownLatch(1)
            var bitmap: Bitmap? = null
            
            // Ejecutar en el hilo UI de forma síncrona
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                try {
                    val view = activity.window?.decorView?.rootView ?: run {
                        android.util.Log.w("ScreenshotTestRule", "No root view available for screenshot")
                        latch.countDown()
                        return@runOnMainSync
                    }
                    
                    // Asegurar que la vista tenga dimensiones válidas
                    val width = view.width.coerceAtLeast(1)
                    val height = view.height.coerceAtLeast(1)
                    
                    if (width <= 0 || height <= 0) {
                        android.util.Log.w("ScreenshotTestRule", "Invalid view dimensions: ${width}x${height}")
                        latch.countDown()
                        return@runOnMainSync
                    }
                    
                    // Crear bitmap y dibujar la vista
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap!!)
                    view.draw(canvas)
                    
                } catch (e: Exception) {
                    android.util.Log.e("ScreenshotTestRule", "Error capturing screenshot", e)
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
            
            // Esperar a que la captura termine (con timeout de 5 segundos)
            if (latch.await(5, TimeUnit.SECONDS)) {
                bitmap?.let { 
                    // Guardar el bitmap en un hilo separado
                    Thread {
                        saveBitmap(it, filename)
                    }.start()
                }
            } else {
                android.util.Log.w("ScreenshotTestRule", "Screenshot capture timeout")
            }
        } catch (ex: Exception) {
            // Captura cualquier excepción para evitar que falle el test
            android.util.Log.e("ScreenshotTestRule", "Error in captureScreenshot", ex)
            ex.printStackTrace()
        }
    }
    
    /**
     * Obtiene la actividad actual desde la regla de Compose o del test runner
     */
    private fun getActivity(): ComponentActivity? {
        return composeTestRule?.activity as? ComponentActivity
            ?: try {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.targetContext as? ComponentActivity
                    ?: instrumentation.context as? ComponentActivity
            } catch (e: Exception) {
                null
            }
    }
    
    /**
     * Guarda el bitmap en el almacenamiento del dispositivo
     */
    private fun saveBitmap(bitmap: Bitmap, filename: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotsDir = getScreenshotsDirectory()
        
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        
        val file = File(screenshotsDir, "$filename.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            android.util.Log.d("ScreenshotTestRule", "Screenshot saved: ${file.absolutePath}")
        } catch (e: IOException) {
            android.util.Log.e("ScreenshotTestRule", "Error saving screenshot", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Obtiene el directorio donde guardar las screenshots
     */
    private fun getScreenshotsDirectory(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Intentar usar el almacenamiento externo primero
        val externalStorage = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_PICTURES
        )
        val externalDir = File(externalStorage, "screenshots")
        
        // Si el almacenamiento externo no está disponible, usar el interno
        return if (externalDir.canWrite() || externalDir.mkdirs()) {
            externalDir
        } else {
            // Fallback al directorio de archivos de la aplicación
            File(context.filesDir, "screenshots").apply { mkdirs() }
        }
    }
    
    /**
     * Obtiene la ruta del directorio donde se guardan las screenshots
     */
    fun getScreenshotsDirectoryPath(): String {
        return getScreenshotsDirectory().absolutePath
    }
    
    /**
     * Método público para tomar screenshots durante la ejecución del test
     * Ejemplo de uso: screenshotTestRule.takeScreenshot("step-1-inicio")
     * 
     * Nota: Asegúrate de llamar setComposeTestRule() si usas esta regla independientemente
     */
    fun takeScreenshot(suffix: String) {
        currentTestDescription?.let { description ->
            takeScreenshot(description, suffix)
        }
    }
    
    /**
     * Método público para tomar screenshots con un nombre específico
     * Ejemplo de uso: screenshotTestRule.takeScreenshotNamed("verificacion-datos")
     */
    fun takeScreenshotNamed(name: String) {
        currentTestDescription?.let { description ->
            val baseFilename = "${description.testClass.simpleName}-${description.methodName}"
            captureScreenshot("$baseFilename-$name")
        }
    }
}