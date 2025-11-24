package com.miso.vinilos.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule que configura el TestDispatcher para tests de coroutinas en Android
 *
 * Este rule asegura que las corrutinas se ejecuten de forma sincrónica y predecible
 * durante los tests, resolviendo race conditions y problemas de timing.
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
 *
 * @param testDispatcher El dispatcher de test a usar (por defecto StandardTestDispatcher)
 */
class TestDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    /**
     * Se ejecuta antes de cada test
     * Reemplaza el Main dispatcher con el test dispatcher
     */
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
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
     * IMPORTANTE: Llamar esto antes de hacer assertions en tests que usan corrutinas
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

    /**
     * Ejecuta una tarea hasta que se complete
     * Combina runTest con advanceUntilIdle
     */
    fun runTest(block: suspend TestScope.() -> Unit) = kotlinx.coroutines.test.runTest(testDispatcher) {
        block()
        advanceUntilIdle()
    }
}
