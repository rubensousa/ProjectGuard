plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("jacoco")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradle.jacoco)
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

if (parent?.name == "ProjectGuard") {
    plugins.apply(libs.plugins.maven.publish.get().pluginId)
}

gradlePlugin {
    plugins {
        create("projectGuard") {
            id = "com.rubensousa.projectguard"
            implementationClass = "com.rubensousa.projectguard.plugin.ProjectGuardPlugin"
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

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
