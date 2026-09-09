package ar.net.faro.foundation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {

    @Test
    fun negro_sobre_blanco_da_el_maximo_de_la_escala() {
        // Ancla la fórmula contra un valor conocido: si la linealización de canal se rompe, esto
        // deja de dar 21 y el resto de asertos dejarían de significar nada.
        val ratio = Contrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

        assertTrue(abs(ratio - 21.0) < 0.01, "negro sobre blanco dio $ratio en vez de 21")
    }

    @Test
    fun un_color_contra_si_mismo_no_tiene_contraste() {
        assertTrue(abs(Contrast.ratio(0xFF07704E.toInt(), 0xFF07704E.toInt()) - 1.0) < 0.001)
    }

    @Test
    fun el_orden_de_los_colores_no_cambia_el_resultado() {
        // La razón se define entre el más claro y el más oscuro, no entre texto y fondo: si
        // dependiera del orden, medir un texto claro sobre fondo oscuro daría otro número.
        val directo = Contrast.ratio(0xFF07704E.toInt(), 0xFFFFFFFF.toInt())
        val inverso = Contrast.ratio(0xFFFFFFFF.toInt(), 0xFF07704E.toInt())

        assertTrue(abs(directo - inverso) < 0.0001)
    }
}
