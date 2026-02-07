# DependencyGuard

Protect your project against undesired dependencies across different modules.


## Setup

1. Add the following to your versions.toml:

```
[plugins]
dependencyguard = { id = "com.rubensousa.dependencyguard", version = "1.0.0-alpha01" }
```

2. Apply the plugin in your root `build.gradle.kts` and all of the subprojects:

```kotlin
plugins {
    alias(libs.plugins.dependencyguard) apply true
}

val dependencyGuardPlugin = libs.plugins.dependencyguard.get().pluginId

subprojects {
    apply(plugin = dependencyGuardPlugin)
}

dependencyGuard {
    // Global configuration. See below for examples
}
```

## Features

1. Restrict dependencies between different projects:


```kotlin
dependencyGuard {
    /**
     * This matches all modules within the domain directory.
     * E.g (":domain:a", ":domain:b", ":domain:c")
     */
    restrict(":domain") {
        /**
         * This matches all modules within the legacy directory.
         * E.g (":legacy:a", ":legacy:b", ":legacy:c")
         */
        deny(":legacy")
    }
    restrict(":domain:a") {
        deny(":domain:b")
    }
}
```

2. Restrict external dependencies:

```kotlin
dependencyGuard {
    restrictAll {
        /**
         * Prevent all modules from using mockk for tests
         */
        deny(libs.mockk)
    }
}
```

3. Use the `dependencyGuardCheck` task to validate the project using the rules you configured

4. Use the `dependencyGuardHtmlReport` task to generate a HTML report with the violations affected by this project, including the ones that are ignored