package com.whyscan.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cuándo se anuncian las novedades.
 *
 * La regla cabe en una línea y aun así tiene el caso que más molesta si se hace mal: el primer
 * arranque. Por eso está en una función pura y no dentro de un `LaunchedEffect`, donde habría sido
 * una condición suelta que nadie mira.
 */
class WhatsNewTest {

    @Test
    fun `a quien acaba de instalar no se le estrena nada`() {
        // `null` es "nunca se ha escrito". Para quien abre la app por primera vez todo es nuevo, y
        // un diálogo de novedades entre él y lo que vino a hacer es puro estorbo.
        assertFalse(WhatsNew.shouldAnnounce(lastSeenRevision = null))
    }

    @Test
    fun `a quien ya tenia la app se le cuenta lo que cambio`() {
        assertTrue(WhatsNew.shouldAnnounce(lastSeenRevision = WhatsNew.REVISION - 1))
    }

    @Test
    fun `no se repite a quien ya las vio`() {
        assertFalse(WhatsNew.shouldAnnounce(lastSeenRevision = WhatsNew.REVISION))
    }

    @Test
    fun `una revision del futuro tampoco anuncia nada`() {
        // Pasa al instalar una versión más vieja encima de una más nueva. No es un caso frecuente,
        // pero enseñar como novedad algo que el usuario ya vio sería peor que no enseñar nada.
        assertFalse(WhatsNew.shouldAnnounce(lastSeenRevision = WhatsNew.REVISION + 1))
    }

    @Test
    fun `hay entradas que contar, y sin repetir`() {
        // Una revisión que sube sin entradas nuevas es un diálogo vacío delante del usuario. Y dos
        // entradas con el mismo texto significan que alguien copió una línea y olvidó cambiarla.
        assertTrue(WhatsNew.ENTRIES.isNotEmpty())
        assertEquals(WhatsNew.ENTRIES.size, WhatsNew.ENTRIES.map { it.title }.toSet().size)
        assertEquals(WhatsNew.ENTRIES.size, WhatsNew.ENTRIES.map { it.body }.toSet().size)
    }
}
