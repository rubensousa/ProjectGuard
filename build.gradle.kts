// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
    id(libs.plugins.dependencyguard.get().pluginId) apply true
}
val dependencyGuardPlugin = libs.plugins.dependencyguard.get().pluginId

subprojects {
    if (!this.path.contains("dependencyguard-plugin")) {
        apply(plugin = dependencyGuardPlugin)
    }
}

dependencyGuard {
    restrict(":domain") {
        deny(":feature") {
            setReason("Dependency should be inverted. Feature depends on domain")
            except(":domain:a", ":domain:b", ":domain:c")
        }
        deny(":data") {
            setReason("Dependency should be inverted. Data depends on domain")
        }
    }
    restrict(":data") {
        deny(":feature") {
            setReason("Data layer should not depend on Feature layer")
        }
    }
    restrict(":feature") {
        deny(":feature") {
            setReason("Features should not depend on other features directly")
            except(":feature:a", ":feature:b")
        }
    }
    restrictAll {
        deny(":legacy") {
            setReason("All modules should not depend on legacy code")
            except(":legacy")
        }
        deny(libs.mockk) {
            setReason("Fakes should be used instead")
            except(":feature:z")
        }
    }
}

// apply(from = "scripts/generate_modules.gradle.kts")
