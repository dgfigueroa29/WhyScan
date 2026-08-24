package com.whyscan.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorre la app en un emulador y anota qué métodos se ejecutan.
 *
 * ## Qué es esto y qué no
 *
 * No es un test: no afirma nada y no puede fallar por lo que la app haga. Es una **grabación**. Lo
 * que sale es una lista de clases y métodos que ART compilará al instalar en vez de interpretar la
 * primera vez, y que Play distribuye dentro del AAB. Sirve exactamente para lo que su nombre dice:
 * que el primer arranque no sea el más lento.
 *
 * Por eso las dos grabaciones de abajo no comprueban nada más allá de llegar a cada pantalla — si un
 * destino no se abre, `openDestination` no encuentra su etiqueta y la grabación de ese recorrido
 * queda coja, que es la señal que interesa. Lo que comprueba que el grafo resuelve y que la app
 * arranca son `AndroidKoinGraphTest` y `KoinGraphTest`, y esos corren sin emulador.
 *
 * ## Los dos recorridos
 *
 * - [startup] va marcado con `includeInStartupProfile`: además del baseline profile alimenta el
 *   *startup profile*, que AGP usa para reordenar el DEX y poner junto lo que se toca al abrir.
 * - [navigation] recorre las tres pantallas que ve un usuario normal. El comparador de motores no
 *   entra: vive detrás del modo avanzado y no es camino de nadie que abra la app a leer un código.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = APP_PACKAGE,
        includeInStartupProfile = true,
    ) {
        grantCameraPermission()
        pressHome()
        startActivityAndWait()
        waitForApp()
    }

    @Test
    fun navigation() = baselineProfileRule.collect(packageName = APP_PACKAGE) {
        grantCameraPermission()
        pressHome()
        startActivityAndWait()
        waitForApp()

        openDestination(HISTORY_LABEL)
        openDestination(SETTINGS_LABEL)
        openDestination(SCAN_LABEL)
    }
}

/**
 * Concede la cámara antes de abrir nada.
 *
 * Sin esto la primera pantalla saca el diálogo del sistema, que tapa la barra de navegación y deja
 * la grabación bloqueada esperando una etiqueta que no se ve. Además el camino que interesa grabar
 * es el de un usuario que ya dijo que sí: es el de todos los arranques menos el primero, y es el
 * que arranca la cámara de verdad.
 */
private fun MacrobenchmarkScope.grantCameraPermission() {
    device.executeShellCommand("pm grant $APP_PACKAGE android.permission.CAMERA")
}

/** Espera a que la barra inferior esté en pantalla: es lo que dice que la app ya compuso. */
private fun MacrobenchmarkScope.waitForApp() {
    device.wait(Until.hasObject(By.text(SCAN_LABEL)), TIMEOUT_MILLIS)
}

private fun MacrobenchmarkScope.openDestination(label: String) {
    val item = By.text(label)
    device.wait(Until.hasObject(item), TIMEOUT_MILLIS)
    device.findObject(item)?.click()
    device.waitForIdle()
}

private const val APP_PACKAGE = "com.whyscan.app"

// Las etiquetas de la barra inferior en inglés: la imagen del emulador arranca en en-US y el idioma
// de la app, sin preferencia guardada, sigue al del sistema. Si algún día cambian los textos de
// `composeApp/src/commonMain/composeResources/values/strings.xml`, esto deja de encontrar los
// destinos y la grabación se queda en el arranque.
private const val SCAN_LABEL = "Scan"
private const val HISTORY_LABEL = "History"
private const val SETTINGS_LABEL = "Settings"

private const val TIMEOUT_MILLIS = 5_000L
