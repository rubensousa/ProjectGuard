plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        // Use the set() function to ensure compatibility with older Gradle versions
        enabled.set(true)
    }
}

if (parent?.name == "DependencyGuard") {
    plugins.apply(libs.plugins.maven.publish.get().pluginId)
}

gradlePlugin {
    plugins {
        create("dependencyGuard") {
            id = "com.rubensousa.dependencyguard"
            implementationClass = "com.rubensousa.dependencyguard.plugin.DependencyGuardPlugin"
        }
    }
}

tasks.test {
    useJUnit()
}

dependencies {
    implementation(libs.kotlin.serialization.json)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin)
    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
    testImplementation(libs.junit)
}
