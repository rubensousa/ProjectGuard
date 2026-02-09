package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraph
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

internal fun buildDependencyGraph(scope: DependencyGraph.() -> Unit): DependencyGraph {
    val graph = DependencyGraph("implementation")
    graph.apply(scope)
    return graph
}
