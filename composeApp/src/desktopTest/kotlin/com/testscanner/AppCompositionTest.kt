package com.testscanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.testscanner.di.appModules
import com.testscanner.navigation.Destination
import com.testscanner.navigation.Navigator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Comprueba el **montaje**: que `App()` compone de verdad, con el grafo real y hasta la pantalla.
 *
 * ## Por qué este test es el que faltaba
 *
 * El criterio de salida de la Fase 1 decía "la app arranca". Nadie lo comprobaba, y el ROADMAP lo
 * dice con todas las letras: *todo lo que este proyecto comprueba son piezas, y nada comprueba el
 * montaje*. Esa frase se pagó dos veces —el `Executor` mal declarado que mataba la app al componer
 * la primera pantalla (D18) y el driver de Room que nunca se aplicaba (SDD §11)—, y las dos veces el
 * fallo estaba **entre** piezas que individualmente estaban bien.
 *
 * `KoinGraphTest` cubrió la mitad de ese hueco: que el grafo resuelva. Pero resolver no es componer.
 * Entre `koin.get<X>()` y una pantalla en marcha están los `CompositionLocal`, los `koinViewModel`,
 * el tema, el idioma, los efectos de arranque de cada pantalla y la navegación — y nada de eso lo
 * ejercita pedirle un tipo al grafo.
 *
 * Esto lo ejercita. Compone `App()` entera, la deja asentarse y cambia de destino.
 *
 * ## Qué comprueba y qué no
 *
 * Comprueba que **componer no revienta**: si un `CompositionLocal` falta, si un `koinViewModel` no
 * encuentra su ViewModel, si un efecto de arranque lanza, el test falla aquí. No comprueba que se
 * **vea** bien: `runComposeUiTest` compone y mide, no juzga. Los píxeles siguen necesitando ojos, y
 * eso sigue en la lista de lo que hay que mirar en un dispositivo.
 *
 * Escritorio es la única de las cuatro plataformas donde esto cabe en un test JVM normal. Vale para
 * las cuatro en todo lo que es común —que es casi todo `App()`— y no vale para lo que aporta cada
 * plataforma; el `platformModule` de Android tiene su propia red en `AndroidKoinGraphTest`.
 *
 * ## Los dos `CompositionLocal` que se proveen a mano
 *
 * En la app los pone la plataforma: Android los trae de la `ComponentActivity` y escritorio de la
 * ventana. Aquí no hay ni una cosa ni la otra, así que se proveen explícitamente en vez de confiar
 * en lo que el arnés de test decida montar por su cuenta. El ciclo de vida arranca en `RESUMED`
 * porque `collectAsStateWithLifecycle` solo colecta a partir de `STARTED`: con menos que eso el test
 * pasaría sin haber leído nunca las preferencias, que es la primera dependencia que `App()` pide.
 */
@OptIn(ExperimentalTestApi::class)
class AppCompositionTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `la raiz de la app compone con el grafo real`() = runComposeUiTest {
        startKoin { modules(appModules()) }
        val navigator = Navigator()

        setContent {
            WithPlatformOwners { App(navigator = navigator) }
        }
        waitForIdle()

        assertEquals(
            listOf(Destination.Scanner),
            navigator.backstack.value,
            "la app no compuso sobre el destino inicial",
        )
    }

    /**
     * Cambiar de destino es donde vive el cableado nuevo de esta revisión: `AnimatedContent` compone
     * las dos pantallas a la vez durante la salida, así que un fallo al montar la entrante sale aquí
     * y no al componer la primera.
     *
     * Se navega llamando al `Navigator` y no pulsando la barra inferior a propósito: las etiquetas
     * salen de `composeResources`, que se cargan de forma asíncrona, y un test que dependa de eso
     * mide la carga de recursos en vez de lo que dice medir.
     *
     * Ajustes y no Historial: el historial consulta Room, y abrir la base de datos es un riesgo de
     * entorno que no tiene nada que ver con lo que aquí se comprueba. Que Room esté bien cableado ya
     * lo dicen `KoinGraphTest` y `AndroidKoinGraphTest`.
     */
    @Test
    fun `cambiar de destino compone la pantalla nueva`() = runComposeUiTest {
        startKoin { modules(appModules()) }
        val navigator = Navigator()

        setContent {
            WithPlatformOwners { App(navigator = navigator) }
        }
        waitForIdle()

        navigator.navigateTo(Destination.Settings)
        waitForIdle()

        assertEquals(Destination.Settings, navigator.backstack.value.last())
    }
}

@Composable
private fun WithPlatformOwners(content: @Composable () -> Unit) {
    val lifecycleOwner = remember { TestLifecycleOwner() }
    val viewModelStoreOwner = remember { TestViewModelStoreOwner() }

    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleOwner,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        content = content,
    )
}

/**
 * `createUnsafe` y no el constructor normal: el constructor de `LifecycleRegistry` exige estar en el
 * hilo principal, y el hilo desde el que compone el arnés de test no lo es. Saltarse esa
 * comprobación es exactamente lo que hace falta aquí y no tiene más consecuencias: este registro no
 * lo comparte nadie.
 */
private class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this).apply {
        currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle get() = registry
}

private class TestViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
