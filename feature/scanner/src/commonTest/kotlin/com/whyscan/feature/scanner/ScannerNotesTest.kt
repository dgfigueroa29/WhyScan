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
import com.whyscan.core.scanner.ScanEvent
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Anotar desde la pantalla de escaneo.
 *
 * La nota se escribe aquí pero **vive en el historial**, y eso es justo lo que estos tests fijan: el
 * escáner no guarda notas en su propio estado, las lee de donde están. Sin eso, releer un código ya
 * anotado abriría el campo vacío y guardarlo se llevaría por delante lo que hubiera.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerNotesTest {

    private lateinit var history: FakeHistoryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(vararg fakeEngines: FakeEngine): ScannerViewModel {
        val engines = FakeEngineRepository(engines = fakeEngines.toList())
        history = FakeHistoryRepository()
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

    /** Un ViewModel con una lectura ya hecha y guardada, que es la situación en la que se anota. */
    private fun scannedViewModel(): Pair<ScannerViewModel, String> {
        val detection = detectionOf(ScannerEngineId.MlKitCameraX)
        val viewModel = viewModel(
            FakeEngine(
                id = ScannerEngineId.MlKitCameraX,
                events = listOf(ScanEvent.Detected(listOf(detection))),
            ),
        )
        viewModel.onAction(ScannerAction.ScreenShown)
        return viewModel to detection.id
    }

    @Test
    fun `la nota escrita en el escaner queda en el historial`() = runTest {
        val (viewModel, id) = scannedViewModel()

        viewModel.onAction(ScannerAction.EditNote(id))
        viewModel.onAction(ScannerAction.NoteDraftChanged("factura de marzo"))
        viewModel.onAction(ScannerAction.SaveNote)

        assertEquals("factura de marzo", history.noteOf(id))
        // Y vuelve a la pantalla por la vía normal: la emisión del historial, no un atajo interno.
        assertEquals("factura de marzo", viewModel.state.value.noteOf(id))
    }

    @Test
    fun `abrir el campo sobre una lectura ya anotada trae lo que habia escrito`() = runTest {
        // Es la razón de que el escáner observe las notas en vez de recordarlas. El id de una
        // detección es determinista, así que releer el mismo código devuelve la fila ya anotada: con
        // el campo abriéndose vacío, guardar habría borrado la nota sin que nadie lo pidiera.
        val (viewModel, id) = scannedViewModel()
        viewModel.onAction(ScannerAction.EditNote(id))
        viewModel.onAction(ScannerAction.NoteDraftChanged("almacén 3"))
        viewModel.onAction(ScannerAction.SaveNote)

        viewModel.onAction(ScannerAction.EditNote(id))

        assertEquals("almacén 3", viewModel.state.value.noteDraft)
    }

    @Test
    fun `guardar con el campo vacio quita la nota`() = runTest {
        val (viewModel, id) = scannedViewModel()
        viewModel.onAction(ScannerAction.EditNote(id))
        viewModel.onAction(ScannerAction.NoteDraftChanged("me equivoqué"))
        viewModel.onAction(ScannerAction.SaveNote)

        viewModel.onAction(ScannerAction.EditNote(id))
        viewModel.onAction(ScannerAction.NoteDraftChanged("   "))
        viewModel.onAction(ScannerAction.SaveNote)

        // Espacios en blanco no son una nota: `ScanHistory` los normaliza a `null` en un solo sitio.
        assertNull(history.noteOf(id))
        assertTrue(viewModel.state.value.notes.isEmpty())
    }

    @Test
    fun `cerrar sin guardar no toca la nota`() = runTest {
        val (viewModel, id) = scannedViewModel()

        viewModel.onAction(ScannerAction.EditNote(id))
        viewModel.onAction(ScannerAction.NoteDraftChanged("a medio escribir"))
        viewModel.onAction(ScannerAction.DismissNote)

        assertNull(history.noteOf(id))
        assertNull(viewModel.state.value.noteTargetId)
        assertEquals("", viewModel.state.value.noteDraft)
    }
}
