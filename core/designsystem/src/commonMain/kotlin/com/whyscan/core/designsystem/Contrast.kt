@file:Suppress("unused")
package com.whyscan.core.designsystem

/**
 * Reexportación de compatibilidad — la implementación vive en :core:foundation.
 *
 * Cualquier consumer de :core:designsystem que importe `com.whyscan.core.designsystem.Contrast`
 * sigue compilando sin cambios. Migrar el import a `ar.net.faro.foundation.Contrast` es opcional
 * y se hará cuando el consumer adopte :core:foundation directamente.
 */
@Deprecated(
    message = "Usar ar.net.faro.foundation.Contrast directamente.",
    replaceWith = ReplaceWith("Contrast", "ar.net.faro.foundation.Contrast"),
    level = DeprecationLevel.WARNING,
)
public typealias Contrast = ar.net.faro.foundation.Contrast
