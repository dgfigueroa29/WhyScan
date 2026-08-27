package com.whyscan.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.whyscan.core.designsystem.LocalCodeValueStyle
import com.whyscan.core.designsystem.Spacing
import com.whyscan.core.domain.scan.OpenKind
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.domain.scan.ResultActionsFactory
import com.whyscan.core.domain.scan.spokenValue
import com.whyscan.core.model.Detection
import com.whyscan.core.model.HistoryEntry
import com.whyscan.feature.history.resources.Res
import com.whyscan.feature.history.resources.a11y_copy_value
import com.whyscan.feature.history.resources.a11y_delete_value
import com.whyscan.feature.history.resources.a11y_note_value
import com.whyscan.feature.history.resources.a11y_open_value
import com.whyscan.feature.history.resources.a11y_share_value
import com.whyscan.feature.history.resources.history_note_add
import com.whyscan.feature.history.resources.history_note_cancel
import com.whyscan.feature.history.resources.history_note_edit
import com.whyscan.feature.history.resources.history_note_hint
import com.whyscan.feature.history.resources.history_note_save
import com.whyscan.feature.history.resources.history_row_latency
import com.whyscan.feature.history.resources.history_row_meta
import com.whyscan.feature.history.resources.result_open_email
import com.whyscan.feature.history.resources.result_open_link
import com.whyscan.feature.history.resources.result_open_map
import com.whyscan.feature.history.resources.result_open_phone
import com.whyscan.feature.history.resources.result_open_sms
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Una lectura del historial: el valor, sus metadatos, la nota y las acciones.
 *
 * @param isEditingNote si esta fila tiene el campo de nota abierto. Lo decide el estado y no la fila,
 *   para que abrir una cierre la anterior: dos campos de texto abiertos a la vez sobre una lista es
 *   una forma fácil de escribir en el sitio equivocado.
 */
@Composable
internal fun HistoryRow(
    entry: HistoryEntry,
    canShare: Boolean,
    advancedMode: Boolean,
    isEditingNote: Boolean,
    onAction: (HistoryAction) -> Unit,
) {
    val detection = entry.detection
    val actions = ResultActionsFactory.actionsFor(detection.barcode, canShare)
    val shareable = ResultActionsFactory.shareableContent(detection.barcode).asText()

    // El valor como hay que **decirlo**, no como hay que escribirlo. Ver `spokenValue`.
    val spoken = spokenValue(detection.barcode)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Monoespaciada por lo mismo que en la pantalla de escaneo: es un dato que se coteja
            // carácter a carácter, y en una proporcional `1`, `l` e `I` se confunden. La
            // descripción es ese mismo cotejo para quien no ve la pantalla — ver `spokenValue`.
            Text(
                text = detection.barcode.rawValue,
                style = LocalCodeValueStyle.current,
                modifier = Modifier.semantics { contentDescription = spoken },
            )

            Text(
                text = detection.metaLine(advancedMode),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isEditingNote) {
                NoteEditor(entry = entry, onAction = onAction)
            } else if (entry.hasNote) {
                NoteText(entry)
            }

            RowActions(
                entry = entry,
                actions = actions,
                shareable = shareable,
                isEditingNote = isEditingNote,
                onAction = onAction,
            )
        }
    }
}

/**
 * La nota, cuando la hay y no se está editando.
 *
 * Va en `bodyMedium` sobre `onSurface` y no en el gris pequeño de los metadatos: la escribió una
 * persona para poder reconocer esta fila, así que pesa más que el formato o la latencia. Con el
 * valor en monoespaciada encima, la diferencia de familia ya separa el dato de la anotación sin
 * necesidad de una etiqueta que diga "Nota:".
 */
@Composable
private fun NoteText(entry: HistoryEntry) {
    val spoken = stringResource(Res.string.a11y_note_value, spokenValue(entry.detection.barcode))

    Text(
        text = entry.note.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { contentDescription = "$spoken. ${entry.note.orEmpty()}" },
    )
}

/**
 * El campo de nota, con su borrador local.
 *
 * El texto a medio escribir vive en un `remember` de la fila y no en el estado del ViewModel, a
 * propósito: cada pulsación de tecla no tiene por qué recorrer un `StateFlow` y repintar la lista
 * entera. Lo que sí sabe el ViewModel es **qué fila** está abierta, que es lo que necesita para que
 * abrir una cierre la otra. La clave del `remember` es el id, así que reciclar la fila para otra
 * lectura no arrastra el borrador de la anterior.
 */
@Composable
private fun NoteEditor(entry: HistoryEntry, onAction: (HistoryAction) -> Unit) {
    var draft by remember(entry.id) { mutableStateOf(entry.note.orEmpty()) }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.history_note_hint)) },
        singleLine = false,
        maxLines = NOTE_MAX_LINES,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TextButton(onClick = { onAction(HistoryAction.SetNote(entry.id, draft)) }) {
            Text(stringResource(Res.string.history_note_save))
        }
        TextButton(onClick = { onAction(HistoryAction.EditNote(null)) }) {
            Text(stringResource(Res.string.history_note_cancel))
        }
    }
}

