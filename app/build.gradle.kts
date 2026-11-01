plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.promode.agenticandroid.shopping"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.promode.agenticandroid.shopping"
        // AppFunctionService (platform and Jetpack) requires Android 16, API 36.
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)

    // AppFunctions: the runtime plus the KSP compiler that turns @AppFunctionDeclaration
    // into the generated service and the XML the system indexes.
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)

    debugImplementation(libs.androidx.ui.tooling)
}
