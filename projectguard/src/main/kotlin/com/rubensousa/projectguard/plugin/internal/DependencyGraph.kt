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
    private val libraries = mutableSetOf<String>()

    fun getConfigurations() = configurations.values.toList()

    fun addInternalDependency(
        module: String,
        dependency: String,
        configurationId: String = DependencyConfiguration.COMPILE,
    ) {
        addDependency(
            module = module,
            dependency = dependency,
            configurationId = configurationId
        )
    }

    fun addExternalDependency(
        module: String,
        dependency: String,
        configurationId: String = DependencyConfiguration.COMPILE,
    ) {
        addDependency(
            module = module,
            dependency = dependency,
            configurationId = configurationId
        )
        libraries.add(dependency)
    }

    fun isExternalLibrary(dependency: String): Boolean {
        return libraries.contains(dependency)
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
                        path = emptyList()
                    )
                )
            }
        }
        while (queue.isNotEmpty()) {
            val currentTraversal = queue.removeFirst()
            val currentDependency = currentTraversal.dependency
            if (visitedDependencies.contains(currentDependency)) {
                continue
            }
            paths[currentDependency] = if (currentTraversal.path.isEmpty()) {
                DirectDependency(currentDependency)
            } else {
                TransitiveDependency(
                    currentDependency,
                    currentTraversal.path + currentDependency
                )
            }
            visitedDependencies.add(currentDependency)
            configurations.values.forEach { configuration ->
                // Search only for non-test configurations as they're not considered transitive at this point
                if (!DependencyConfiguration.isTestConfiguration(configuration.id)) {
                    configuration.getDependencies(currentDependency).forEach { nextDependency ->
                        queue.addFirst(
                            TraversalState(
                                configurationId = configuration.id,
                                dependency = nextDependency,
                                path = currentTraversal.path + currentDependency
                            )
                        )
                    }
                }
            }
        }
        return paths.values.sortedBy { it.id }
    }

    private fun addDependency(
        module: String,
        dependency: String,
        configurationId: String,
    ) {
        val configuration = configurations.getOrPut(configurationId) {
            Configuration(configurationId)
        }
        configuration.add(module = module, dependency = dependency)
    }

    private data class TraversalState(
        val configurationId: String,
        val dependency: String,
        val path: List<String>,
    )

    class Configuration(val id: String) : Serializable {

        private val nodes = mutableMapOf<String, MutableSet<String>>()

        fun add(module: String, dependency: String) {
            val existingDependencies = nodes.getOrPut(module) { mutableSetOf() }
            existingDependencies.add(dependency)
        }

        fun getDependencies(module: String): Set<String> {
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
                && other.libraries == this.libraries
                && other.configurations == this.configurations
    }

    override fun hashCode(): Int {
        var result = configurations.hashCode()
        result = 31 * result + libraries.hashCode()
        return result
    }


}
