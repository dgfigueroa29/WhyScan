package com.whyscan.feature.scanner

import com.whyscan.core.domain.usecase.DecodeImageUseCase
import com.whyscan.core.domain.usecase.SaveDetectionUseCase
import com.whyscan.core.domain.usecase.ScanHistory
import com.whyscan.core.domain.usecase.ScanSessions
import com.whyscan.core.domain.usecase.ScanSettings
import com.whyscan.core.domain.usecase.SelectScannerEngineUseCase
import com.whyscan.core.domain.usecase.StartScanSessionUseCase
import com.whyscan.core.model.ScannerEngineId
import com.whyscan.core.platform.NoOpPlatformActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Segundo plano y primer plano, que **no son** llegar a la pantalla y salir de ella.
 *
 * ## El defecto que estas pruebas cierran
 *
 * Los dos pares se fundieron en uno —`LifecycleStartEffect` mandando `ScreenShown`/`ScreenHidden`—
 * y eso encerró al usuario dentro de la cámara. El Google Code Scanner abre **su propia pantalla,
 * en otro proceso**, así que arrancar la sesión manda WhyScan al fondo; al cerrar esa pantalla,
 * WhyScan volvía al primer plano, eso contaba como "llegar a la pantalla", la sesión arrancaba y el
 * motor abría su pantalla otra vez. Ni la X, ni atrás, ni el gesto: no había salida, y daba igual
 * que la lectura hubiera funcionado.
 *
 * El arreglo es que sean dos preguntas distintas, y esto es lo que lo sujeta.
 *
 * ## Por qué el motor falso tiene que quedarse abierto
 *
 * Porque la pregunta es "¿estaba corriendo la sesión cuando la app se fue al fondo?", y un motor
 * que termina en cuanto emite deja esa pregunta sin sujeto: cualquier aserción sobre parar y
 * reanudar saldría verde sin comprobar nada. De ahí `keepsScanning`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerLifecycleTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `volver al primer plano no arranca una sesion por su cuenta`() = runTest {
        // El invariante que se violó. Volver al primer plano **no** es llegar a la pantalla: si
        // arrancara sesión, cerrar la pantalla del Google Code Scanner la reabriría, y así siempre.
        val engine = FakeEngine(ScannerEngineId.GmsCodeScanner)
        val viewModel = viewModel(engine)
        viewModel.onAction(ScannerAction.ScreenShown)
        assertEquals(1, engine.scanInvocations, "la sesión debería haber arrancado al llegar")

        // La pantalla del motor tapa la app, el usuario la cierra y la app vuelve.
        viewModel.onAction(ScannerAction.Backgrounded)
        viewModel.onAction(ScannerAction.Foregrounded)

        assertEquals(1, engine.scanInvocations, "volver al primer plano reabrió la cámara")
    }

    @Test
    fun `el motor que abre su propia pantalla no se para al irse la app al fondo`() = runTest {
        // Aquí "estamos en segundo plano" no significa que el usuario se haya ido: significa que el
        // motor está trabajando, y lo mandamos nosotros. Pararlo cancelaría el escaneo que el
        // usuario está haciendo en ese momento, y su resultado moriría en una corrutina cancelada.
        val viewModel = viewModel(FakeEngine(ScannerEngineId.GmsCodeScanner, keepsScanning = true))
        viewModel.onAction(ScannerAction.ScreenShown)
        assertEquals(SessionStatus.Scanning, viewModel.state.value.sessionStatus)

        viewModel.onAction(ScannerAction.Backgrounded)

        assertEquals(SessionStatus.Scanning, viewModel.state.value.sessionStatus)
        viewModel.onAction(ScannerAction.ScreenHidden)
    }

    @Test
    fun `un motor normal si apaga la camara al irse al fondo, y la devuelve al volver`() = runTest {
        // Lo que la Ronda 12 pedía: la cámara no sigue capturando con la app minimizada. Y volver la
        // devuelve, porque el usuario no la apagó — la apagamos nosotros.
        val engine = FakeEngine(ScannerEngineId.MlKitCameraX, keepsScanning = true)
        val viewModel = viewModel(engine)
        viewModel.onAction(ScannerAction.ScreenShown)

        viewModel.onAction(ScannerAction.Backgrounded)
        assertEquals(SessionStatus.Idle, viewModel.state.value.sessionStatus)

        viewModel.onAction(ScannerAction.Foregrounded)

        assertEquals(SessionStatus.Scanning, viewModel.state.value.sessionStatus)
        assertEquals(2, engine.scanInvocations)
        viewModel.onAction(ScannerAction.ScreenHidden)
    }

    @Test
    fun `una camara que el usuario paro a mano no vuelve sola`() = runTest {
        // Reanudar algo que el usuario apagó es contestarle que no. La marca solo se pone si fuimos
        // nosotros los que quitamos la cámara.
        val engine = FakeEngine(ScannerEngineId.MlKitCameraX, keepsScanning = true)
        val viewModel = viewModel(engine)
        viewModel.onAction(ScannerAction.ScreenShown)
        viewModel.onAction(ScannerAction.StopSession)

        viewModel.onAction(ScannerAction.Backgrounded)
        viewModel.onAction(ScannerAction.Foregrounded)

        assertEquals(SessionStatus.Idle, viewModel.state.value.sessionStatus)
        assertEquals(1, engine.scanInvocations)
    }

    @Test
    fun `salir de la pantalla no deja nada apuntado para reanudar`() = runTest {
        // Al desmontarse la composición se disparan los dos efectos, y `Backgrounded` va primero.
        // Si dejara la marca puesta, volver a la pantalla arrancaría dos sesiones: la del arranque
        // automático y la de la reanudación.
        val engine = FakeEngine(ScannerEngineId.MlKitCameraX, keepsScanning = true)
        val viewModel = viewModel(engine)
        viewModel.onAction(ScannerAction.ScreenShown)

        // El orden real al navegar fuera de la pantalla.
        viewModel.onAction(ScannerAction.Backgrounded)
        viewModel.onAction(ScannerAction.ScreenHidden)

        viewModel.onAction(ScannerAction.Foregrounded)

        assertEquals(SessionStatus.Idle, viewModel.state.value.sessionStatus)
        assertEquals(1, engine.scanInvocations)
    }

    private fun viewModel(engine: FakeEngine): ScannerViewModel {
        val engines = FakeEngineRepository(engines = listOf(engine))
        val history = FakeHistoryRepository()
        val select = SelectScannerEngineUseCase(engines)

        return ScannerViewModel(
            settings = ScanSettings(FakePreferencesRepository()),
            sessions = ScanSessions(
                startSession = StartScanSessionUseCase(engines, select),
                decodeImage = DecodeImageUseCase(engines, select),
                saveDetection = SaveDetectionUseCase(history),
            ),
            history = ScanHistory(history),
            engineRepository = engines,
            permissionController = FakePermissionController(),
            imagePicker = FakeImagePicker(),
            resultActions = ResultActionRunner(NoOpPlatformActions()),
        )
    }
}
