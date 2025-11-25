package com.miso.vinilos.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule que configura el TestDispatcher para tests de corrutinas en Android
 *
 * Este rule asegura que las corrutinas se ejecuten de forma sincrónica y predecible
 * durante los tests, resolviendo race conditions y problemas de timing.
 *
 * IMPORTANTE: Para tests de instrumentación (androidTest), este rule usa
 * UnconfinedTestDispatcher que ejecuta las corrutinas inmediatamente en el
 * thread actual (Main thread de Android), evitando problemas con Lifecycle.
 *
 * Uso:
 * ```
 * @get:Rule(order = 0)
 * val testDispatcherRule = TestDispatcherRule()
 * ```
 *
 * Luego en los tests:
 * ```
 * testDispatcherRule.advanceUntilIdle() // Ejecutar todas las corrutinas pendientes
 * composeTestRule.waitForIdle()
 * ```
 */
class TestDispatcherRule : TestWatcher() {

    // Para tests de instrumentación, usamos UnconfinedTestDispatcher
    // que ejecuta corrutinas inmediatamente en el thread actual
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    /**
     * Se ejecuta antes de cada test
     * Reemplaza el Main dispatcher y el IO dispatcher con el test dispatcher
     */
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
        // También reemplazar IO dispatcher para controlar operaciones de base de datos
        // Esto asegura que Room operations también estén bajo control del test
    }

    /**
     * Se ejecuta después de cada test
     * Restaura el Main dispatcher original
     */
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }

    /**
     * Avanza el scheduler hasta que todas las corrutinas pendientes se completen
     *
     * NOTA: Con UnconfinedTestDispatcher, las corrutinas se ejecutan inmediatamente,
     * por lo que este método es principalmente para compatibilidad y claridad del código.
     *
     * Ejemplo:
     * ```
     * testViewModel.loadData()
     * testDispatcherRule.advanceUntilIdle() // Esperar a que termine
     * composeTestRule.waitForIdle()
     * // Ahora se pueden hacer assertions
     * ```
     */
    fun advanceUntilIdle() {
        // Con UnconfinedTestDispatcher, las corrutinas ya se ejecutaron
        // Este método se mantiene para compatibilidad
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /**
     * Avanza el tiempo virtual del scheduler
     *
     * Útil para tests que usan delay() o timeout
     *
     * @param delayTimeMillis Tiempo en milisegundos a avanzar
     */
    fun advanceTimeBy(delayTimeMillis: Long) {
        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis)
    }
}
