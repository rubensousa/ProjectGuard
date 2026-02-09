package com.rubensousa.dependencyguard.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency

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

    fun buildFromReport(report: DependencyGraphAggregateReport): List<DependencyGraph> {
        val graphs = mutableMapOf<String, DependencyGraph>()
        report.moduleReports.forEach { report ->
            report.configurations.forEach { configuration ->
                if (isConfigurationSupported(configuration.id)) {
                    val graph = graphs.getOrPut(configuration.id) {
                        DependencyGraph(configurationId = configuration.id)
                    }
                    configuration.dependencies.forEach { dependency ->
                        graph.addDependency(report.module, dependency)
                    }
                }

            }
        }
        return graphs.values.toList()
    }

    fun buildFrom(project: Project): List<DependencyGraph> {
        return project.configurations
            .filter { config -> config.isCanBeResolved && isConfigurationSupported(config.name) }
            .map { config ->
                val graph = DependencyGraph(
                    configurationId = config.name,
                )
                val moduleId = project.path
                config.incoming.dependencies
                    .forEach { dependency ->
                        when (dependency) {
                            is ProjectDependency -> {
                                if (dependency.path != moduleId) {
                                    graph.addDependency(moduleId, dependency.path)
                                }
                            }

                            is ExternalModuleDependency -> {
                                graph.addDependency(
                                    moduleId,
                                    "${dependency.group}:${dependency.name}"
                                )
                            }
                        }
                    }
                graph
            }
            .filter { graph -> graph.nodes.isNotEmpty() }
    }

    private fun isConfigurationSupported(configurationId: String): Boolean {
        if (supportedConfigurations.contains(configurationId)) {
            return true
        }
        return androidConfigurationPatterns.any { pattern ->
            configurationId.contains(pattern)
        }
    }

}
