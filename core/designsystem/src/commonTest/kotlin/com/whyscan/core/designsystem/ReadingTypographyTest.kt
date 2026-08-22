package com.whyscan.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El modo dislexia, medido en vez de descrito.
 *
 * Un ajuste tipográfico es de las cosas más fáciles de romper sin que nadie se entere: basta con que
 * un estilo nuevo se añada a la escala y no pase por la transformación para que ese texto se quede
 * como estaba, y en pantalla no se nota hasta que alguien lo necesita. Estos tests recorren **los
 * quince roles** y no una muestra.
 */
class ReadingTypographyTest {

    private val normal = whyScanTypography(easierReading = false)
    private val easier = whyScanTypography(easierReading = true)

    private fun Typography.allStyles(): List<TextStyle> = listOf(
        displayLarge,
        displayMedium,
        displaySmall,
        headlineLarge,
        headlineMedium,
        headlineSmall,
        titleLarge,
        titleMedium,
        titleSmall,
        bodyLarge,
        bodyMedium,
        bodySmall,
        labelLarge,
        labelMedium,
        labelSmall,
    )

    @Test
    fun `sin el modo activo la escala es exactamente la de siempre`() {
        // Que activarlo cambie cosas está bien; que **no** activarlo no cambie ninguna es lo que
        // permite que este modo se pueda añadir sin rediseñar la app para todos los demás.
        assertEquals(WhyScanTypography, normal)
        assertEquals(CodeValueStyle, codeValueStyle(easierReading = false))
    }

    @Test
    fun `todos los roles crecen`() {
        normal.allStyles().zip(easier.allStyles()).forEach { (before, after) ->
            assertTrue(
                after.fontSize.value > before.fontSize.value,
                "un rol se quedó en ${before.fontSize} al activar el modo",
            )
        }
    }

    @Test
    fun `todos los roles ganan espacio entre letras`() {
        // Es lo único de este modo con evidencia sólida detrás (Zorzi et al., PNAS 2012), así que
        // es lo que más merece un test: que no quede ningún rol sin ese espacio.
        normal.allStyles().zip(easier.allStyles()).forEach { (before, after) ->
            assertTrue(
                after.letterSpacing.value > before.letterSpacing.value,
                "un rol conservó su tracking de ${before.letterSpacing}",
            )
        }
    }

    @Test
    fun `ningun rol conserva tracking negativo`() {
        // Los estilos grandes venían apretados a propósito, por estética tipográfica. Eso es
        // exactamente lo contrario de lo que este modo busca, así que el suelo es cero.
        assertTrue(normal.displayLarge.letterSpacing.value < 0f, "el caso a corregir ya no existe")

        easier.allStyles().forEach { style ->
            assertTrue(style.letterSpacing.value > 0f, "tracking de ${style.letterSpacing}")
        }
    }

    @Test
    fun `todos los roles ganan interlinea, en proporcion al tamano`() {
        easier.allStyles().forEach { style ->
            val ratio = style.lineHeight.value / style.fontSize.value
            assertTrue(ratio > MIN_LINE_HEIGHT_RATIO, "interlínea de solo ${ratio}x el tamaño")
        }
    }

    @Test
    fun `el valor de un codigo sigue siendo monoespaciado`() {
        // La monoespaciada no se negocia ni en este modo: el valor de un código se coteja carácter a
        // carácter contra una etiqueta impresa, y en proporcional `1`, `l` e `I` se parecen. Este es
        // el test que impide "mejorar" la legibilidad haciendo el dato menos legible como dato.
        val code = codeValueStyle(easierReading = true)

        assertEquals(FontFamily.Monospace, code.fontFamily)
        assertTrue(code.fontSize.value > CodeValueStyle.fontSize.value)
        assertTrue(code.letterSpacing.value > CodeValueStyle.letterSpacing.value)
    }

    @Test
    fun `la escala ajustada no lleva serifas`() {
        easier.allStyles().forEach { style ->
            assertEquals(FontFamily.SansSerif, style.fontFamily)
        }
    }

    private companion object {
        /** Por debajo de esto la interlínea deja de dar el aire que este modo existe para dar. */
        const val MIN_LINE_HEIGHT_RATIO = 1.5f
    }
}
