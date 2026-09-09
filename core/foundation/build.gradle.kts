/*
 * :core:foundation — base sin marca, publicable bajo ar.net.faro.foundation.
 *
 * Regla de hierro: este módulo no puede depender de :core:designsystem ni de ningún módulo que
 * lleve la marca WhyScan. Cualquier import de ScannerPalette, BrandMark o WhyScanTheme aquí es un
 * defecto de arquitectura — check_design_system() en tools/checks.py lo detecta.
 *
 * Targetea las mismas plataformas que :core:designsystem para que AppLanguage.kt y sus cuatro
 * actuals (Android, iOS, JVM, wasmJs) compilen sin cambios de estructura.
 *
 * Por ahora se consume solo desde :core:designsystem. La publicación Maven (ar.net.faro:foundation)
 * y el binary-compatibility validator llegan en tasks 3 y 5 del openspec federate-design-system.
 */
plugins {
    id("whyscan.kmp.compose")
}

android {
    namespace = "ar.net.faro.foundation"
}

kotlin {
    explicitApi()

    sourceSets {
        commonMain.dependencies {
            // Compose runtime necesario para @Composable, staticCompositionLocalOf y key().
            api(libs.compose.runtime)
            // Material3 necesario para SnackbarHostState en LocalSnackbarHostState.
            api(libs.compose.material3)
        }
    }
}
