plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.TensorFlowObjectDetector.domain.detection"
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
    implementation(libs.litert)
}
