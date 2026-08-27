package com.whyscan.feature.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whyscan.core.designsystem.Spacing
import com.whyscan.core.scanner.ui.CameraPreviewEngine
import com.whyscan.feature.scanner.resources.Res
import com.whyscan.feature.scanner.resources.fullscreen_close
import com.whyscan.feature.scanner.resources.fullscreen_title
import org.jetbrains.compose.resources.stringResource

/**
 * El visor ocupando la pantalla entera, para probar un motor recién elegido en el catálogo.
 *
 * ## Por qué un diálogo y no otra pantalla
 *
 * Porque "pantalla completa" tiene que ser **completa**. La pantalla de escaneo vive dentro del
 * `Scaffold` de la app, entre la barra de navegación de abajo y los insets de arriba: cualquier cosa
 * que se dibuje ahí nace con un recorte que no puede quitarse desde dentro. Un `Dialog` con
 * `usePlatformDefaultWidth = false` se pinta por encima de todo eso sin que la feature tenga que
 * negociar nada con la raíz de la app ni añadir un destino a la navegación — que además sería un
 * sitio al que se puede volver con el botón atrás desde donde no tiene sentido.
 *
 * El botón atrás lo cierra, que es lo que hace un diálogo por defecto y lo que aquí se espera.
 *
 * ## Qué se reutiliza
 *
 * Todo: el visor es [ViewfinderArea] tal cual —con sus estados, su linterna, su zoom y su píldora de
 * sesión— y el resultado es la misma [DetectionCard] de la hoja. Duplicar cualquiera de los dos
 * habría creado una segunda versión del visor que se queda atrás a la primera corrección.
 *
 * @param previewEngine la superficie del motor que se está probando. Mientras esto está en pantalla,
 *   el visor de debajo **no** compone la suya: ver `previewMoved` en [ViewfinderArea].
 */
@Composable
internal fun FullScreenPreview(
    state: ScannerState,
    onAction: (ScannerAction) -> Unit,
    previewEngine: CameraPreviewEngine?,
) {
    Dialog(
        onDismissRequest = { onAction(ScannerAction.CloseFullScreen) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // El diálogo se pinta por encima de las barras del sistema, así que los insets hay que
            // respetarlos aquí: sin esto el botón de cerrar queda debajo del reloj.
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Header(state, onAction)

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // `advancedMode = true`: quien llega aquí viene del banco de motores y está
                    // probando uno concreto, así que la píldora dice **cuál** está leyendo.
                    ViewfinderArea(
                        state = state,
                        previewEngine = previewEngine,
                        onAction = onAction,
                        advancedMode = true,
                    )
                }

                // Solo la última lectura, y no la lista entera: aquí se está probando un motor, y la
                // pregunta es si lee. El historial guarda todas y la hoja de resultados las enseña.
                state.latestDetection?.let { detection ->
                    Box(modifier = Modifier.padding(Spacing.md)) {
                        DetectionCard(
                            detection = detection,
                            canShare = state.canShare,
                            advancedMode = true,
                            highlighted = true,
                            note = state.noteOf(detection.id),
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cerrar, y el nombre del motor que está leyendo.
 *
 * El nombre es la mitad del sentido de esta pantalla: se abrió para ver a **uno** trabajando, y sin
 * decir cuál es solo un visor más. Sale del descriptor —ver `ScannerState.activeEngineName`—, así
 * que es el mismo texto que el usuario acaba de leer en la ficha.
 */
@Composable
private fun Header(state: ScannerState, onAction: (ScannerAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onAction(ScannerAction.CloseFullScreen) }) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.fullscreen_close),
            )
        }
        Text(
            // Mientras la sesión arranca todavía no hay motor activo, y el hueco se llena con el
            // título genérico en vez de con una línea vacía que salta al llegar el nombre.
            text = state.activeEngineName ?: stringResource(Res.string.fullscreen_title),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
