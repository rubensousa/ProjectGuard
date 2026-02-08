package com.rubensousa.dependencyguard.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector

internal class DependencyGraphBuilder {

    private val supportedConfigurations = mutableSetOf(
        "compileClasspath",
        "testCompileClasspath",
        "testFixturesCompileClasspath",
    )
    private val androidConfigurationPatterns = mutableSetOf(
        "androidTestUtil", // To exclude test orchestrator in some modules
        "AndroidTestCompileClasspath" // Tests would include
    )

    fun buildFrom(project: Project): List<DependencyGraph> {
        return project.configurations
            .filter { config -> isConfigurationSupported(config) }
            .map { config ->
                val graph = DependencyGraph(
                    configurationId = config.name,
                )
                /**
                 * Until https://github.com/rubensousa/DependencyGuard/issues/3 is resolved,
                 * exclude transitive dependency traversals for test configurations
                 */
                if (isTestConfiguration(config)) {
                    config.incoming.dependencies
                        .withType(ProjectDependency::class.java)
                        .forEach { projectDependency ->
                            graph.addDependency(project.path, projectDependency.path)
                        }
                    config.incoming.dependencies
                        .withType(ExternalModuleDependency::class.java)
                        .forEach { lib ->
                            graph.addDependency(project.path, "${lib.group}:${lib.name}")
                        }
                } else {
                    config.incoming.resolutionResult.allDependencies.forEach { dependencyResult ->
                        val moduleId = extractModuleId(dependencyResult.from.id)
                        val dependencyId = extractDependencyId(dependencyResult.requested)
                        if (moduleId != null && dependencyId != null) {
                            graph.addDependency(moduleId, dependencyId)
                        }
                    }
                }
                graph
            }
            .filter { graph -> graph.nodes.isNotEmpty() }
    }

    private fun extractModuleId(component: ComponentIdentifier): String? {
        return when (component) {
            is ProjectComponentIdentifier -> {
                component.projectPath
            }

            is ModuleComponentIdentifier -> {
                "${component.group}:${component.module}"
            }

            else -> null
        }
    }

    private fun extractDependencyId(component: ComponentSelector): String? {
        return when (component) {
            is ProjectComponentSelector -> {
                component.projectPath
            }

            is ModuleComponentSelector -> {
                "${component.group}:${component.module}"
            }

            else -> null
        }
    }

    private fun isTestConfiguration(configuration: Configuration): Boolean {
        return configuration.name.lowercase().contains("test")
    }

    private fun isConfigurationSupported(configuration: Configuration): Boolean {
        if (!configuration.isCanBeResolved) {
            return false
        }
        val name = configuration.name
        if (supportedConfigurations.contains(name)) {
            return true
        }
        return androidConfigurationPatterns.any { pattern ->
            name.contains(pattern)
        }
    }

}
