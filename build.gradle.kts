// AGP 9 compiles Kotlin itself ("built-in Kotlin"). Pin the Kotlin Gradle Plugin it uses so it
// matches the Compose compiler plugin and KSP. See https://kotl.in/gradle/agp-built-in-kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
