package com.whyscan.feature.scanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.whyscan.core.designsystem.LocalCodeValueStyle
import com.whyscan.core.designsystem.Radius
import com.whyscan.core.designsystem.Spacing
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.domain.scan.ResultActionsFactory
import com.whyscan.core.domain.scan.spokenValue
import com.whyscan.core.model.Detection
import com.whyscan.feature.scanner.resources.Res
import com.whyscan.feature.scanner.resources.a11y_note_add
import com.whyscan.feature.scanner.resources.a11y_note_edit
import com.whyscan.feature.scanner.resources.a11y_note_value
import com.whyscan.feature.scanner.resources.a11y_results_collapse
import com.whyscan.feature.scanner.resources.a11y_results_expand
import com.whyscan.feature.scanner.resources.action_scan_from_image
import com.whyscan.feature.scanner.resources.action_submit
import com.whyscan.feature.scanner.resources.detection_latency
import com.whyscan.feature.scanner.resources.detection_meta
import com.whyscan.feature.scanner.resources.manual_input_label
import com.whyscan.feature.scanner.resources.note_add
import com.whyscan.feature.scanner.resources.note_cancel
import com.whyscan.feature.scanner.resources.note_edit
import com.whyscan.feature.scanner.resources.note_hint
import com.whyscan.feature.scanner.resources.note_save
import com.whyscan.feature.scanner.resources.results_clear
import com.whyscan.feature.scanner.resources.results_hint_body
import com.whyscan.feature.scanner.resources.results_hint_title
import com.whyscan.feature.scanner.resources.results_more
import com.whyscan.feature.scanner.resources.results_show_less
import org.jetbrains.compose.resources.stringResource

/**
 * La hoja de resultados: lo que ocupa la parte baja de la pantalla debajo del visor.
 *
 * No es un `ModalBottomSheet` a propósito. Una hoja modal tapa la cámara y hay que arrastrarla para
 * volver a ver, que es exactamente lo contrario de lo que quiere quien escanea en serie: leer, mirar
 * el resultado y apuntar al siguiente sin tocar la pantalla. Esta empuja el visor hacia arriba en
 * lugar de taparlo, así que la cámara nunca deja de verse.
 */
@Composable
internal fun ResultsSheet(
    state: ScannerState,
    onAction: (ScannerAction) -> Unit,
    advancedMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = Spacing.xxs,
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                // Crecer y encogerse con animación en vez de dar un salto: la hoja cambia de altura
                // cada vez que llega una lectura, y sin esto el visor da un tirón hacia arriba.
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (state.isManualEntryActive) {
                ManualEntryField(state, onAction)
            }

            if (state.detections.isEmpty()) {
                EmptyResults(state, onAction)
            } else {
                DetectedResults(state, onAction, advancedMode)
            }
        }
    }
}

/**
 * El campo de nota, en un diálogo.
 *
 * Es lo único de esta pantalla que sí tapa la cámara, y a propósito: escribir una nota es lo
 * contrario de escanear en serie —el usuario ha parado a pensar qué es ese código— y un campo
 * embutido en la hoja empujaría el visor a saltar de tamaño con cada apertura del teclado.
 *
 * Lo monta [ScannerContent] y no la hoja de resultados, porque las tarjetas con el botón de anotar
 * salen en las **dos** disposiciones: colgarlo de la hoja dejaba el banco de pruebas abriendo un
 * diálogo que no existía.
 */
