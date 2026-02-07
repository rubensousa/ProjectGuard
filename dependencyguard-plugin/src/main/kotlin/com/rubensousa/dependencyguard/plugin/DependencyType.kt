package com.rubensousa.dependencyguard.plugin

enum class DependencyType(val id: String) {
    IMPLEMENTATION("implementation"),
    API("api"),
    TEST_IMPLEMENTATION("testImplementation"),
    ANDROID_TEST_IMPLEMENTATION("androidTestImplementation")
}