/** Copiar, compartir, abrir, anotar y eliminar. */
@Composable
private fun RowActions(
    entry: HistoryEntry,
    actions: List<ResultAction>,
    shareable: String,
    isEditingNote: Boolean,
    onAction: (HistoryAction) -> Unit,
) {
    // El valor como hay que decirlo, no como hay que escribirlo: ver `spokenValue`. En una lista
    // larga las descripciones son lo único que distingue un botón del de la fila de arriba, así que
    // aquí importa el doble.
    val value = spokenValue(entry.detection.barcode)

    // Cinco botones caben en una fila **si no son palabras**. Con "Abrir enlace · Copiar ·
    // Compartir · Agregar nota · Eliminar" la fila se salía de la pantalla en cualquier teléfono, y
    // con el cuerpo de letra subido no llegaba ni a la mitad. Lo que dicen no se pierde: es su
    // descripción hablada, que además lleva el valor dentro (RNF-05).
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            // El historial es una lista larga de botones que se llaman igual. Sin el valor
            // dentro de la descripción, un lector de pantalla los hace indistinguibles.
            val spoken = stringResource(action.spokenResource(), value)
            val onClick = { onAction(HistoryAction.RunResultAction(action, shareable)) }
            val semantics = Modifier.semantics { contentDescription = spoken }

            // Abrir conserva su palabra: "Abrir enlace", "Llamar" y "Ver en el mapa" son cosas
            // distintas que ningún icono separa. Copiar y compartir sí tienen el suyo.
            when (val look = action.look()) {
                is ResultActionLook.Symbol -> IconButton(onClick = onClick, modifier = semantics) {
                    Icon(imageVector = look.icon, contentDescription = null)
                }

                is ResultActionLook.Word -> TextButton(onClick = onClick, modifier = semantics) {
                    Text(stringResource(look.label))
                }
            }
        }

        if (!isEditingNote) {
            // El lápiz vale para anotar y para editar: si ya hay nota, se lee justo encima. La
            // diferencia la dice la descripción, que es lo que oye quien no ve la pantalla.
            val noteSpoken = stringResource(
                if (entry.hasNote) Res.string.history_note_edit else Res.string.history_note_add,
            )
            IconButton(
                onClick = { onAction(HistoryAction.EditNote(entry.id)) },
                modifier = Modifier.semantics { contentDescription = noteSpoken },
            ) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
            }
        }

        val deleteSpoken = stringResource(Res.string.a11y_delete_value, value)
        IconButton(
            onClick = { onAction(HistoryAction.Delete(entry.id)) },
            modifier = Modifier.semantics { contentDescription = deleteSpoken },
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                // El único botón de la fila que destruye algo, y el color es lo que lo dice ahora
                // que no hay palabra. Deshacer sigue existiendo, en el aviso que sale al borrar.
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Formato, y en modo avanzado también el motor y la latencia. */
@Composable
private fun Detection.metaLine(advancedMode: Boolean): String = buildString {
    if (advancedMode) {
        append(
            stringResource(Res.string.history_row_meta, barcode.format.displayName, engineId.id),
        )
        latencyMillis?.let { append(stringResource(Res.string.history_row_latency, it)) }
    } else {
        append(barcode.format.displayName)
    }
}

/** Cómo la anuncia un lector de pantalla, con el valor dentro para distinguir un botón de otro. */
private fun ResultAction.spokenResource(): StringResource = when (this) {
    ResultAction.Copy -> Res.string.a11y_copy_value
    ResultAction.Share -> Res.string.a11y_share_value
    is ResultAction.Open -> Res.string.a11y_open_value
}

/**
 * Cómo se dibuja cada acción sobre el resultado: con símbolo o con palabra (RF-13).
 *
 * Es la misma regla que en la pantalla de escaneo, y está escrita dos veces por lo mismo que
 * [spokenResource]: las cadenas son **por módulo** y ninguna de las dos features puede leer las de
 * la otra. Lo que decide la forma es si el símbolo basta — copiar y compartir tienen uno que ya no
 * hay que aprender; las cinco maneras de abrir, no.
 */
private sealed interface ResultActionLook {

    data class Symbol(val icon: ImageVector) : ResultActionLook

    data class Word(val label: StringResource) : ResultActionLook
}

/** Ver [ResultActionLook]. */
private fun ResultAction.look(): ResultActionLook = when (this) {
    ResultAction.Copy -> ResultActionLook.Symbol(Icons.Filled.ContentCopy)
    ResultAction.Share -> ResultActionLook.Symbol(Icons.Filled.Share)
    is ResultAction.Open -> ResultActionLook.Word(
        when (kind) {
            OpenKind.Link -> Res.string.result_open_link
            OpenKind.Email -> Res.string.result_open_email
            OpenKind.Phone -> Res.string.result_open_phone
            OpenKind.Sms -> Res.string.result_open_sms
            OpenKind.Map -> Res.string.result_open_map
        },
    )
}

/** Una nota larga puede crecer, pero no puede comerse la lista. */
private const val NOTE_MAX_LINES = 4
