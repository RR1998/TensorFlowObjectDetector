plugins {
    alias(libs.plugins.android.library)
}

android {
    androidResources {
        noCompress("tflite")
    }

    namespace = "com.example.TensorFlowObjectDetector.tensorflow.processing"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.litert)
}
