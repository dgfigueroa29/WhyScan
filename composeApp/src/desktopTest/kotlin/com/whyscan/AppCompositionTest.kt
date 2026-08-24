package com.whyscan

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.whyscan.di.appModules
import com.whyscan.navigation.Destination
import com.whyscan.navigation.Navigator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Cierra la mitad de **D18** que quedaba abierta: nada componía la raíz.
 *
 * `KoinGraphTest` comprueba que el grafo **resuelva**, que es donde estaba el defecto original. Lo
 * que no comprobaba nadie es que `App()` se **componga**: un `CompositionLocal` que falta, un
 * `stringResource` cuya clave se borró, un `remember` que lanza. Todo eso compila, pasa lint, pasa
 * R8 y revienta al abrir la app — exactamente el modo de fallo que D18 describía, con otra causa.
 *
 * ## Por qué arranca en Ajustes y no en el escáner
 *
 * Porque el destino inicial de la app es el escáner, y montarlo aquí pediría cámara. No es que la
 * cámara no importe: es que lo que este test comprueba —que la raíz se compone, con su tema, su
 * idioma y su barra— no necesita hardware, y mezclarlo con algo que sí lo necesita convertiría un
 * test que corre en cada PR en uno que no corre nunca. El hueco de que ningún test lea un código
 * con una cámara de verdad sigue escrito en el ROADMAP, y sigue siendo un hueco.
 *
 * ## Qué se monta
 *
 * El grafo **real** de escritorio, no dobles. Un doble aquí comprobaría que Compose sabe pintar
 * dobles. Igual que en `KoinGraphTest`, resolver el historial crea `~/.whyscan` y no abre la base:
 * Room construye el archivo en la primera consulta y aquí no se hace ninguna.
 */
@OptIn(ExperimentalTestApi::class)
class AppCompositionTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `la raiz se compone y ofrece los destinos de la barra`() = runComposeUiTest {
        startKoin { modules(appModules()) }

        setContent {
            App(navigator = Navigator(initial = Destination.Settings))
        }

        // En inglés porque `values/` es el catálogo por defecto y el modo de idioma arranca en
        // "el del sistema", que en un runner de CI no es español.
        onNodeWithText("Scan").assertIsDisplayed()
        onNodeWithText("History").assertIsDisplayed()

        // Dos y no uno: el título de la barra superior y la pestaña de abajo. Que sean exactamente
        // dos dice además que la barra superior **sí** está en este destino — el escáner es el único
        // que no la lleva, y esa asimetría es fácil de romper sin enterarse.
        onAllNodesWithText("Settings").assertCountEquals(2)
    }

    @Test
    fun `sin modo avanzado el comparador no aparece en la barra`() = runComposeUiTest {
        // El modo avanzado está apagado por defecto, y esa decisión —que el producto que se abre por
        // primera vez sea un lector de códigos y no un banco de pruebas— se sostenía solo en
        // `destinationsFor`, sin nada que la comprobara sobre la barra de verdad.
        startKoin { modules(appModules()) }

        setContent {
            App(navigator = Navigator(initial = Destination.Settings))
        }

        onNodeWithText("Compare").assertDoesNotExist()
    }
}
