plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.rubensousa.projectguard.android"
    compileSdk {
        version = release(36)
    }
    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain:a"))
    testImplementation(libs.junit)
    androidTestImplementation(project(":legacy:a"))
}