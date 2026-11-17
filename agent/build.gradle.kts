plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.promode.agenticandroid.agent"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.promode.agenticandroid.agent"
        // The platform AppFunctionManager arrived in Android 16, API 36.
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
