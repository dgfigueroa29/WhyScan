package com.whyscan.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import com.whyscan.core.platform.PlatformActions
import com.whyscan.core.platform.isOpenableUri

/**
 * Acciones del sistema en Android.
 *
 * Todas se lanzan desde el `Context` de aplicación, no desde una Activity: el controlador es un
 * singleton del grafo y retener la Activity filtraría memoria en cada rotación. El precio es
 * `FLAG_ACTIVITY_NEW_TASK`, obligatorio al arrancar una Activity fuera de otra.
 */
class AndroidPlatformActions(private val context: Context) : PlatformActions {

    override val canShare: Boolean = true

    /**
     * Copia marcando el contenido como **sensible**, siempre.
     *
     * Desde Android 13 el sistema muestra una previsualización flotante con lo que se acaba de
     * copiar. Encima de cualquier app, y por tanto delante de quien esté mirando la pantalla. Eso
     * convierte copiar un QR de WiFi —cuyo `rawValue` es literalmente `WIFI:T:WPA;S:red;P:clave;;`—
     * en enseñar la contraseña.
     *
     * **Es la misma forma que el `allowBackup` de la Ronda 5**, y por eso vale la pena decirlo aquí:
     * la promesa de privacidad de este producto se apoya en no declarar `INTERNET`, y esto tampoco
     * lo hace la app — lo hace un proceso del sistema al que ese permiso le da igual. Son ya dos
     * puertas del mismo tipo.
     *
     * ### Por qué siempre y no solo cuando el valor parece secreto
     * Porque **el usuario ya está viendo el valor**: la pantalla lo tiene delante, encima del botón
     * que acaba de pulsar. La previsualización del sistema no le informa de nada que no sepa, así
     * que su utilidad aquí es cercana a cero mientras que su coste, en el peor caso, es una
     * credencial en pantalla. Con esa asimetría, clasificar qué códigos son "sensibles" sería
     * añadir una decisión que puede equivocarse a cambio de no ganar nada.
     */
    override suspend fun copyToClipboard(text: String): Boolean = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(CLIP_LABEL, text).apply {
            description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)
    }.isSuccess

    override suspend fun share(text: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // `createChooser` y no el intent pelado: sin él, Android recuerda una app por defecto y el
        // usuario pierde la posibilidad de elegir a dónde manda cada resultado.
        return start(Intent.createChooser(intent, null))
    }

    /**
     * La lista blanca de esquemas, comprobada donde se ejecuta y no solo donde se decide.
     *
     * `ACTION_VIEW` con un `intent://` o un `content://` arranca componentes de otras apps con
     * datos que vienen del código escaneado, y el contenido de un código lo controla entero quien
     * lo imprime. Que hoy solo lleguen aquí los seis esquemas del dominio es una propiedad del
     * grafo de llamadas, no de este método.
     */
    override suspend fun openUrl(url: String): Boolean {
        if (!isOpenableUri(url)) return false
        return start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun start(intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    private companion object {
        const val CLIP_LABEL = "WhyScan"

        /**
         * `ClipDescription.EXTRA_IS_SENSITIVE`, escrito como literal a propósito.
         *
         * La constante solo existe a partir de API 33 y este proyecto tiene `minSdk` 24. Es un
         * `String` de compilación, así que referenciarla se inlinea y funcionaría igual — pero el
         * lint de release lo señalaría como uso de API nueva y habría que silenciarlo. El literal
         * evita esa supresión y, de paso, lo entienden las capas de fabricante que ya honraban
         * este extra antes de que Google lo documentara.
         */
        const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    }
}
