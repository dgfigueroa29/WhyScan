plugins {
    id("whyscan.kmp.compose")

    // Ronda 13: el suelo de cobertura cubría `:core:domain` y `:core:data`, y los cuatro
    // ViewModels —donde vive la máquina de estados— quedaban fuera. Tenían tests, así que el
    // problema no era la falta de red: era que **nadie sabía el número**, que es exactamente el
    // reproche con el que se abrió la medición en la Ronda 5, un módulo más allá.
    alias(libs.plugins.kover)
}

android {
    namespace = "com.whyscan.feature.settings"
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.whyscan.feature.settings.resources"
    generateResClass = always
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))

            // Abrir la política de privacidad y los términos de uso desde "Acerca de". Es la
            // primera vez que esta pantalla necesita algo del sistema: hasta ahora todo lo que
            // hacía se quedaba en sus propias preferencias.
            implementation(project(":core:platform"))

            api(project(":core:designsystem"))

            implementation(libs.compose.components.resources)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
