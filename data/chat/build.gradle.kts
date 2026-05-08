plugins {
    alias(libs.plugins.android.library)
}

android {
    defaultConfig {
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "CLOUD_RUN_BASE_URL",
            "\"${project.findProperty("CLOUD_RUN_BASE_URL") ?: "https://example.com/"}\""
        )
    }
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.example.TensorFlowObjectDetector.data.chat"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain:detection"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
