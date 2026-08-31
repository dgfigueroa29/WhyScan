package com.whyscan.core.domain.usecase

import com.whyscan.core.domain.FakeScannerEngine
import com.whyscan.core.domain.FakeScannerEngineRepository
import com.whyscan.core.domain.model.RejectionReason
import com.whyscan.core.model.BarcodeFormat
import com.whyscan.core.model.Permission
import com.whyscan.core.model.ScanRequest
import com.whyscan.core.model.ScanSource
import com.whyscan.core.model.ScannerEngineId
import com.whyscan.core.model.ScannerPlatform
import com.whyscan.core.scanner.EngineAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectScannerEngineUseCaseTest {

    // `select` es lógica pura: el repositorio solo hace falta para la sobrecarga `invoke`.
    private val useCase = SelectScannerEngineUseCase(FakeScannerEngineRepository())

    @Test
    fun `ordena por la prioridad de la plataforma`() {
        val catalog = listOf(
            FakeScannerEngine(ScannerEngineId.ManualInput).status(),
            FakeScannerEngine(ScannerEngineId.MlKitCameraX).status(),
            FakeScannerEngine(ScannerEngineId.GmsCodeScanner).status(),
        )

        val selection = useCase.select(
            catalog = catalog,
            request = ScanRequest(),
            preferredEngineId = null,
            platform = ScannerPlatform.Android,
        )

        assertEquals(
            listOf(
                ScannerEngineId.GmsCodeScanner,
                ScannerEngineId.MlKitCameraX,
                ScannerEngineId.ManualInput,
            ),
            selection.chain,
        )
    }

    @Test
    fun `el motor elegido por el usuario encabeza la cadena sin perder los fallbacks`() {
        val catalog = listOf(
            FakeScannerEngine(ScannerEngineId.GmsCodeScanner).status(),
            FakeScannerEngine(ScannerEngineId.MlKitCameraX).status(),
            FakeScannerEngine(ScannerEngineId.ManualInput).status(),
        )

        val selection = useCase.select(
            catalog = catalog,
            request = ScanRequest(),
            preferredEngineId = ScannerEngineId.ManualInput,
            platform = ScannerPlatform.Android,
        )

        assertEquals(ScannerEngineId.ManualInput, selection.preferred)
        assertTrue(selection.hasFallback)
        assertEquals(
            listOf(ScannerEngineId.GmsCodeScanner, ScannerEngineId.MlKitCameraX),
            selection.chain.drop(1),
        )
    }

    @Test
    fun `descarta motores no disponibles y explica el motivo`() {
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.GmsCodeScanner,
                availability = EngineAvailability.RequiresPermission(Permission.Camera),
            ).status(),
            FakeScannerEngine(ScannerEngineId.ManualInput).status(),
        )

        val selection = useCase.select(catalog, ScanRequest(), null, ScannerPlatform.Android)

        assertEquals(listOf(ScannerEngineId.ManualInput), selection.chain)
        val rejection = selection.rejected.single()
        assertEquals(ScannerEngineId.GmsCodeScanner, rejection.id)
        assertTrue(rejection.reason is RejectionReason.NotAvailable)
    }

    @Test
    fun `descarta motores del catalogo que no estan instalados en esta plataforma`() {
        val catalog = listOf(
            FakeScannerEngine(ScannerEngineId.VisionIos).status(installed = false),
            FakeScannerEngine(ScannerEngineId.ManualInput).status(),
        )

        val selection = useCase.select(catalog, ScanRequest(), null, ScannerPlatform.Android)

        assertEquals(listOf(ScannerEngineId.ManualInput), selection.chain)
    }

    @Test
    fun `el modo continuo descarta a los motores que no lo soportan`() {
        // Es el caso real del Google Code Scanner: abre su UI, devuelve un código y se cierra.
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.GmsCodeScanner,
                capabilities = FakeScannerEngine.defaultCapabilities(continuous = false),
            ).status(),
            FakeScannerEngine(ScannerEngineId.MlKitCameraX).status(),
        )

        val selection = useCase.select(
            catalog = catalog,
            request = ScanRequest(continuous = true),
            preferredEngineId = null,
            platform = ScannerPlatform.Android,
        )

        assertEquals(listOf(ScannerEngineId.MlKitCameraX), selection.chain)
        val reason = selection.rejected.single().reason
        assertTrue(reason is RejectionReason.DoesNotSatisfyRequest)
        assertTrue("escaneo continuo" in reason.missingCapabilities)
    }

    @Test
    fun `escanear desde imagen descarta a los motores que solo leen de camara`() {
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.GmsCodeScanner,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    sources = setOf(ScanSource.LiveCamera),
                ),
            ).status(),
            FakeScannerEngine(
                id = ScannerEngineId.ZXingCpp,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    sources = setOf(ScanSource.LiveCamera, ScanSource.StaticImage),
                ),
            ).status(),
        )

        val selection = useCase.select(
            catalog = catalog,
            request = ScanRequest(source = ScanSource.StaticImage),
            preferredEngineId = null,
            platform = ScannerPlatform.Android,
        )

        assertEquals(listOf(ScannerEngineId.ZXingCpp), selection.chain)
    }

    @Test
    fun `a igualdad de prioridad gana el que cubre mas formatos`() {
        val request = ScanRequest(formats = setOf(BarcodeFormat.QrCode, BarcodeFormat.Ean13))
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.MlKitOcr,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    formats = setOf(BarcodeFormat.Ean13),
                ),
            ).status(),
            FakeScannerEngine(
                id = ScannerEngineId.BrowserDetector,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    formats = setOf(BarcodeFormat.QrCode, BarcodeFormat.Ean13),
                ),
            ).status(),
        )

        // En Desktop ninguno de los dos está en la tabla de prioridad: desempata la cobertura.
        val selection = useCase.select(catalog, request, null, ScannerPlatform.Desktop)

        assertEquals(ScannerEngineId.BrowserDetector, selection.preferred)
    }

    @Test
    fun `un motor preferido pero no elegible no se promueve`() {
        val catalog = listOf(
            FakeScannerEngine(ScannerEngineId.ManualInput).status(),
            FakeScannerEngine(
                id = ScannerEngineId.GmsCodeScanner,
                availability = EngineAvailability.NotImplemented(plannedPhase = 2),
            ).status(),
        )

        val selection = useCase.select(
            catalog = catalog,
            request = ScanRequest(),
            preferredEngineId = ScannerEngineId.GmsCodeScanner,
            platform = ScannerPlatform.Android,
        )

        assertEquals(listOf(ScannerEngineId.ManualInput), selection.chain)
    }

    @Test
    fun `en escritorio ZXing encabeza al pedir imagen y la camara cae a la entrada manual`() {
        // Es el motor de escritorio (D13) y solo decodifica archivos. Que la prioridad lo ponga
        // primero no puede colarlo en una sesión de cámara: ahí no hay nada que pueda hacer, y una
        // cadena que empieza por un motor inútil deja al usuario mirando una pantalla vacía.
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.ZXingJava,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    sources = setOf(ScanSource.StaticImage),
                    continuous = false,
                    torch = false,
                ),
                platforms = setOf(ScannerPlatform.Desktop),
            ).status(),
            FakeScannerEngine(
                id = ScannerEngineId.ManualInput,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    sources = setOf(ScanSource.ManualInput),
                    torch = false,
                ),
                platforms = setOf(ScannerPlatform.Desktop),
            ).status(),
        )

        val fromImage = useCase.select(
            catalog = catalog,
            request = ScanRequest(source = ScanSource.StaticImage),
            preferredEngineId = null,
            platform = ScannerPlatform.Desktop,
        )
        val fromCamera = useCase.select(
            catalog = catalog,
            request = ScanRequest(source = ScanSource.LiveCamera),
            preferredEngineId = null,
            platform = ScannerPlatform.Desktop,
        )

        assertEquals(listOf(ScannerEngineId.ZXingJava), fromImage.chain)

        // Antes esto afirmaba `fromCamera.chain.isEmpty()`, y esa cadena vacía era el estado "no se
        // puede escanear" que el objetivo G4 dice que no existe. En escritorio no hay captura de
        // webcam, así que **toda** petición de cámara caía ahí.
        assertEquals(listOf(ScannerEngineId.ManualInput), fromCamera.chain)

        // Los descartes son los de la primera pasada, que son los que explican por qué no hubo
        // cámara. Si fueran los de la segunda hablarían de una petición que el usuario no hizo.
        //
        // La entrada manual **no** está entre ellos, aunque la primera pasada la descartara por la
        // fuente: es la que acabó atendiendo, y no puede ser a la vez la respuesta y el motivo del
        // fallo. Esta línea es la que se cayó al implementar G4, por esperar solo lo primero.
        assertEquals(listOf(ScannerEngineId.ZXingJava), fromCamera.rejected.map { it.id })
    }

    @Test
    fun `sin motores elegibles y pidiendo camara, la entrada manual cierra la cadena`() {
        // El motor manual está descartado por disponibilidad, no por la fuente: aquí no hay nada
        // que sustituir y la cadena se queda vacía de verdad. Es el único caso en el que debe.
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.ManualInput,
                availability = EngineAvailability.Unsupported("test"),
            ).status(),
        )

        val selection = useCase.select(catalog, ScanRequest(), null, ScannerPlatform.Web)

        assertTrue(selection.chain.isEmpty())
        assertEquals(1, selection.rejected.size)
    }

    @Test
    fun `una imagen que nadie puede decodificar sigue fallando, no cae a manual`() {
        // La sustitución es **solo** para `LiveCamera`, y este test es lo que impide que alguien la
        // simplifique a "si la cadena está vacía, manual". El usuario eligió una foto: ofrecerle un
        // teclado no es un respaldo, es cambiarle de tema. `DecodeImageUseCase` llama a este mismo
        // `select`, así que la confusión no sería teórica.
        val catalog = listOf(
            FakeScannerEngine(
                id = ScannerEngineId.ManualInput,
                capabilities = FakeScannerEngine.defaultCapabilities(
                    sources = setOf(ScanSource.ManualInput),
                    torch = false,
                ),
            ).status(),
        )

        val selection = useCase.select(
            catalog = catalog,
            request = ScanRequest(source = ScanSource.StaticImage),
            preferredEngineId = null,
            platform = ScannerPlatform.Desktop,
        )

        assertTrue(selection.chain.isEmpty())
    }
}
