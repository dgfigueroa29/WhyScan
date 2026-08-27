// El módulo del OCR: un motor por plataforma, un solo intérprete.
//
// `OcrCodeInterpreter` —la parte que decide si un número leído es realmente un código— vive en
// `commonMain` y no sabe nada de reconocedores. Encima de él hay dos motores distintos, y son
// distintos de verdad: ML Kit Text Recognition en Android y `VNRecognizeTextRequest` del framework
// Vision en iOS. Por eso tienen id propio en el catálogo, con el mismo criterio de D13 —zxing-java
// no se llama zxing-cpp— y no un `expect/actual` que los presentaría como un solo motor: lo que la
// app existe para comparar es precisamente qué lee cada uno.
//
// Ni Android ni iOS arrastran nada del otro: el `sourceSet` de cada plataforma trae su reconocedor,
// y quitar el OCR del producto sigue siendo borrar una línea de `settings.gradle.kts`.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    // Aporta su propia superficie de preview, así que necesita Compose. Ver ADR-0007.
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    androidTarget()

    // Sin `iosX64`: el simulador de los Mac con Intel, que Compose Multiplatform ya no publica.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":core:scanner-api"))
            api(project(":core:scanner-ui"))
        }
        androidMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
        }
        iosMain.dependencies {
            // Nada más: Vision, AVFoundation y Core Graphics vienen con el sistema. Es la razón de
            // que en iOS el OCR no tenga el coste de binario que tiene en Android.
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":core:scanner-testing"))
        }
    }
}

android {
    namespace = "com.whyscan.engines.ocr"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
