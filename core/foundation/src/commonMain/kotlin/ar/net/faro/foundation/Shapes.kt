package ar.net.faro.foundation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp

/**
 * Crea un objeto [Shapes] de Material 3 a partir de radios de curvatura.
 */
public fun shapesFrom(
    extraSmall: Dp,
    small: Dp,
    medium: Dp,
    large: Dp,
    extraLarge: Dp,
): Shapes = Shapes(
    extraSmall = RoundedCornerShape(extraSmall),
    small = RoundedCornerShape(small),
    medium = RoundedCornerShape(medium),
    large = RoundedCornerShape(large),
    extraLarge = RoundedCornerShape(extraLarge),
)
