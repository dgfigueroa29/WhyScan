package com.whyscan.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whyscan.core.designsystem.Spacing
import com.whyscan.feature.settings.resources.Res
import com.whyscan.feature.settings.resources.news_dismiss
import com.whyscan.feature.settings.resources.news_dyslexia_body
import com.whyscan.feature.settings.resources.news_dyslexia_title
import com.whyscan.feature.settings.resources.news_export_body
import com.whyscan.feature.settings.resources.news_export_title
import com.whyscan.feature.settings.resources.news_groups_body
import com.whyscan.feature.settings.resources.news_groups_title
import com.whyscan.feature.settings.resources.news_notes_body
import com.whyscan.feature.settings.resources.news_notes_title
import com.whyscan.feature.settings.resources.news_search_body
import com.whyscan.feature.settings.resources.news_search_title
import com.whyscan.feature.settings.resources.news_title
import com.whyscan.feature.settings.resources.news_undo_body
import com.whyscan.feature.settings.resources.news_undo_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Qué hay de nuevo.
 *
 * ## Por qué existe
 *
 * Una función que nadie encuentra es una función que no está. La nota, el buscador y la agrupación
 * por día no se descubren solos: no añaden un botón en la pantalla principal, cambian lo que hace
 * una pantalla que el usuario ya creía conocer. Estrenarlas en silencio es la forma más cara de
 * hacer trabajo — se paga entero y no lo usa nadie.
 *
 * ## Por qué una revisión y no la versión de la app
 *
 * `versionName` sube por arreglos que no le importan a nadie. Lo que decide si hay algo que contar
 * es si **han pasado cosas contables** desde la última vez, y eso es un número aparte que solo sube
 * cuando se añade una entrada aquí. Además vive en `commonMain`: `versionName` solo existe en el
 * módulo de Android.
 */
object WhatsNew {

    /** Sube **solo** al añadir entradas a [ENTRIES]. Ver el KDoc de arriba. */
    const val REVISION = 1

    /**
     * Decide si hay algo que anunciar.
     *
     * `null` es "nunca se ha escrito", y devuelve `false` a propósito: **a quien acaba de instalar
     * la app no se le estrena nada**, porque para él todo es nuevo, y un diálogo de novedades en el
     * primer arranque es un obstáculo entre el usuario y lo que vino a hacer. Quien ya la tenía sí
     * lo ve, que es justo para quien está escrito.
     *
     * Es una función pura y por eso se puede probar sin levantar nada, que es lo que la salva de
     * ser una condición suelta dentro de un `LaunchedEffect` donde nadie la mira.
     */
    fun shouldAnnounce(lastSeenRevision: Int?): Boolean =
        lastSeenRevision != null && lastSeenRevision < REVISION

    /**
     * Las entradas, de más reciente a más antigua dentro de la misma tanda.
     *
     * Son `StringResource` y no cadenas: esta lista es una constante y los textos se resuelven en
     * la composición, con el idioma que esté puesto en ese momento.
     */
    val ENTRIES: List<WhatsNewEntry> = listOf(
        WhatsNewEntry(Res.string.news_notes_title, Res.string.news_notes_body),
        WhatsNewEntry(Res.string.news_search_title, Res.string.news_search_body),
        WhatsNewEntry(Res.string.news_groups_title, Res.string.news_groups_body),
        WhatsNewEntry(Res.string.news_undo_title, Res.string.news_undo_body),
        WhatsNewEntry(Res.string.news_export_title, Res.string.news_export_body),
        WhatsNewEntry(Res.string.news_dyslexia_title, Res.string.news_dyslexia_body),
    )
}

data class WhatsNewEntry(val title: StringResource, val body: StringResource)

/**
 * El diálogo de novedades.
 *
 * Un diálogo y no una pantalla en la navegación: aparece una vez, se lee y se cierra. Meterlo en el
 * backstack lo convertiría en un sitio al que se puede volver con el botón atrás desde donde no
 * tiene sentido.
 *
 * Lleva `verticalScroll` porque la lista crece con cada tanda y la altura de un diálogo no: sin
 * eso, la entrada número siete se quedaría fuera de la pantalla sin que nada avisara.
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.news_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                WhatsNew.ENTRIES.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            text = stringResource(entry.title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(entry.body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        // Un solo botón: no hay nada que rechazar. "Cancelar" al lado de una lista de novedades
        // sugeriría que se puede deshacer algo, y aquí no hay nada que deshacer.
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.news_dismiss)) }
        },
    )
}
