package com.whyscan.di

import com.whyscan.core.domain.repository.AppPreferencesRepository
import com.whyscan.core.domain.repository.ScanHistoryRepository
import com.whyscan.core.domain.repository.ScanPreferencesRepository
import com.whyscan.core.domain.repository.ScannerEngineRepository
import com.whyscan.core.domain.usecase.ClearScanHistoryUseCase
import com.whyscan.core.domain.usecase.ObserveScanHistoryUseCase
import com.whyscan.core.domain.usecase.ScanSessions
import com.whyscan.core.domain.usecase.ScanSettings
import com.whyscan.core.domain.usecase.StartComparisonUseCase
import com.whyscan.core.model.ScannerPlatform
import com.whyscan.core.permissions.PermissionController
import com.whyscan.core.platform.FileSaver
import com.whyscan.core.platform.ImagePicker
import com.whyscan.core.platform.PlatformActions
import com.whyscan.core.scanner.BarcodeScannerEngine
import com.whyscan.core.scanner.TimeProvider
import com.whyscan.feature.scanner.EnginePreviewResolver
import com.whyscan.feature.scanner.ResultActionRunner
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cierra la mitad que le faltaba a **D18**: el `platformModule` de **Android**.
 *
 * ## Por qué justo este
 *
 * El defecto que abrió D18 estaba aquí y en ningún otro sitio. `platformModule` registraba el
 * executor de análisis como `ExecutorService` mientras los tres motores de cámara lo piden como
 * `Executor`; Koin resuelve por igualdad exacta de tipo y no recorre supertipos, así que la app
 * moría al componer la primera pantalla. `KoinGraphTest` —el de `desktopTest`— cubrió los módulos
 * comunes y el `platformModule` de escritorio, y dejó dicho que el de Android seguía sin red porque
 * necesitaba un `androidUnitTest` en este módulo. Esto es ese `androidUnitTest`.
 *
 * ## Por qué Robolectric y no un emulador
 *
 * Este proyecto decidió que no habría tests instrumentados: sin emulador en CI, un test que exija
 * dispositivo es un test que nadie ejecuta (deuda D6). Robolectric no lo contradice, lo esquiva: el
 * grafo de Android no necesita un dispositivo, necesita un `Context`. Todas las definiciones de
 * abajo se construyen con ese `Context` y nada más — los motores de cámara guardan la referencia y
 * dejan el `LifecycleCameraController` y los clientes de ML Kit detrás de un `by lazy`, así que
 * resolverlos no arranca ninguna cámara.
 *
 * `sdk = 34` a propósito, y no el `targetSdk` 36 del proyecto: Robolectric exige JDK 21 para simular
 * la 36 y el CI corre sobre 17. Lo que se comprueba aquí es cableado de Koin, que no cambia entre
 * niveles de API; fijar el número evita que la elección la haga el entorno por su cuenta.
 *
 * ## Qué sigue sin cubrir
 *
 * Lo mismo que en escritorio: no construye los ViewModels —eso arrancaría corrutinas en
 * `viewModelScope`— sino todo lo que piden por constructor, que es donde falló D18. Y no comprueba
 * que un motor **lea** un código: para eso hace falta una cámara, y eso no lo arregla ningún test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class AndroidKoinGraphTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `el grafo de Android arranca con todos los modulos de la app`() {
        val koin = start()

        assertEquals(ScannerPlatform.Android, koin.get<ScannerPlatform>())
        assertEquals(
            EXPECTED_ENGINES,
            koin.get<List<BarcodeScannerEngine>>().size,
            "la lista de motores de Android cambió sin que nadie lo dijera",
        )
    }

    /**
     * El test que existe por el defecto original, escrito para que se lea como lo que fue.
     *
     * `Executor` y no `ExecutorService`: si alguien vuelve a declarar la fábrica por su tipo
     * concreto, esto falla aquí en lugar de en el primer arranque del primer teléfono. Se pide con
     * `get` y no con una aserción propia porque el mensaje que interesa leer es el de Koin, palabra
     * por palabra el que apareció en el logcat aquel día: `No definition found for type
     * 'java.util.concurrent.Executor'`.
     */
    @Test
    fun `el executor de analisis se resuelve por el tipo que piden los motores`() {
        val koin = start()

        koin.get<Executor>()
    }

    @Test
    fun `resuelve todo lo que ScannerViewModel pide por constructor`() {
        val koin = start()

        koin.get<ScanSettings>()
        koin.get<ScanSessions>()
        koin.get<ScannerEngineRepository>()
        koin.get<PermissionController>()
        koin.get<ImagePicker>()
        koin.get<ResultActionRunner>()

        // La pantalla lo pide aparte del ViewModel, con `koinInject`.
        koin.get<EnginePreviewResolver>()
    }

    @Test
    fun `resuelve todo lo que HistoryViewModel pide por constructor`() {
        val koin = start()

        koin.get<ObserveScanHistoryUseCase>()
        koin.get<ClearScanHistoryUseCase>()
        koin.get<PlatformActions>()
        koin.get<FileSaver>()
    }

    @Test
    fun `resuelve todo lo que ComparisonViewModel pide por constructor`() {
        val koin = start()

        koin.get<StartComparisonUseCase>()
        koin.get<ScanPreferencesRepository>()
    }

    @Test
    fun `resuelve todo lo que SettingsViewModel pide por constructor`() {
        val koin = start()

        koin.get<AppPreferencesRepository>()
    }

    @Test
    fun `resuelve lo que la app necesita para arrancar`() {
        val koin = start()

        koin.get<TimeProvider>()
    }

    /**
     * Aparte de los demás por el mismo motivo que en escritorio: es el único que construye algo
     * pesado. Room no abre el archivo aquí —lo hace en la primera consulta, y no se hace ninguna—,
     * pero sí ejecuta la configuración del driver, que es justo la línea que llevaba meses sin
     * ejecutarse en las otras dos plataformas (SDD §11).
     */
    @Test
    fun `resuelve el historial persistente`() {
        val koin = start()

        koin.get<ScanHistoryRepository>()
    }

    private fun start(): Koin = startKoin {
        androidContext(RuntimeEnvironment.getApplication())
        modules(appModules())
    }.koin

    private companion object {
        /** Los cuatro motores de Android más el de entrada manual. */
        const val EXPECTED_ENGINES = 5
    }
}

/**
 * Nivel de API que simula Robolectric.
 *
 * Fuera de la clase porque `@Config` necesita una constante de compilación y las de un
 * `companion object` no lo son a efectos de anotación.
 */
private const val ROBOLECTRIC_SDK = 34
