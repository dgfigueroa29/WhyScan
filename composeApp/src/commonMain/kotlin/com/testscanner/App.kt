package com.testscanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.testscanner.core.designsystem.LocalSnackbarHostState
import com.testscanner.core.designsystem.ProvideAppLanguage
import com.testscanner.core.designsystem.ScanlyTheme
import com.testscanner.core.domain.repository.AppPreferences
import com.testscanner.core.domain.repository.AppPreferencesRepository
import com.testscanner.feature.history.HistoryScreen
import com.testscanner.feature.scanner.ScannerScreen
import com.testscanner.feature.scanner.comparison.ComparisonScreen
import com.testscanner.feature.settings.SettingsScreen
import com.testscanner.navigation.Destination
import com.testscanner.navigation.Navigator
import com.testscanner.resources.Res
import com.testscanner.resources.destination_comparison
import com.testscanner.resources.destination_history
import com.testscanner.resources.destination_scanner
import com.testscanner.resources.destination_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Raíz de la app, compartida por Android, iOS, Desktop y Web.
 *
 * No arranca Koin: eso lo hace `initKoin()` desde cada punto de entrada, porque Android necesita
 * entregar su `Context` antes de que exista cualquier composable. Aquí solo se consume el grafo ya
 * montado.
 *
 * ## Por qué ya no hay `KoinContext { }` (deuda D20)
 *
 * Lo había, envolviendo todo lo de abajo, y llevaba tiempo avisando de que sobraba:
 *
 *     w: 'KoinContext' is deprecated. KoinContext is not needed anymore. This can be removed.
 *        Compose Koin context is setup with StartKoin()
 *
 * El aviso dice la verdad y se puede comprobar leyendo koin-compose: `koinInject` y `koinViewModel`
 * resuelven contra `LocalKoinScopeContext`, y ese `CompositionLocal` se declara con un **valor por
 * defecto** que es `KoinPlatform.getKoin().scopeRegistry.rootScope` — exactamente el mismo scope que
 * `KoinContext` proveía a mano. Sin proveedor, se cae en el valor por defecto y sale el mismo objeto;
 * con proveedor, se provee el mismo objeto. El envoltorio era una identidad.
 *
 * Lo que hacía que esto no se pudiera cerrar es que el valor por defecto se calcula **la primera vez
 * que alguien lo consume**, y eso solo ocurre componiendo. Ya no hace falta un dispositivo para
 * verlo: `ComposeKoinContextTest` compone de verdad —con el runtime de Compose y sin UI— y comprueba
 * que `koinInject` devuelve la misma instancia que `koin.get()`.
 *
 * El [Navigator] se recibe por parámetro para que Android pueda cederle el botón atrás del sistema
 * y para que la navegación sea testeable sin Compose (ADR-0005).
 *
 * @param onDarkThemeResolved lo llama la app cada vez que cambia el claro/oscuro **ya resuelto**.
 *   Existe porque las barras del sistema no las pinta Compose: en Android, con `enableEdgeToEdge`,
 *   los iconos de la barra de estado siguen al tema del *sistema*, así que un usuario con el
 *   teléfono en claro y la app forzada a oscuro se quedaba con iconos oscuros sobre fondo oscuro.
 *   Las plataformas que no tienen barras que ajustar no pasan nada y no se enteran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    navigator: Navigator = remember { Navigator() },
    onDarkThemeResolved: (Boolean) -> Unit = {},
) {
    val preferencesRepository = koinInject<AppPreferencesRepository>()
    val preferences by preferencesRepository.observePreferences()
        .collectAsStateWithLifecycle(AppPreferences())

    val darkTheme = preferences.themeMode.isDark(isSystemInDarkTheme())
    LaunchedEffect(darkTheme) { onDarkThemeResolved(darkTheme) }

    // El idioma envuelve al tema y no al revés: cambiar de idioma recompone el subárbol entero
    // (ver `ProvideAppLanguage`), y no hay motivo para volver a construir el `ColorScheme` por
    // eso. Al revés sí lo habría.
    ProvideAppLanguage(preferences.language.tag) {
        ScanlyTheme(darkTheme = darkTheme) {
            AppScaffold(navigator = navigator, advancedMode = preferences.advancedMode)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(navigator: Navigator, advancedMode: Boolean) {
    val backstack by navigator.backstack.collectAsStateWithLifecycle()
    val destinations = destinationsFor(advancedMode)
    val current = backstack.last()
    val snackbarHostState = remember { SnackbarHostState() }

    // Apagar el modo avanzado con el comparador en pantalla dejaba al usuario en un destino que ya
    // no aparece en ninguna barra. Se poda el backstack entero y no solo la pantalla actual: si el
    // comparador quedara enterrado más abajo, el botón atrás acabaría volviendo a él.
    LaunchedEffect(destinations) { navigator.pruneTo(destinations) }

    Scaffold(
        topBar = {
            // El escáner no lleva barra de título, y no es por ahorrar píxeles: una barra que dice
            // "Escanear" encima de un visor de cámara no añade ninguna información que el visor no
            // esté dando ya, y se come la altura que necesita lo único que importa en esa pantalla.
            // El ítem activo de la barra inferior dice dónde está el usuario.
            //
            // El `Scaffold` sigue aportando los insets de la barra de estado en su `padding`, así
            // que quitarla no mete la cámara debajo del reloj.
            if (current != Destination.Scanner) {
                TopAppBar(
                    title = { Text(current.title()) },
                    // El contenedor de la barra iguala al del `NavigationBar` de abajo: con el color
                    // por defecto, la superior salía del tono de `surface` y la inferior de
                    // `surfaceContainer`, y la pantalla quedaba enmarcada por dos grises distintos.
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination,
                        onClick = { navigator.navigateTo(destination) },
                        // El icono estaba vacío (`icon = {}`), que es lo que dejaba la barra como
                        // una fila de etiquetas sueltas sin el indicador de píldora de Material 3:
                        // ese indicador se dibuja **alrededor del icono**, así que sin icono no
                        // había nada que resaltara el destino activo.
                        icon = {
                            Icon(
                                imageVector = destination.icon(),
                                // La etiqueta va justo debajo y dice lo mismo. Describir también el
                                // icono haría que el lector de pantalla leyera cada ítem dos veces.
                                contentDescription = null,
                            )
                        },
                        label = { Text(destination.title()) },
                    )
                }
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            // Cambiar de destino era un corte seco: un fotograma con la pantalla vieja y el
            // siguiente con la nueva. `AnimatedContent` con el *fade through* de Material 3 —la
            // saliente se desvanece y solo entonces entra la nueva, creciendo un pelo— da la
            // continuidad que faltaba sin sugerir una dirección que aquí no existe: la barra
            // inferior no es una pila, así que deslizar de lado contaría una jerarquía falsa.
            //
            // Efecto colateral que conviene saber: durante los ~300 ms de la transición conviven
            // las dos pantallas en la composición. El escáner apaga su sesión al salir de ella, así
            // que la cámara sigue viva ese instante de más. Es el mismo apagado de siempre, un poco
            // más tarde.
            AnimatedContent(
                targetState = current,
                transitionSpec = { fadeThrough() },
                modifier = Modifier.fillMaxSize().padding(padding),
                label = "destination",
            ) { destination ->
                when (destination) {
                    Destination.Scanner -> ScannerScreen(advancedMode = advancedMode)
                    Destination.Comparison -> ComparisonScreen()
                    Destination.History -> HistoryScreen(advancedMode = advancedMode)
                    Destination.Settings -> SettingsScreen()
                }
            }
        }
    }
}

/**
 * *Fade through* de Material 3: la pantalla saliente se desvanece primero y la entrante aparece
 * después, con un crecimiento mínimo que sugiere que llega desde el fondo.
 *
 * Los tiempos no son inventados, son los de la especificación de movimiento de Material 3, y el
 * solape es justo lo que la hace legible: la entrada espera a que termine la salida (`delayMillis`),
 * así que en ningún momento se ven las dos pantallas a media opacidad una encima de otra.
 *
 * `SizeTransform(clip = false)`: sin esto, `AnimatedContent` animaría también el tamaño del
 * contenedor y recortaría lo que sobresale. Las cuatro pantallas ocupan lo mismo —todo el hueco del
 * `Scaffold`—, así que no hay tamaño que animar y sí hay contenido que no conviene recortar.
 */
private fun fadeThrough(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(
        animationSpec = tween(durationMillis = ENTER_MILLIS, delayMillis = EXIT_MILLIS),
    ) + scaleIn(
        animationSpec = tween(durationMillis = ENTER_MILLIS, delayMillis = EXIT_MILLIS),
        initialScale = ENTER_SCALE,
    ),
    initialContentExit = fadeOut(animationSpec = tween(durationMillis = EXIT_MILLIS)),
    sizeTransform = SizeTransform(clip = false),
)

private const val ENTER_MILLIS = 210
private const val EXIT_MILLIS = 90
private const val ENTER_SCALE = 0.92f

/**
 * Qué destinos se ofrecen.
 *
 * Comparar motores solo aparece en modo avanzado: es la pantalla más específica de todas —un banco
 * de pruebas dentro del producto— y ocupaba un cuarto de la barra de navegación para alguien que
 * abre la app a leer un QR.
 */
private fun destinationsFor(advancedMode: Boolean): List<Destination> =
    Destination.all.filter { it != Destination.Comparison || advancedMode }

@Composable
private fun Destination.title(): String = when (this) {
    Destination.Scanner -> stringResource(Res.string.destination_scanner)
    Destination.Comparison -> stringResource(Res.string.destination_comparison)
    Destination.History -> stringResource(Res.string.destination_history)
    Destination.Settings -> stringResource(Res.string.destination_settings)
}

private fun Destination.icon(): ImageVector = when (this) {
    Destination.Scanner -> Icons.Filled.QrCodeScanner
    Destination.Comparison -> Icons.Filled.Speed
    Destination.History -> Icons.Filled.History
    Destination.Settings -> Icons.Filled.Settings
}
