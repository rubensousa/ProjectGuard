plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.rubensousa.dependencyguard.android"
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
    testImplementation(libs.junit)
    androidTestImplementation(project(":legacy:a"))
}