@Composable
internal fun NoteDialog(state: ScannerState, onAction: (ScannerAction) -> Unit) {
    val editing = state.noteOf(state.noteTargetId.orEmpty()) != null

    AlertDialog(
        onDismissRequest = { onAction(ScannerAction.DismissNote) },
        title = {
            Text(
                stringResource(if (editing) Res.string.note_edit else Res.string.note_add),
            )
        },
        text = {
            OutlinedTextField(
                value = state.noteDraft,
                onValueChange = { onAction(ScannerAction.NoteDraftChanged(it)) },
                placeholder = { Text(stringResource(Res.string.note_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            // Sin `enabled`: guardar con el campo vacío es cómo se borra una nota que ya existía, y
            // desactivar el botón dejaría al usuario sin forma de quitarla.
            TextButton(onClick = { onAction(ScannerAction.SaveNote) }) {
                Text(stringResource(Res.string.note_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ScannerAction.DismissNote) }) {
                Text(stringResource(Res.string.note_cancel))
            }
        },
    )
}

/**
 * Lo que se ve antes de la primera lectura.
 *
 * Dice qué hacer —apuntar— y no qué está pasando. Un "Sesión detenida" o un "Escaneando…" describe
 * el estado interno de la app y no ayuda a nadie a leer un código.
 */
@Composable
private fun EmptyResults(state: ScannerState, onAction: (ScannerAction) -> Unit) {
    Text(
        text = stringResource(Res.string.results_hint_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(Res.string.results_hint_body),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Escanear desde imagen se ofrece solo si algún motor disponible declara la fuente (RF-07). En
    // escritorio, donde hoy solo hay entrada manual en vivo, el botón no aparece.
    if (state.canScanFromImage) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onAction(ScannerAction.ScanFromImage) },
                enabled = !state.isDecodingImage,
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.md),
                )
                Text(
                    text = stringResource(Res.string.action_scan_from_image),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            if (state.isDecodingImage) {
                CircularProgressIndicator(modifier = Modifier.size(Spacing.lg))
            }
        }
    }
}

/** La lectura más reciente destacada, y el resto plegado detrás de un contador. */
@Composable
private fun DetectedResults(
    state: ScannerState,
    onAction: (ScannerAction) -> Unit,
    advancedMode: Boolean,
) {
    // La guarda va antes del `remember`: salir de un composable después de haber declarado estado
    // recordado es la clase de cosa que funciona hasta que alguien mueve una línea.
    val latest = state.latestDetection ?: return
    val older = state.detections.drop(1)
    var expanded by remember { mutableStateOf(false) }

    // La lectura nueva **entra**: sube desde abajo mientras la anterior se desvanece, en lugar de
    // sustituirla de un fotograma al siguiente. En escaneo continuo esa sustitución seca era lo que
    // hacía dudar de si la app había leído otro código o seguía enseñando el mismo.
    //
    // `contentKey` va sobre el id y no sobre la lectura entera: anotar un código produce un estado
    // nuevo con la misma lectura dentro, y sin esto la tarjeta se reanimaría al guardar la nota.
    AnimatedContent(
        targetState = latest,
        contentKey = { it.id },
        transitionSpec = {
            (fadeIn(tween(ARRIVAL_MILLIS)) + slideInVertically { height -> height / ARRIVAL_OFFSET })
                .togetherWith(fadeOut(tween(ARRIVAL_MILLIS)))
        },
        label = "última lectura",
    ) { detection ->
        DetectionCard(
            detection = detection,
            canShare = state.canShare,
            advancedMode = advancedMode,
            // La que se está yendo deja de estar destacada, y eso no es solo color: `highlighted`
            // marca la tarjeta como región viva, y dos a la vez harían que un lector de pantalla
            // anunciara la lectura vieja justo detrás de la nueva.
            highlighted = detection.id == state.latestDetection?.id,
            note = state.noteOf(detection.id),
            onAction = onAction,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (older.isNotEmpty()) {
            val spoken = stringResource(
                if (expanded) Res.string.a11y_results_collapse else Res.string.a11y_results_expand,
            )
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.semantics { contentDescription = spoken },
            ) {
                val label = if (expanded) {
                    stringResource(Res.string.results_show_less)
                } else {
                    stringResource(Res.string.results_more, older.size)
                }
                Text(label)
            }
        }

        TextButton(onClick = { onAction(ScannerAction.ClearDetections) }) {
            Text(stringResource(Res.string.results_clear))
        }
    }

    AnimatedVisibility(
        visible = expanded && older.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        // Altura acotada: la lista puede llegar al tope de cien lecturas y sin límite se comería el
        // visor entero, que es justo lo que esta disposición existe para evitar.
        LazyColumn(
            modifier = Modifier.heightIn(max = OLDER_RESULTS_MAX_HEIGHT),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(older, key = { it.id }) { detection ->
                DetectionCard(
                    detection = detection,
                    canShare = state.canShare,
                    advancedMode = advancedMode,
                    highlighted = false,
                    note = state.noteOf(detection.id),
                    onAction = onAction,
                )
            }
        }
    }
}

/**
 * Un resultado con sus acciones (RF-13).
 *
 * Las acciones salen de `ResultActionsFactory`, así que dependen de **qué significa** el código y no
 * de su formato: un QR con una URL ofrece "Abrir enlace" y el mismo QR con texto plano no.
 *
 * @param highlighted la lectura recién llegada. Se pinta sobre el contenedor primario para que se
 *   distinga de las anteriores sin depender de la posición — quien usa un lector de pantalla no ve
 *   que está arriba del todo, y por eso además se anuncia como región viva.
 * @param note la nota que ya tenga esa lectura en el historial, si tiene. Llega de fuera y no se
 *   guarda aquí: la tarjeta muestra lo que hay, y quien manda es el historial.
 */
@Composable
internal fun DetectionCard(
    detection: Detection,
    canShare: Boolean,
    advancedMode: Boolean,
    highlighted: Boolean,
    note: String?,
    onAction: (ScannerAction) -> Unit,
) {
    val actions = ResultActionsFactory.actionsFor(detection.barcode, canShare)
    val shareable = ResultActionsFactory.shareableContent(detection.barcode).asText()

    // El valor tal y como hay que **decirlo**, que no es como hay que escribirlo: un EAN-13 seguido
    // se pronuncia como una cifra de trece dígitos y deja de poder cotejarse contra la etiqueta.
    // Ver `spokenValue`. Se calcula una vez porque lo usan el propio valor y las cuatro etiquetas.
    val spoken = spokenValue(detection.barcode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (highlighted) liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Monoespaciada: es un dato que se coteja carácter a carácter contra una etiqueta
            // impresa, y en proporcional `1`, `l` e `I` se confunden.
            //
            // Y el mismo cotejo, para quien no ve la pantalla: sin la descripción de abajo el
            // lector pronuncia el valor como una cantidad y la lectura se vuelve inservible. La
            // decisión tipográfica y esta son la misma decisión por dos caminos distintos.
            Text(
                text = detection.barcode.rawValue,
                style = LocalCodeValueStyle.current,
                // Un QR puede traer un vCard entero. Tres líneas y elipsis: lo que no cabe se copia
                // o se comparte con los botones de abajo, que es lo que se hace con un valor largo.
                maxLines = CODE_VALUE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { contentDescription = spoken },
            )

            Text(
                text = buildString {
                    // El formato es útil siempre —saber que es un QR y no un EAN-13 orienta—; el id
                    // del motor y la latencia son medidas del banco de pruebas.
                    if (advancedMode) {
                        append(
                            stringResource(
                                Res.string.detection_meta,
                                detection.barcode.format.displayName,
                                detection.engineId.id,
                            ),
                        )
                        detection.latencyMillis?.let {
                            append(stringResource(Res.string.detection_latency, it))
                        }
                    } else {
                        append(detection.barcode.format.displayName)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            // La nota va **entre** el valor y las acciones, y no debajo de los botones: es lo que
            // le da sentido a un código que el usuario ya no recuerda, así que se lee antes de
            // decidir qué hacer con él.
            if (note != null) {
                val spokenNote = stringResource(Res.string.a11y_note_value, spoken)
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = NOTE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.semantics { contentDescription = spokenNote },
                )
            }

            // Anotar viaja ahora en la **misma** fila que las demás acciones, y no en una propia.
            // Estaba aparte porque un cuarto botón de texto se salía de la pantalla; con copiar y
            // compartir convertidos en iconos (ver `ResultActionLook`) el sitio sobra, y el lápiz
            // suelto en su propio renglón quedaba huérfano.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEachIndexed { index, action ->
                    ResultActionButton(
                        action = action,
                        spoken = spoken,
                        // La primera acción de la lectura destacada va rellena: es la que el
                        // usuario quiere el 90 % de las veces —abrir el enlace, copiar el número— y
                        // con tres botones iguales no había ninguna pista de cuál.
                        prominent = index == 0 && highlighted,
                        onClick = { onAction(ScannerAction.RunResultAction(action, shareable)) },
                    )
                }

                val spokenNoteAction = stringResource(
                    if (note == null) Res.string.a11y_note_add else Res.string.a11y_note_edit,
                    spoken,
                )
                IconButton(
                    onClick = { onAction(ScannerAction.EditNote(detection.id)) },
                    modifier = Modifier.semantics { contentDescription = spokenNoteAction },
                ) {
                    // El lápiz vale para las dos: si la lectura ya tiene nota se lee encima, así que
                    // el botón no tiene que contarlo. Quien no ve la pantalla sí lo distingue, por
                    // la descripción de arriba.
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                }
            }
        }
    }
}

/**
 * Un botón de acción sobre el resultado, con símbolo o con palabra según lo que diga [look].
 *
 * @param spoken el valor tal y como hay que **decirlo**. Con varios resultados en pantalla todos
 *   estos botones se llaman igual, y sin el valor dentro de la descripción un lector de pantalla
 *   anuncia "Copiar" cinco veces sin decir qué (RNF-05). Con el icono en lugar de la palabra esto
 *   deja de ser un detalle de accesibilidad y pasa a ser lo único que nombra al botón.
 * @param prominent si es la acción principal de la lectura destacada, que va rellena.
 */
@Composable
private fun ResultActionButton(
    action: ResultAction,
    spoken: String,
    prominent: Boolean,
    onClick: () -> Unit,
) {
    val spokenAction = stringResource(action.spokenResource(), spoken)
    val semantics = Modifier.semantics { contentDescription = spokenAction }

    when (val look = action.look()) {
        is ResultActionLook.Symbol -> if (prominent) {
            FilledIconButton(onClick = onClick, modifier = semantics) {
                Icon(imageVector = look.icon, contentDescription = null)
            }
        } else {
            IconButton(onClick = onClick, modifier = semantics) {
                Icon(imageVector = look.icon, contentDescription = null)
            }
        }

        is ResultActionLook.Word -> if (prominent) {
            Button(onClick = onClick, modifier = semantics) {
                Text(stringResource(look.label))
            }
        } else {
            TextButton(onClick = onClick, modifier = semantics) {
                Text(stringResource(look.label))
            }
        }
    }
}

/**
 * Campo de entrada manual. Vive en la hoja porque es la forma de "leer" cuando no hay cámara.
 *
 * Trae su propia `Column` en lugar de apoyarse en la de quien lo llame: emite tres hijos, y en el
 * banco de motores se le invoca desde un `item {}` de `LazyColumn`, cuyo ámbito **no** los apila —
 * los superpondría uno encima de otro.
 */
@Composable
internal fun ManualEntryField(state: ScannerState, onAction: (ScannerAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = state.manualInput,
            onValueChange = { onAction(ScannerAction.ManualInputChanged(it)) },
            label = { Text(stringResource(Res.string.manual_input_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onAction(ScannerAction.SubmitManualInput) },
            enabled = state.manualInput.isNotBlank(),
        ) {
            Text(stringResource(Res.string.action_submit))
        }
        HorizontalDivider()
    }
}

/** Lo que tarda una lectura nueva en ocupar el sitio de la anterior. */
private const val ARRIVAL_MILLIS = 180

/** Desde qué fracción de su propia altura sube. Un empujón, no un desplazamiento de pantalla. */
private const val ARRIVAL_OFFSET = 4

private const val CODE_VALUE_MAX_LINES = 3

/**
 * La nota se recorta antes que el valor. Es texto libre y aquí solo tiene que recordarle al
 * usuario qué es ese código; el texto entero está en el historial, que es donde se lee.
 */
private const val NOTE_MAX_LINES = 2

private val OLDER_RESULTS_MAX_HEIGHT = Spacing.xxl * 5
