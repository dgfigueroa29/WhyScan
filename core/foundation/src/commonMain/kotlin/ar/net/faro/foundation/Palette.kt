package ar.net.faro.foundation

/**
 * Representación genérica de una paleta de colores de Material 3.
 *
 * Se usa para desacoplar la lógica de creación de un [ColorScheme] de los valores concretos de
 * una marca. La implementación vive en :core:foundation y no depende de Compose, operando sobre
 * enteros ARGB.
 */
public data class Palette(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val surfaceContainerLowest: Int,
    val surfaceContainerLow: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val outline: Int,
    val outlineVariant: Int,
    val scrim: Int,
    val inverseSurface: Int,
    val inverseOnSurface: Int,
    val inversePrimary: Int,
)
