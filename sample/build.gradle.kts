// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.projectguard) apply true
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
}

projectGuard {
    guard(":data") {
        deny(":legacy")
    }
    restrictModule(":android") {
        // Test dependencies are fine
        allow(libs.junit)
    }
    restrictModule(":domain") {
        reason("Domain modules should not depend on other modules")

        // Domain modules can only depend on other domain modules
        allow(":domain")

        // Test dependencies are fine
        allow(libs.junit)
    }
    restrictDependency(":legacy") {
        reason("Legacy modules should no longer be used")

        // Only legacy modules can still depend on other legacy modules
        allow(":legacy")
    }
    restrictDependency(libs.mockk) {
        reason("Fakes should be used instead")

        // This feature requires mockk to test platform code
        allow(":feature:a")
    }
}
