package ar.net.faro.foundation

import kotlin.math.pow

/**
 * Aritmética de contraste WCAG 2.1.
 *
 * Sin dependencias de Compose ni de la marca WhyScan: opera sobre enteros ARGB del formato
 * `0xAARRGGBB` y devuelve doubles. Apto para tests JVM puros sin runtime de UI.
 */
public object Contrast {

    public const val AA_NORMAL_TEXT: Double = 4.5
    public const val AA_LARGE_TEXT: Double = 3.0

    public fun ratio(foreground: Int, background: Int): Double {
        val first = relativeLuminance(foreground)
        val second = relativeLuminance(background)
        val lighter = maxOf(first, second)
        val darker = minOf(first, second)
        return (lighter + OFFSET) / (darker + OFFSET)
    }

    public fun relativeLuminance(color: Int): Double {
        val red = channel((color shr RED_SHIFT) and BYTE)
        val green = channel((color shr GREEN_SHIFT) and BYTE)
        val blue = channel(color and BYTE)
        return RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue
    }

    private fun channel(value: Int): Double {
        val normalized = value / MAX_CHANNEL
        return if (normalized <= LINEAR_THRESHOLD) {
            normalized / LINEAR_DIVISOR
        } else {
            ((normalized + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
        }
    }

    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BYTE = 0xFF
    private const val MAX_CHANNEL = 255.0
    private const val OFFSET = 0.05
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val LINEAR_THRESHOLD = 0.03928
    private const val LINEAR_DIVISOR = 12.92
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_DIVISOR = 1.055
    private const val GAMMA = 2.4
}
