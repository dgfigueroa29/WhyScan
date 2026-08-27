package com.whyscan.platform

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lo que la Ronda 9 arregló, comprobado en vez de afirmado.
 *
 * Robolectric da un `ClipboardManager` de verdad en la JVM, así que esto corre en cada pull request
 * como el resto — no hace falta emulador y no contradice D6. Sin él, "el portapapeles marca el
 * contenido como sensible" sería una frase en un comentario, que es exactamente el estado en el que
 * estaba la garantía de `allowBackup` antes de que alguien la mirara.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidPlatformActionsTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val actions get() = AndroidPlatformActions(context)

    @Test
    fun `lo copiado se marca como sensible para que el sistema no lo previsualice`() = runTest {
        assertTrue(actions.copyToClipboard("WIFI:T:WPA;S:mi-red;P:clave-secreta;;"))

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val description = requireNotNull(clipboard.primaryClip).description

        assertTrue(
            requireNotNull(description.extras).getBoolean("android.content.extra.IS_SENSITIVE"),
            "sin este extra, Android 13+ pinta la contraseña en una previsualización flotante",
        )
    }

    @Test
    fun `el valor copiado sigue siendo el valor, marcarlo no lo cambia`() = runTest {
        val value = "7501234567890"
        actions.copyToClipboard(value)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = requireNotNull(clipboard.primaryClip)

        assertEquals(1, clip.itemCount)
        assertEquals(value, clip.getItemAt(0).text.toString())
    }

    @Test
    fun `abrir un esquema fuera de la lista blanca no lanza nada`() = runTest {
        // El valor devuelto es lo observable: `false` significa que la UI avisará de que no se pudo
        // abrir, que es justo lo que debe pasar con un `intent://` venido de un código.
        assertFalse(actions.openUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(actions.openUrl("javascript:alert(1)"))
        assertFalse(actions.openUrl("content://com.otra.app/datos/1"))
    }
}
