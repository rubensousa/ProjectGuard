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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.artifacts.result.DefaultUnresolvedDependencyResult
import org.gradle.api.provider.Provider

internal class DependencyGraphBuilder {

    fun buildFromDump(projectDump: DependencyGraphDump): DependencyGraph {
        val graph = DependencyGraph()
        projectDump.modules.forEach { report ->
            report.configurations.forEach { configuration ->
                configuration.dependencies.forEach { dependency ->
                    graph.addDependency(
                        module = report.module,
                        dependency = DirectDependency(
                            id = dependency.id,
                            isLibrary = dependency.isLibrary
                        ),
                        configurationId = configuration.id
                    )
                }
            }
        }
        return graph
    }

    fun buildFromComponents(results: Map<String, Provider<ResolvedComponentResult>>): DependencyGraph {
        val graph = DependencyGraph()
        results.forEach { (configurationId, resultProvider) ->
            val result = resultProvider.get()
            val resultId = result.id
            if (resultId is ProjectComponentIdentifier) {
                val moduleId = resultId.projectPath
                result.dependencies.forEach { dependencyResult ->
                    when (dependencyResult) {
                        is DefaultUnresolvedDependencyResult -> {
                            val requested = dependencyResult.requested
                            if (requested is ModuleComponentSelector) {
                                graph.addLibraryDependency(
                                    module = moduleId,
                                    dependency = "${requested.group}:${requested.module}",
                                    configurationId = configurationId,
                                )
                            }
                        }

                        is ResolvedDependencyResult -> {
                            val selected = dependencyResult.selected
                            when (val projectId = selected.id) {
                                is ProjectComponentIdentifier -> {
                                    if (projectId.projectPath != moduleId) {
                                        graph.addInternalDependency(
                                            module = moduleId,
                                            dependency = projectId.projectPath,
                                            configurationId = configurationId,
                                        )
                                    }
                                }

                                is ModuleComponentIdentifier -> {
                                    graph.addLibraryDependency(
                                        module = moduleId,
                                        dependency = "${projectId.group}:${projectId.module}",
                                        configurationId = configurationId,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return graph
    }

}
