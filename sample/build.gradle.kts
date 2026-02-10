// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dependencyguard) apply true
}
val dependencyGuardPlugin = libs.plugins.dependencyguard.get().pluginId

dependencyGuard {
    guard(":domain") {
        deny(":feature") {
            reason("Dependency should be inverted. Feature depends on domain")
        }
        deny(":data") {
            reason("Dependency should be inverted. Data depends on domain")
        }
    }
    guard(":feature") {
        deny(":feature") {
            reason("Features should not depend on other features directly")
        }
    }
    restrictDependency(":legacy") {
        reason("Legacy modules should no longer be used")
        allow(":legacy") {
            reason("Only legacy modules can still depend on another legacy modules")
        }
    }
    restrictDependency(libs.mockk) {
        reason("Fakes should be used instead")
        allow(":feature:a") {
            reason("This feature requires mockk to test platform code")
        }
    }
}

// apply(from = "scripts/generate_modules.gradle.kts")
