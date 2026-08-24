package com.whyscan.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

actual class DatabaseBuilderFactory {

    @OptIn(ExperimentalForeignApi::class)
    actual fun create(): RoomDatabase.Builder<ScanDatabase> {
        // Documents y no Caches: el historial es dato del usuario, no algo que el sistema pueda
        // borrar cuando le haga falta espacio.
        val documents: NSURL = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            ),
        ) { "No se pudo resolver el directorio Documents" }

        val file = documents.URLByAppendingPathComponent(ScanDatabase.FILE_NAME)
            ?: error("No se pudo componer la ruta de la base")

        excludeFromBackup(file)

        return Room.databaseBuilder(name = requireNotNull(file.path))
    }

    /**
     * Saca la base de la copia de iCloud.
     *
     * Es el equivalente de `allowBackup="false"` en Android, y aquí hace **más falta**: en Android
     * la copia se activa con una bandera visible en el manifiesto, mientras que en iOS todo lo que
     * está en `Documents` entra en la copia por defecto y no hay nada que lo delate al leer el
     * proyecto. El mismo agujero, sin el cartel.
     *
     * Importa porque el historial guarda el `rawValue` literal de cada código: un QR de WiFi es
     * `WIFI:T:WPA;S:red;P:clave;;`, con la contraseña dentro. La app no habla con ninguna red, y esa
     * garantía se le cuenta al usuario en Ajustes; la copia del sistema la rompía sin pasar por
     * ella.
     *
     * Se llama en cada arranque y no una sola vez, y no es descuido. El atributo se pone **sobre el
     * archivo**, no se hereda del directorio, y en el primer arranque el archivo todavía no existe
     * porque lo crea Room justo después: esa primera llamada no marca nada. Repetirla en cada
     * arranque es lo que hace que quede marcado a partir del segundo, y cuesta una llamada.
     *
     * Un fallo aquí no debe impedir abrir la app: se prefiere un historial que funciona y acaba en
     * la copia a una app que no arranca. Es la misma clase de decisión que el `runCatching` de las
     * acciones de plataforma.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun excludeFromBackup(file: NSURL) {
        runCatching {
            file.setResourceValue(
                value = NSNumber(bool = true),
                forKey = NSURLIsExcludedFromBackupKey,
                error = null,
            )
        }
    }
}

/**
 * Ver la nota en `commonMain`. Aquí `Dispatchers.IO` sí resuelve, pero **solo con el import de
 * arriba**: en Kotlin/Native `IO` no es miembro de `Dispatchers` —ese es `internal`— sino una
 * extensión declarada en `concurrentMain`, y una extensión no viaja con el import del receptor.
 *
 * `import kotlinx.coroutines.IO` parece un import sin usar y no lo es: quitarlo devuelve el
 * `Cannot access 'val IO': it is internal` que tumbó el job de iOS. Mover la declaración de
 * `commonMain` a cada plataforma era necesario pero no suficiente.
 */
internal actual val queryDispatcher: CoroutineDispatcher = Dispatchers.IO
