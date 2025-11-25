package com.miso.vinilos.helpers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.*
import kotlinx.coroutines.CompletableDeferred

/**
 * FakeImageLoader para tests que retorna imágenes inmediatamente de forma sincrónica
 * sin hacer network calls, evitando que Compose se quede "ocupado"
 * esperando a que las imágenes carguen.
 *
 * Basado en solución de Stack Overflow:
 * https://stackoverflow.com/questions/72903412/jetpack-compose-ui-test-coil-fetched-image
 */
class FakeImageLoader(private val context: Context) : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? get() = null
    override val diskCache: DiskCache? get() = null

    override fun enqueue(request: ImageRequest): Disposable {
        // Llamar onStart inmediatamente
        request.target?.onStart(request.placeholder)

        // Crear un drawable fake (gris)
        val result = ColorDrawable(Color.LTGRAY)

        // Llamar onSuccess inmediatamente (sincrónico)
        request.target?.onSuccess(result)

        return object : Disposable {
            override val job = CompletableDeferred(
                newResult(request, result)
            )
            override val isDisposed get() = true
            override fun dispose() {}
        }
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        // Retornar resultado inmediatamente (sincrónico)
        return newResult(request, ColorDrawable(Color.LTGRAY))
    }

    private fun newResult(
        request: ImageRequest,
        drawable: Drawable
    ): SuccessResult {
        return SuccessResult(
            drawable = drawable,
            request = request,
            dataSource = DataSource.MEMORY
        )
    }

    override fun newBuilder() =
        throw UnsupportedOperationException()

    override fun shutdown() {}
}
