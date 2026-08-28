package com.whyscan.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import com.whyscan.core.designsystem.WhyScanTheme
import com.whyscan.core.model.Barcode
import com.whyscan.core.model.BarcodeFormat
import com.whyscan.core.model.Detection
import com.whyscan.core.model.HistoryEntry
import com.whyscan.core.model.ScannerEngineId
import kotlin.test.Test

/**
 * Que lo que un lector de pantalla anuncia sea lo que la Ronda 10 decidió que anunciara.
 *
 * ## Qué agujero cierra
 *
 * `spokenValue` tiene sus propios tests —cuándo se deletrea y cuándo no— y es una función pura del
 * dominio. Lo que **nadie comprobaba** eran sus seis llamadas: que la descripción llegue al nodo,
 * que los botones la lleven dentro y que no se pierda por el camino. Era el paso tres de la Ronda
 * 13, y dejó de ser opcional cuando copiar, compartir, anotar y eliminar pasaron a ser **iconos**:
 * desde entonces la descripción hablada no es un extra de accesibilidad, es **lo único que nombra
 * al botón**.
 *
 * ## Por qué aquí y no en `:composeApp`
 *
 * Porque [HistoryContent] es stateless: recibe el estado y no toca ni Koin ni la base. Montar la
 * app entera para esto obligaría a sembrar Room y añadiría veinte razones de fallo ajenas a lo que
 * se quiere afirmar. `AppCompositionTest` existe para lo otro — que la raíz **se monte**.
 *
 * Corre en la JVM con `runComposeUiTest`, sin emulador y sin ventana, así que cumple la regla del
 * proyecto: todo lo que se comprueba se ejecuta en cada PR.
 *
 * ## En inglés
 *
 * `values/` es el catálogo sin calificador y por tanto el respaldo de cualquier idioma; el runner de
 * CI no está en español. Es el mismo criterio que `AppCompositionTest`.
 */
@OptIn(ExperimentalTestApi::class)
class HistorySemanticsTest {

    @Test
    fun `un codigo de producto se anuncia cifra a cifra, y no como una cantidad`() =
        runComposeUiTest {
            // Escrito seguido, un EAN-13 lo pronuncia TalkBack como un número de trece cifras y deja
            // de poder cotejarse contra la etiqueta impresa. Ver `spokenValue`.
            setContent { WhyScanTheme(darkTheme = false) { Historial(entryOf(EAN)) } }

            onNodeWithContentDescription(EAN_HABLADO).assertExists()
        }

    @Test
    fun `los botones sin palabra llevan el valor dentro`() = runComposeUiTest {
        // Copiar, compartir y eliminar son iconos desde la Ronda 15. En una lista larga, todos los
        // de una columna son idénticos: sin el valor en la descripción, un lector de pantalla los
        // hace indistinguibles (RNF-05).
        setContent { WhyScanTheme(darkTheme = false) { Historial(entryOf(EAN)) } }

        onNodeWithContentDescription("Copy $EAN_HABLADO").assertExists()
        onNodeWithContentDescription("Share $EAN_HABLADO").assertExists()
        onNodeWithContentDescription("Delete $EAN_HABLADO").assertExists()
    }

    @Test
    fun `dos filas distintas no se anuncian igual`() = runComposeUiTest {
        // La afirmación de arriba se sostiene sola con una fila. Lo que de verdad se prometió en la
        // Ronda 10 es que **en una lista** se pueda distinguir un botón del de la fila de al lado.
        setContent { WhyScanTheme(darkTheme = false) { Historial(entryOf(EAN), entryOf(OTRO_EAN)) } }

        onAllNodesWithContentDescription("Copy $EAN_HABLADO").assertCountEquals(1)
        onAllNodesWithContentDescription("Copy $OTRO_EAN_HABLADO").assertCountEquals(1)
    }

    @Test
    fun `una URL no se deletrea`() = runComposeUiTest {
        // `h t t p s d o s p u n t o s…` no lo entiende nadie. Deletrear solo tiene sentido sobre lo
        // que no es una palabra, y esa decisión vive en el dominio: aquí se comprueba que llega.
        setContent { WhyScanTheme(darkTheme = false) { Historial(entryOf(URL, BarcodeFormat.QrCode)) } }

        onNodeWithContentDescription(URL).assertExists()
    }

    /** La pantalla sin nada alrededor: ni Koin, ni base, ni la raíz de la app. */
    @Composable
    private fun Historial(vararg entries: HistoryEntry) {
        HistoryContent(
            state = HistoryState(isLoading = false, entries = entries.toList(), canShare = true),
            onAction = {},
        )
    }

    private fun entryOf(
        value: String,
        format: BarcodeFormat = BarcodeFormat.Ean13,
    ): HistoryEntry = HistoryEntry(
        Detection.of(
            barcode = Barcode(rawValue = value, format = format),
            engineId = ScannerEngineId.ManualInput,
            detectedAtMillis = INSTANTE,
        ),
    )

    private companion object {
        const val EAN = "7501234567893"
        const val EAN_HABLADO = "7 5 0 1 2 3 4 5 6 7 8 9 3"

        const val OTRO_EAN = "4006381333931"
        const val OTRO_EAN_HABLADO = "4 0 0 6 3 8 1 3 3 3 9 3 1"

        const val URL = "https://whyscan.test/a"

        /** Un instante fijo: la fila se agrupa por día y el test no debe depender de cuándo corre. */
        const val INSTANTE = 1_756_000_000_000L
    }
}
