import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// El juego entero (ADR 0001): motor, reglas, dibujo, sonido y pantallas, comunes a Android, iOS y el escritorio.
// Es también el framework que enlaza la app de iOS.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "com.fanguskdev.carofarty.shared")
        }
    }
    jvm()
    android {
        namespace = "com.fanguskdev.carofarty.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            // Skia de verdad en la JVM, para dibujar pantallas en PNG sin abrir ventanas (las capturas de las pruebas).
            implementation(compose.desktop.currentOs)
        }
    }
}

// El arte de arte/ convertido en las hojas que carga el juego (TEC-11, tools/arte.py) antes de preparar los recursos:
// cambiar un sprite y compilar basta para verlo. Si la entrega no cumple el formato, la compilación dice qué y dónde.
val generarArte = tasks.register<Exec>("generarArte") {
    val raiz = rootProject.layout.projectDirectory
    workingDir = raiz.asFile
    // The icon goes to androidApp and iosApp, which this task does not own: tools/arte.py installs it, the gate checks it.
    commandLine("python3", "tools/arte.py", "--sin-iconos")
    inputs.dir(raiz.dir("arte"))
    inputs.file(raiz.file("tools/arte.py"))
    outputs.dir(layout.projectDirectory.dir("src/commonMain/composeResources/files/arte"))
}
tasks.matching { it.name != "generarArte" && (it.name.contains("Resources") || it.name.contains("ResClass")) }.configureEach {
    dependsOn(generarArte)
}
