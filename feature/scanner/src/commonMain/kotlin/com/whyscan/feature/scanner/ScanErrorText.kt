package com.whyscan.feature.scanner

import androidx.compose.runtime.Composable
import com.whyscan.core.model.ScanError
import com.whyscan.feature.scanner.resources.Res
import com.whyscan.feature.scanner.resources.error_camera_unavailable
import com.whyscan.feature.scanner.resources.error_cancelled
import com.whyscan.feature.scanner.resources.error_decode_failed
import com.whyscan.feature.scanner.resources.error_engine_unavailable
import com.whyscan.feature.scanner.resources.error_format_rejected
import com.whyscan.feature.scanner.resources.error_permission_denied
import com.whyscan.feature.scanner.resources.error_timeout
import com.whyscan.feature.scanner.resources.error_unexpected
import org.jetbrains.compose.resources.stringResource

/**
 * Un [ScanError] dicho en el idioma del usuario.
 *
 * ## Qué había antes
 *
 * `stringResource(Res.string.session_error, error.toString())`. En un `data class` de Kotlin,
 * `toString()` es el volcado de sus campos, así que la pantalla enseñaba literalmente:
 *
 *     Error: EngineUnavailable(engineId=null, reason=Motores descartados: gms_code_scanner,
 *     mlkit_camerax, zxing_cpp)
 *
 * En una app cuyo criterio de salida es que alguien lea un código **sin ver nunca la palabra
 * "motor"**. Y sin traducir: da igual el idioma que tenga puesto, el nombre de la clase sale en
 * inglés y los identificadores en `snake_case`.
 *
 * ## Por qué el `when` no lleva `else`
 *
 * `ScanError` es una jerarquía sellada, así que sin `else` el compilador obliga a contestar aquí
 * cada variante nueva. Es la misma garantía contra el estado imposible que usa `ViewfinderArea`, y
 * es exactamente lo que faltaba: una variante nueva de error no puede colarse en la pantalla como
 * un volcado de datos porque nadie se acordó de traducirla.
 *
 * ## Lo que se pierde, dicho
 *
 * El detalle técnico —qué motores se descartaron y por qué— **deja de estar en pantalla**, y eso es
 * información real para quien depura. No se tira: `ScanError` la sigue llevando en sus campos, y el
 * banco de motores del modo avanzado ya muestra el motivo de cada descarte, motor por motor, que es
 * donde esa información sirve de algo.
 */
@Composable
internal fun ScanError.readable(): String = when (this) {
    is ScanError.PermissionDenied -> stringResource(Res.string.error_permission_denied)
    is ScanError.CameraUnavailable -> stringResource(Res.string.error_camera_unavailable)
    is ScanError.EngineUnavailable -> stringResource(Res.string.error_engine_unavailable)
    is ScanError.DecodeFailed -> stringResource(Res.string.error_decode_failed)
    is ScanError.FormatRejected -> stringResource(Res.string.error_format_rejected)
    ScanError.Timeout -> stringResource(Res.string.error_timeout)
    ScanError.Cancelled -> stringResource(Res.string.error_cancelled)
    is ScanError.Unexpected -> stringResource(Res.string.error_unexpected)
}
