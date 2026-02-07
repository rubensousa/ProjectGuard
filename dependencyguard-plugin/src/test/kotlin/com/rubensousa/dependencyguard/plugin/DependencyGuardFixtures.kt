package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import org.gradle.testfixtures.ProjectBuilder

internal fun dependencyGuard(scope: DependencyGuardScope.() -> Unit): DependencyGuardSpec {
    val project = ProjectBuilder.builder().build()
    val extension = project.extensions.create(
        "dependencyGuard",
        DependencyGuardExtension::class.java
    )
    extension.scope()
    return extension.getSpec()
}
