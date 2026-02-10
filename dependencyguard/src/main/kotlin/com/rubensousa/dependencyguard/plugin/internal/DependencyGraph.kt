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

import java.io.Serializable

internal class DependencyGraph(
    val configurationId: String,
    val nodes: MutableMap<String, MutableSet<String>> = mutableMapOf(),
) : Serializable {

    fun addDependency(
        module: String,
        dependency: String,
    ) {
        val existingDependencies = nodes.getOrPut(module) { mutableSetOf() }
        existingDependencies.add(dependency)
    }

    fun getDependencies(module: String): Set<String> {
        return nodes[module] ?: emptySet()
    }

    fun getDependencyMatches(module: String): List<DependencyMatch> {
        /**
         * Until https://github.com/rubensousa/DependencyGuard/issues/3 is resolved,
         * exclude transitive dependency traversals for test configurations
         */
        if (configurationId.contains("test")) {
            return getDependencies(module).map {
                DependencyMatch(it, listOf(it))
            }
        }
        val visitedDependencies = mutableSetOf<String>()
        val paths = mutableMapOf<String, DependencyMatch>()
        val stack = ArrayDeque<TraversalState>()
        stack.addAll(getDependencies(module).map { dependency ->
            TraversalState(dependency, emptyList())
        })
        while (stack.isNotEmpty()) {
            val currentModule = stack.removeFirst()
            val currentDependency = currentModule.dependency
            if (visitedDependencies.contains(currentDependency)) {
                continue
            }
            paths[currentDependency] = DependencyMatch(
                currentDependency, currentModule.path + currentDependency
            )
            visitedDependencies.add(currentDependency)
            getDependencies(currentDependency).forEach { nextDependency ->
                stack.addFirst(
                    TraversalState(
                        nextDependency,
                        currentModule.path + currentDependency
                    )
                )
            }
        }
        return paths.values.toList()
    }

    override fun toString(): String {
        return "DependencyGraph(configurationId='$configurationId', nodes=$nodes)"
    }

    data class DependencyMatch(
        val dependencyId: String,
        val path: List<String>,
    )

    private data class TraversalState(
        val dependency: String,
        val path: List<String>,
    )

}
