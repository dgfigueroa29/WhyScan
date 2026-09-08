package ar.net.faro.foundation

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypographyTest {

    @Test
    fun el_estilo_ajustado_crece_en_tamano_e_interlinea() {
        val base = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)
        val adjusted = base.forEasierReading()

        assertTrue(adjusted.fontSize.value > base.fontSize.value)
        assertTrue(adjusted.lineHeight.value > base.lineHeight.value)
    }

    @Test
    fun el_estilo_ajustado_gana_espacio_entre_letras() {
        val base = TextStyle(letterSpacing = 0.sp)
        val adjusted = base.forEasierReading()

        assertTrue(adjusted.letterSpacing.value > base.letterSpacing.value)
    }

    @Test
    fun el_estilo_ajustado_elimina_el_tracking_negativo() {
        val base = TextStyle(letterSpacing = (-0.5).sp)
        val adjusted = base.forEasierReading()

        assertTrue(adjusted.letterSpacing.value >= 0.75f) // EXTRA_LETTER_SPACING
    }

    @Test
    fun se_puede_cambiar_la_familia_al_ajustar() {
        val base = TextStyle(fontFamily = FontFamily.Default)
        val adjusted = base.forEasierReading(family = FontFamily.Monospace)

        assertEquals(FontFamily.Monospace, adjusted.fontFamily)
    }
}
