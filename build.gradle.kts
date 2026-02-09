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

subprojects {
    if (!this.path.contains("dependencyguard-plugin")) {
        apply(plugin = dependencyGuardPlugin)
    }
}

dependencyGuard {
    restrictModule(":domain") {
        deny(":feature") {
            setReason("Dependency should be inverted. Feature depends on domain")
        }
        deny(":data") {
            setReason("Dependency should be inverted. Data depends on domain")
        }
    }
    restrictModule(":feature") {
        deny(":feature") {
            setReason("Features should not depend on other features directly")
        }
    }
    restrictDependency(":legacy") {
        setReason("Legacy modules should no longer be used")
        suppress(":domain") {
            setReason("Legacy already exists in some domains")
        }
        allow(":legacy") {
            setReason("Only legacy modules can still depend on another legacy modules")
        }
    }
    restrictDependency(libs.mockk) {
        setReason("Fakes should be used instead")
        allow(":feature:a") {
            setReason("This feature requires mockk to test platform code")
        }
        suppress(":feature:z") {
            setReason("Some tests in this feature still use mocks")
        }
    }
}

// apply(from = "scripts/generate_modules.gradle.kts")
