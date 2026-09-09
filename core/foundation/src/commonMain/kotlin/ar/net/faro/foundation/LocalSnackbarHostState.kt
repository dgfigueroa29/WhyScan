package ar.net.faro.foundation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal que transporta el [SnackbarHostState] del Scaffold raíz a cualquier punto del
 * árbol de composición sin pasarlo como parámetro en cada nivel.
 *
 * Sin este valor en el árbol, cualquier intento de mostrar un snackbar lanzará una excepción con el
 * mensaje de error de abajo. En la práctica, el Scaffold de `App()` en `:composeApp` es quien lo
 * provee.
 */
public val LocalSnackbarHostState: androidx.compose.runtime.ProvidableCompositionLocal<SnackbarHostState> =
    staticCompositionLocalOf {
        error("No hay SnackbarHostState en el árbol: falta envolver la UI en el Scaffold de App()")
    }
