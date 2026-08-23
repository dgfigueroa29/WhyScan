package com.whyscan.core.domain.concurrency

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La red que impide que un fallo de disco cierre la app.
 *
 * Los dos primeros tests son el contrato obvio. El tercero es el que de verdad importa: tragarse una
 * `CancellationException` es el error clásico de este patrón, no rompe ningún test evidente y
 * convierte cada salida de pantalla en un "algo falló" delante del usuario.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LaunchCatchingTest {

    @Test
    fun `un fallo se reporta en vez de propagarse`() = runTest {
        var reported: Throwable? = null

        launchCatching(onFailure = { reported = it }) {
            error("la base no abre")
        }.join()

        assertEquals("la base no abre", reported?.message)
    }

    @Test
    fun `sin fallo no se reporta nada`() = runTest {
        var reported: Throwable? = null
        var ran = false

        launchCatching(onFailure = { reported = it }) { ran = true }.join()

        assertTrue(ran)
        assertNull(reported)
    }

    @Test
    fun `cancelar no es un fallo`() = runTest {
        // El caso que un `runCatching` haría mal. Si la cancelación se tratara como fallo, salir de
        // una pantalla —que cancela su `viewModelScope`— le enseñaría al usuario un error por haber
        // navegado. Y peor: el ámbito creería que el trabajo terminó bien.
        var reported: Throwable? = null
        val started = CompletableDeferred<Unit>()

        val job = launchCatching(onFailure = { reported = it }) {
            started.complete(Unit)
            CompletableDeferred<Unit>().await() // no termina nunca: solo se puede cancelar
        }

        started.await()
        job.cancel()
        job.join()

        assertNull(reported, "la cancelación se reportó como si fuera un fallo")
        assertTrue(job.isCancelled)
    }

    @Test
    fun `un Error no se captura`() = runTest {
        // `OutOfMemoryError` y compañía dicen que el proceso ya no está en condiciones de seguir.
        // Fingir que se puede continuar cambia un cierre honesto por un comportamiento imprevisible,
        // así que solo se capturan las `Exception`.
        //
        // Corre en un ámbito propio y no en el de `runTest`: el `Error` tiene que quedar sin
        // capturar para que este test signifique algo, y sin capturar en el ámbito del test lo haría
        // fallar. El manejador es la forma de verlo salir sin que se lleve la prueba por delante.
        var reported: Throwable? = null
        val escaped = mutableListOf<Throwable>()
        val scope = CoroutineScope(Job() + CoroutineExceptionHandler { _, failure -> escaped += failure })

        scope.launchCatching(onFailure = { reported = it }) {
            throw StackOverflowError("desbordamiento")
        }.join()

        assertNull(reported, "se capturó un Error, que no se debe capturar")
        assertTrue(escaped.single() is StackOverflowError)
    }
}
