package com.whyscan.core.domain.concurrency

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lanza una corrutina que **no mata la app** si lo que hay dentro falla.
 *
 * ## El problema que resuelve
 *
 * `viewModelScope` es `SupervisorJob() + Dispatchers.Main.immediate`. El supervisor evita que un
 * hijo que falla cancele a sus hermanos, y eso es todo lo que hace: la excepción **sigue subiendo**
 * hasta el manejador por defecto del hilo, que en Android termina el proceso.
 *
 * Este proyecto tenía treinta `viewModelScope.launch` y ni una sola captura. Cualquier excepción de
 * Room —disco lleno, `SQLITE_FULL`, base corrupta— mataba la app. Y como Room abre el archivo de
 * forma perezosa, en la primera consulta y no al construir la base, esa primera consulta ocurre
 * siempre dentro de una corrutina: **una base corrupta era un cierre en el arranque** que el usuario
 * no podía deshacer sin borrar los datos de la app. Es el peor modo de fallo que existe.
 *
 * ## Por qué esto y no un `CoroutineExceptionHandler`
 *
 * Un manejador en el `viewModelScope` capturaría **todo** de una vez, incluidos los errores de
 * programación, y los convertiría en un mensajito. Eso no es robustez: es esconder los defectos
 * donde nadie los va a encontrar, y con el CI de este proyecto —que no ejecuta la app— serían
 * invisibles para siempre.
 *
 * Aquí cada sitio se acoge a mano. Lo que se envuelve es lo que **puede fallar por causas ajenas al
 * código**: escribir en disco, leer la base, hablar con el sistema. Lo demás sigue reventando, que
 * es lo que debe hacer un defecto.
 *
 * ## Las dos líneas que más se equivocan
 *
 * **`CancellationException` se relanza.** Un `runCatching` normal la traga, y eso rompe la
 * concurrencia estructurada: el ámbito cree que el trabajo terminó bien cuando en realidad lo
 * cancelaron. En una pantalla es peor todavía — cada vez que el usuario sale, la cancelación se
 * reportaría como si algo hubiera fallado, y el usuario vería un error por navegar.
 *
 * **Se captura `Exception` y no `Throwable`.** Un `Error` —`OutOfMemoryError`, `StackOverflowError`—
 * dice que el proceso ya no está en condiciones de seguir, y fingir que se puede continuar solo
 * cambia un cierre honesto por un comportamiento imprevisible.
 *
 * @param onFailure qué contarle al usuario. Es `suspend` porque el sitio natural para contarlo es
 *   emitir un efecto, y eso suspende.
 */
@Suppress("TooGenericExceptionCaught")
fun CoroutineScope.launchCatching(
    onFailure: suspend (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (cancellation: CancellationException) {
        // Relanzar y no reportar: la cancelación es cómo funciona el ámbito, no un fallo.
        throw cancellation
    } catch (failure: Exception) {
        onFailure(failure)
    }
}
