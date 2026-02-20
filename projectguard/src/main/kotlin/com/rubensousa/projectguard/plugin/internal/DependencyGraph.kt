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

import java.io.Serializable

internal class DependencyGraph : Serializable {

    private val configurations = mutableMapOf<String, Configuration>()

    fun getConfigurations() = configurations.values.toList()

    fun addDependency(
        module: String,
        dependency: DirectDependency,
        configurationId: String = DependencyConfiguration.COMPILE,
    ) {
        val configuration = configurations.getOrPut(configurationId) {
            Configuration(configurationId)
        }
        configuration.add(module = module, dependency = dependency)
    }

    fun addInternalDependency(
        module: String,
        dependency: String,
        configurationId: String = DependencyConfiguration.COMPILE,
    ) {
        addDependency(
            module = module,
            dependency = DirectDependency(dependency, isLibrary = false),
            configurationId = configurationId,
        )
    }

    fun addLibraryDependency(
        module: String,
        dependency: String,
        configurationId: String = DependencyConfiguration.COMPILE,
    ) {
        addDependency(
            module = module,
            dependency = DirectDependency(dependency, isLibrary = true),
            configurationId = configurationId
        )
    }

    fun getDependencies(module: String): List<Dependency> {
        val visitedDependencies = mutableSetOf<String>()
        val paths = mutableMapOf<String, Dependency>()
        val queue = ArrayDeque<TraversalState>()
        configurations.values.forEach { configuration ->
            configuration.getDependencies(module).forEach { dependency ->
                queue.addFirst(
                    TraversalState(
                        configurationId = configuration.id,
                        dependency = dependency,
                    )
                )
            }
        }
        while (queue.isNotEmpty()) {
            val currentTraversal = queue.removeFirst()
            val currentDependency = currentTraversal.dependency
            if (visitedDependencies.contains(currentDependency.id)) {
                continue
            }
            paths[currentDependency.id] = currentDependency
            visitedDependencies.add(currentDependency.id)
            configurations.values.forEach { configuration ->
                // Search only for non-test configurations as they're not considered transitive at this point
                if (!DependencyConfiguration.isTestConfiguration(configuration.id)) {
                    configuration.getDependencies(currentDependency.id).forEach { nextDependency ->
                        queue.addFirst(
                            TraversalState(
                                configurationId = configuration.id,
                                dependency = TransitiveDependency(
                                    id = nextDependency.id,
                                    isLibrary = nextDependency.isLibrary,
                                    path = when (currentDependency) {
                                        is DirectDependency -> listOf(currentDependency.id, nextDependency.id)
                                        is TransitiveDependency -> currentDependency.path + nextDependency.id
                                    },
                                )
                            )
                        )
                    }
                }
            }
        }
        return paths.values.sortedBy { dependency -> dependency.id }
    }

    private data class TraversalState(
        val configurationId: String,
        val dependency: Dependency,
    )

    class Configuration(val id: String) : Serializable {

        private val nodes = mutableMapOf<String, MutableSet<DirectDependency>>()

        fun add(module: String, dependency: DirectDependency) {
            val existingDependencies = nodes.getOrPut(module) { mutableSetOf() }
            existingDependencies.add(dependency)
        }

        fun getDependencies(module: String): Set<DirectDependency> {
            return nodes[module] ?: emptySet()
        }

        override fun equals(other: Any?): Boolean {
            return other is Configuration
                    && other.id == this.id
                    && other.nodes == this.nodes
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + nodes.hashCode()
            return result
        }

    }

    override fun equals(other: Any?): Boolean {
        return other is DependencyGraph
                && other.configurations == this.configurations
    }

    override fun hashCode(): Int {
        return configurations.hashCode()
    }


}
