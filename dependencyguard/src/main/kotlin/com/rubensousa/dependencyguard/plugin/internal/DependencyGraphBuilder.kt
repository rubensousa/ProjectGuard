/*
 * Copyright 2026 Rúben Sousa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rubensousa.dependencyguard.plugin.internal

import org.gradle.api.Project
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

    fun buildFromReport(aggregateReport: DependencyGraphAggregateReport): List<DependencyGraph> {
        val graphs = mutableMapOf<String, DependencyGraph>()
        aggregateReport.moduleReports.forEach { report ->
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

    fun buildFromProject(project: Project): List<DependencyGraph> {
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
