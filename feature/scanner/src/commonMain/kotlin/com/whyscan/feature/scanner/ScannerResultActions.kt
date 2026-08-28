package com.whyscan.feature.scanner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.whyscan.core.domain.scan.OpenKind
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.domain.scan.ShareableContent
import com.whyscan.feature.scanner.resources.Res
import com.whyscan.feature.scanner.resources.a11y_copy_value
import com.whyscan.feature.scanner.resources.a11y_open_value
import com.whyscan.feature.scanner.resources.a11y_share_value
import com.whyscan.feature.scanner.resources.result_open_email
import com.whyscan.feature.scanner.resources.result_open_link
import com.whyscan.feature.scanner.resources.result_open_map
import com.whyscan.feature.scanner.resources.result_open_phone
import com.whyscan.feature.scanner.resources.result_open_sms
import com.whyscan.feature.scanner.resources.share_separator
import com.whyscan.feature.scanner.resources.share_wifi
import com.whyscan.feature.scanner.resources.share_wifi_with_password
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Cómo se nombran las acciones sobre un resultado (RF-13).
//
// Están en su propio archivo porque las usan la hoja de resultados y el banco de motores, que ya no
// viven en el mismo fichero. Son `internal` y no `private` justo por eso.

/** Cómo la anuncia un lector de pantalla, con el valor dentro para poder distinguir un botón de otro. */
internal fun ResultAction.spokenResource(): StringResource = when (this) {
    ResultAction.Copy -> Res.string.a11y_copy_value
    ResultAction.Share -> Res.string.a11y_share_value
    is ResultAction.Open -> Res.string.a11y_open_value
}

/**
 * Cómo se dibuja una acción sobre el resultado: con símbolo o con palabra.
 *
 * No es una preferencia estética, es dónde cabe. Una lectura ofrece hasta tres acciones y debajo va
 * la de anotar: con cuatro palabras seguidas, la fila se salía de la pantalla en cuanto el idioma
 * alargaba una etiqueta —"Abrir enlace", "Compartir"— o el usuario subía el tamaño de letra.
 *
 * La línea entre unas y otras es si el símbolo **basta**. Copiar y compartir tienen uno que ya no
 * hay que aprender, y su palabra no añadía nada. Abrir no lo tiene: "Abrir enlace", "Llamar",
 * "Escribir", "Enviar SMS" y "Ver en el mapa" son cinco cosas distintas que ningún icono separa, y
 * ahí la palabra es lo único que dice qué va a pasar al tocar.
 *
 * Para quien no ve la pantalla no cambia nada: la descripción hablada sigue siendo la de
 * [spokenResource], que además lleva el valor dentro (RNF-05).
 */
internal sealed interface ResultActionLook {

    data class Symbol(val icon: ImageVector) : ResultActionLook

    data class Word(val label: StringResource) : ResultActionLook
}

/** Ver [ResultActionLook]. */
internal fun ResultAction.look(): ResultActionLook = when (this) {
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

/**
 * Redacta lo que se copia o se comparte.
 *
 * El dominio dice qué datos son relevantes; el texto se arma aquí, donde están los recursos
 * traducibles. Antes la frase se componía en `ResultActionsFactory`, que era español dentro del
 * dominio (deuda D15).
 */
@Composable
internal fun ShareableContent.asText(): String = when (this) {
    is ShareableContent.Raw -> value

    is ShareableContent.Wifi ->
        password
            ?.let { stringResource(Res.string.share_wifi_with_password, ssid, it) }
            ?: stringResource(Res.string.share_wifi, ssid)

    is ShareableContent.Contact -> parts.joinToString(stringResource(Res.string.share_separator))
}
