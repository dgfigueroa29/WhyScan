package com.whyscan.feature.history

import app.cash.turbine.test
import com.whyscan.core.domain.repository.ScanHistoryRepository
import com.whyscan.core.domain.usecase.ScanHistory
import com.whyscan.core.model.Detection
import com.whyscan.core.model.HistoryEntry
import com.whyscan.core.platform.NoOpPlatformActions
import com.whyscan.core.platform.SaveFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Qué pasa cuando el disco falla.
 *
 * Es el escenario que cerraba la app. Room abre el archivo de forma perezosa —en la primera
 * consulta, no al construir la base— y esa primera consulta ocurre siempre dentro de una corrutina,
 * así que **una base corrupta era un cierre en el arranque** que el usuario no podía deshacer sin
 * borrar los datos de la app.
 *
 * Estos tests son de los que no se pueden escribir después de un incidente: si esto se rompe, lo que
 * ve el usuario no es una excepción en un log sino una app que no abre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryFailureTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `si el historial no se puede leer, la pantalla lo dice en vez de cerrarse`() = runTest {
        val viewModel = viewModel(BrokenHistory())

        viewModel.state.test {
            val state = awaitItem()

            assertTrue(state.loadFailed, "el fallo de lectura no llegó al estado")
            assertFalse(state.isLoading, "se quedó cargando para siempre")
            // Y sobre todo: **no dice que no haya nada**. Un historial que no se pudo abrir puede
            // seguir teniendo doscientas lecturas dentro, y decirle al usuario "todavía no
            // escaneaste nada" es darle por perdido lo que sigue ahí.
            assertFalse(state.isEmpty, "dijo que el historial estaba vacío cuando no se pudo leer")
        }
    }

    @Test
    fun `si guardar una nota falla, se avisa y la app sigue viva`() = runTest {
        val viewModel = viewModel(BrokenHistory())

        viewModel.effects.test {
            viewModel.onAction(HistoryAction.SetNote("cualquiera", "factura de marzo"))

            assertEquals(
                HistoryEffect.ShowMessage(HistoryMessage.OperationFailed),
                awaitItem(),
            )
        }
    }

    @Test
    fun `vaciar el historial con la base rota tampoco cierra la app`() = runTest {
        val viewModel = viewModel(BrokenHistory())

        viewModel.effects.test {
            viewModel.onAction(HistoryAction.Clear)

            assertEquals(
                HistoryEffect.ShowMessage(HistoryMessage.OperationFailed),
                awaitItem(),
            )
        }
    }

    private fun viewModel(repository: ScanHistoryRepository) = HistoryViewModel(
        history = ScanHistory(repository),
        platformActions = NoOpPlatformActions(),
        // `FileSaver` es una `fun interface`: aquí basta con decir que el usuario canceló, porque
        // ninguno de estos tests exporta nada.
        fileSaver = { _, _, _ -> SaveFileResult.Cancelled },
    )

    /** Un almacén que falla en todo, como una base corrupta o un disco lleno. */
    private class BrokenHistory : ScanHistoryRepository {
        override fun observeHistory(): Flow<List<HistoryEntry>> = flow { error("base corrupta") }
        override suspend fun save(detection: Detection) = error("base corrupta")
        override suspend fun restore(entry: HistoryEntry) = error("base corrupta")
        override suspend fun setNote(detectionId: String, note: String?) = error("base corrupta")
        override suspend fun delete(detectionId: String) = error("base corrupta")
        override suspend fun clear() = error("base corrupta")
    }
}
