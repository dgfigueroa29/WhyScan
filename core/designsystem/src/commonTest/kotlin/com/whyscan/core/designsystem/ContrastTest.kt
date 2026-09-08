package com.whyscan.core.designsystem

import ar.net.faro.foundation.Contrast
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pruebas de contraste sobre la paleta real de WhyScan.
 *
 * La aritmética vive en :core:foundation; aquí se comprueba que los valores elegidos para la marca
 * cumplen con los mínimos de accesibilidad RNF-05.
 */
class ContrastTest {

    @Test
    fun `toda_la_paleta_cumple_el_contraste_AA_para_texto_normal`() {
        // RNF-05. Incluye los pares que la UI usa de hecho —primary, tertiary y error como color
        // de texto sobre la tarjeta— y no solo los que Material garantiza por convención.
        val incumplen = ScannerPalette.measuredPairs()
            .map { it to Contrast.ratio(it.foreground, it.background) }
            .filter { (_, ratio) -> ratio < Contrast.AA_NORMAL_TEXT }
            .map { (pair, ratio) -> "${pair.name}: ${ratio.rounded()}" }

        assertTrue(incumplen.isEmpty(), "pares por debajo de AA (4.5:1):\n${incumplen.joinToString("\n")}")
    }

    @Test
    fun `los_componentes_no_textuales_cumplen_el_contraste_AA`() {
        // WCAG pide 3.0:1 para lo que transmite información sin ser texto. Aquí es `outline`: el
        // borde de un OutlinedButton o de un campo de texto. Sin este test, aclararlo "un poco para
        // que se vea más elegante" es un cambio de una cifra hexadecimal que nadie discute en
        // revisión y que deja el borde invisible para quien tiene baja visión.
        val incumplen = ScannerPalette.measuredNonTextPairs()
            .map { it to Contrast.ratio(it.foreground, it.background) }
            .filter { (_, ratio) -> ratio < Contrast.AA_LARGE_TEXT }
            .map { (pair, ratio) -> "${pair.name}: ${ratio.rounded()}" }

        assertTrue(incumplen.isEmpty(), "pares por debajo de 3.0:1:\n${incumplen.joinToString("\n")}")
    }

    @Test
    fun `los_dos_temas_miden_los_mismos_pares`() {
        // Un par que solo existe en claro es un par que nadie comprueba en oscuro. Es la forma
        // habitual de que el modo oscuro se degrade sin que nadie se entere.
        val (claros, oscuros) = ScannerPalette.measuredPairs().partition { it.name.startsWith("claro") }

        assertTrue(claros.isNotEmpty())
        assertTrue(
            claros.map { it.name.substringAfter(": ") } == oscuros.map { it.name.substringAfter(": ") },
            "los pares medidos en claro y en oscuro no coinciden",
        )
    }

    private fun Double.rounded(): String {
        val scaled = (this * 100).toInt()
        return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
    }
}
