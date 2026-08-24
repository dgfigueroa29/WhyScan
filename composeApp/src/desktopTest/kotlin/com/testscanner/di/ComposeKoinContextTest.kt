package com.testscanner.di

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import com.testscanner.core.domain.repository.AppPreferencesRepository
import com.testscanner.core.domain.repository.ScannerEngineRepository
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Cierra la deuda **D20**: el envoltorio `KoinContext { }` de `App.kt` sobraba, y lo que impedía
 * quitarlo era no tener forma de comprobarlo sin instalar la app.
 *
 * ## Qué estaba en duda
 *
 * Koin avisaba en cada build de que `KoinContext` está deprecado —"Compose Koin context is setup
 * with StartKoin()"—, pero quitarlo cambia **por dónde** resuelven `koinInject` y `koinViewModel`:
 * con envoltorio leen un `CompositionLocal` provisto explícitamente; sin él caen en el valor por
 * defecto de ese mismo `CompositionLocal`, que koin-compose declara como
 * `KoinPlatform.getKoin().scopeRegistry.rootScope`. Sobre el papel son el mismo scope. Sobre el
 * papel también estaba bien el `build()` de Room que nunca se llamaba (SDD §11), así que aquí no se
 * da nada por bueno leyendo la librería: se compone y se mira qué sale.
 *
 * ## Por qué esto no necesita un dispositivo ni una ventana
 *
 * `koinInject` no es UI: es una función `@Composable` que lee un `CompositionLocal` y llama a
 * `remember`. Todo eso lo resuelve el **runtime** de Compose, que es Kotlin puro y no sabe nada de
 * pantallas. Se monta una `Composition` con un `Applier` que no aplica nada —no hay árbol de nodos
 * que construir, solo interesa el efecto de componer— y se captura lo que devuelve.
 *
 * Es deliberadamente más pequeño que un test de UI: no arranca Skiko, no abre ventana y corre en el
 * mismo `desktopTest` que ya existe. Lo que comprueba es exactamente lo que estaba en duda, y nada
 * más: que sin envoltorio se resuelve, y que se resuelve **contra el mismo grafo**.
 *
 * Nota sobre el orden: el valor por defecto del `CompositionLocal` se calcula la primera vez que
 * alguien lo consume, así que `startKoin()` tiene que haber ocurrido antes de componer. En la app
 * eso lo garantiza `initKoin()` desde el punto de entrada de cada plataforma, antes de que exista
 * ningún composable; aquí lo garantiza el orden de este test.
 */
class ComposeKoinContextTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `koinInject resuelve sin envolver la app en KoinContext`() {
        val koin = startKoin { modules(appModules()) }.koin
        val expected = koin.get<AppPreferencesRepository>()

        val resolved = composeAndCapture { koinInject<AppPreferencesRepository>() }

        assertSame(
            expected,
            resolved,
            "koinInject devolvió otra instancia: no resolvió contra el grafo que arrancó la app",
        )
    }

    /**
     * El segundo tipo no es redundante y no es cualquiera: `ScannerEngineRepository` es un `single`
     * que **depende del `platformModule`** —la lista de motores instalados—, así que comprobar su
     * identidad dice que la composición resuelve contra el grafo completo y no contra un scope
     * paralelo que hubiera vuelto a construir lo que puede construir por su cuenta.
     */
    @Test
    fun `koinInject ve el grafo entero y no solo los modulos comunes`() {
        val koin = startKoin { modules(appModules()) }.koin
        val expected = koin.get<ScannerEngineRepository>()

        val resolved = composeAndCapture { koinInject<ScannerEngineRepository>() }

        assertSame(expected, resolved)
    }

    /**
     * Compone [content] una vez y devuelve lo que produjo.
     *
     * `Recomposer` se crea pero no se pone a recomponer: la composición inicial que dispara
     * `setContent` es síncrona, y aquí no hay estado que cambie ni efectos que lanzar.
     */
    private fun <T : Any> composeAndCapture(content: @Composable () -> T): T {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(NoOpApplier, recomposer)
        var captured: T? = null

        try {
            composition.setContent { captured = content() }
        } finally {
            composition.dispose()
            recomposer.cancel()
        }

        return requireNotNull(captured) { "la composición no llegó a ejecutar el contenido" }
    }
}

/**
 * Un `Applier` que no aplica nada.
 *
 * `Composition` necesita uno porque su trabajo normal es materializar un árbol de nodos —vistas,
 * `LayoutNode`s— a partir de la composición. Aquí no hay árbol: lo que se comprueba ocurre
 * *durante* la composición, no en lo que sale de ella.
 */
private object NoOpApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) = Unit
    override fun up() = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun clear() = Unit
}
