import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    // La puerta de calidad (docs/proceso.md): ktlint lee el código que escribe una persona (lo generado queda fuera por
    // .editorconfig) y cualquier aviso del compilador de Kotlin, en cualquier plataforma, rompe la construcción.
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}
