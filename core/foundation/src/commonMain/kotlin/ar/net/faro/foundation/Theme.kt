package ar.net.faro.foundation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Crea un [ColorScheme] de Material 3 a partir de una [Palette].
 *
 * @param palette los colores de la marca.
 * @param isDark si se debe crear un esquema oscuro o claro.
 */
public fun colorSchemeFrom(palette: Palette, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = Color(palette.primary),
            onPrimary = Color(palette.onPrimary),
            primaryContainer = Color(palette.primaryContainer),
            onPrimaryContainer = Color(palette.onPrimaryContainer),
            secondary = Color(palette.secondary),
            onSecondary = Color(palette.onSecondary),
            secondaryContainer = Color(palette.secondaryContainer),
            onSecondaryContainer = Color(palette.onSecondaryContainer),
            tertiary = Color(palette.tertiary),
            onTertiary = Color(palette.onTertiary),
            tertiaryContainer = Color(palette.tertiaryContainer),
            onTertiaryContainer = Color(palette.onTertiaryContainer),
            error = Color(palette.error),
            onError = Color(palette.onError),
            errorContainer = Color(palette.errorContainer),
            onErrorContainer = Color(palette.onErrorContainer),
            background = Color(palette.background),
            onBackground = Color(palette.onBackground),
            surface = Color(palette.surface),
            onSurface = Color(palette.onSurface),
            surfaceVariant = Color(palette.surfaceVariant),
            onSurfaceVariant = Color(palette.onSurfaceVariant),
            surfaceContainerLowest = Color(palette.surfaceContainerLowest),
            surfaceContainerLow = Color(palette.surfaceContainerLow),
            surfaceContainer = Color(palette.surfaceContainer),
            surfaceContainerHigh = Color(palette.surfaceContainerHigh),
            surfaceContainerHighest = Color(palette.surfaceContainerHighest),
            outline = Color(palette.outline),
            outlineVariant = Color(palette.outlineVariant),
            scrim = Color(palette.scrim),
            inverseSurface = Color(palette.inverseSurface),
            inverseOnSurface = Color(palette.inverseOnSurface),
            inversePrimary = Color(palette.inversePrimary),
            surfaceTint = Color(palette.primary),
        )
    } else {
        lightColorScheme(
            primary = Color(palette.primary),
            onPrimary = Color(palette.onPrimary),
            primaryContainer = Color(palette.primaryContainer),
            onPrimaryContainer = Color(palette.onPrimaryContainer),
            secondary = Color(palette.secondary),
            onSecondary = Color(palette.onSecondary),
            secondaryContainer = Color(palette.secondaryContainer),
            onSecondaryContainer = Color(palette.onSecondaryContainer),
            tertiary = Color(palette.tertiary),
            onTertiary = Color(palette.onTertiary),
            tertiaryContainer = Color(palette.tertiaryContainer),
            onTertiaryContainer = Color(palette.onTertiaryContainer),
            error = Color(palette.error),
            onError = Color(palette.onError),
            errorContainer = Color(palette.errorContainer),
            onErrorContainer = Color(palette.onErrorContainer),
            background = Color(palette.background),
            onBackground = Color(palette.onBackground),
            surface = Color(palette.surface),
            onSurface = Color(palette.onSurface),
            surfaceVariant = Color(palette.surfaceVariant),
            onSurfaceVariant = Color(palette.onSurfaceVariant),
            surfaceContainerLowest = Color(palette.surfaceContainerLowest),
            surfaceContainerLow = Color(palette.surfaceContainerLow),
            surfaceContainer = Color(palette.surfaceContainer),
            surfaceContainerHigh = Color(palette.surfaceContainerHigh),
            surfaceContainerHighest = Color(palette.surfaceContainerHighest),
            outline = Color(palette.outline),
            outlineVariant = Color(palette.outlineVariant),
            scrim = Color(palette.scrim),
            inverseSurface = Color(palette.inverseSurface),
            inverseOnSurface = Color(palette.inverseOnSurface),
            inversePrimary = Color(palette.inversePrimary),
            surfaceTint = Color(palette.primary),
        )
    }
}
