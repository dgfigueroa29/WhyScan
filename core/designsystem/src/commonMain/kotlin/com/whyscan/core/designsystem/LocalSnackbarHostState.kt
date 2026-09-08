package com.whyscan.core.designsystem

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.ProvidableCompositionLocal
import ar.net.faro.foundation.LocalSnackbarHostState

/**
 * Reexportación de compatibilidad — la implementación vive en :core:foundation.
 */
@Deprecated(
    message = "Usar ar.net.faro.foundation.LocalSnackbarHostState directamente.",
    replaceWith = ReplaceWith("LocalSnackbarHostState", "ar.net.faro.foundation.LocalSnackbarHostState"),
    level = DeprecationLevel.WARNING,
)
public val LocalSnackbarHostState: ProvidableCompositionLocal<SnackbarHostState> = LocalSnackbarHostState
