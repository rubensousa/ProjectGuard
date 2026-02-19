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

package com.rubensousa.projectguard.plugin.internal

import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphDump
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency

internal class DependencyGraphBuilder {

    fun buildFromDump(projectDump: DependencyGraphDump): DependencyGraph {
        val graph = DependencyGraph()
        projectDump.modules.forEach { report ->
            report.configurations.forEach { configuration ->
                configuration.dependencies.forEach { dependency ->
                    if (dependency.isLibrary) {
                        graph.addExternalDependency(
                            configurationId = configuration.id,
                            module = report.module,
                            dependency = dependency.id,
                        )
                    } else {
                        graph.addInternalDependency(
                            configurationId = configuration.id,
                            module = report.module,
                            dependency = dependency.id,
                        )
                    }
                }
            }
        }
        return graph
    }

    fun buildFromProject(project: Project): DependencyGraph {
        val graph = DependencyGraph()
        project.configurations
            .filter { config -> config.isCanBeResolved && DependencyConfiguration.isConfigurationSupported(config.name) }
            .forEach { config ->
                val moduleId = project.path
                config.incoming.dependencies
                    .forEach { dependency ->
                        when (dependency) {
                            is ProjectDependency -> {
                                if (dependency.path != moduleId) {
                                    graph.addInternalDependency(
                                        module = moduleId,
                                        dependency = dependency.path,
                                        configurationId = config.name
                                    )
                                }
                            }

                            is ExternalModuleDependency -> {
                                graph.addExternalDependency(
                                    module = moduleId,
                                    dependency = "${dependency.group}:${dependency.name}",
                                    configurationId = config.name
                                )
                            }
                        }
                    }
            }
        return graph
    }
}