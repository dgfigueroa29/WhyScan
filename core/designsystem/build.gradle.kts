plugins {
    id("whyscan.kmp.compose")
}

android {
    namespace = "com.whyscan.core.designsystem"
}

kotlin {
    explicitApi()

    sourceSets {
        commonMain.dependencies {
            // :core:foundation aporta Contrast, AppLanguage y LocalSnackbarHostState.
            // Se expone como api() para que los consumers de :core:designsystem no necesiten
            // declarar la dependencia por separado mientras foundation no se publique aparte.
            api(project(":core:foundation"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            // Explícita y no heredada de `foundation`: las pantallas usan
            // `AnimatedVisibility` y `animateContentSize`, y depender de que otro artefacto
            // las arrastre es depender de un detalle de empaquetado ajeno.
            api(libs.compose.animation)
            api(libs.compose.material3)
            api(libs.compose.material.icons.extended)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
        }
    }
}
