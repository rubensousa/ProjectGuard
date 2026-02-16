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

internal class DependencyRestrictionFinder {

    fun find(
        moduleId: String,
        graph: DependencyGraph,
        spec: ProjectGuardSpec,
    ): List<DependencyRestriction> {
        return find(
            moduleId = moduleId,
            graphs = listOf(graph),
            spec = spec
        )
    }

    fun find(
        moduleId: String,
        graphs: List<DependencyGraph>,
        spec: ProjectGuardSpec,
    ): List<DependencyRestriction> {
        val restrictions = mutableListOf<DependencyRestriction>()
        graphs.forEach { graph ->
            graph.getAllDependencies(moduleId).forEach { dependency ->
                fillRestrictions(
                    restrictions = restrictions,
                    moduleId = moduleId,
                    dependency = dependency,
                    spec = spec,
                )
            }
        }
        // We might find multiple restrictions to the same dependency, just filter them out
        return filterRestrictions(moduleId, restrictions)
    }

    private fun filterRestrictions(
        moduleId: String,
        restrictions: List<DependencyRestriction>,
    ): List<DependencyRestriction> {
        val visitedMatches = mutableSetOf<String>()
        val output = mutableListOf<DependencyRestriction>()
        restrictions.forEach { restriction ->
            val restrictionId = restriction.getId(moduleId)
            if (!visitedMatches.contains(restrictionId)) {
                visitedMatches.add(restrictionId)
                output.add(restriction)
            }
        }
        return output
    }

    private fun fillRestrictions(
        restrictions: MutableList<DependencyRestriction>,
        moduleId: String,
        dependency: Dependency,
        spec: ProjectGuardSpec,
    ) {
        fillModuleRestrictions(
            restrictions = restrictions,
            moduleId = moduleId,
            dependency = dependency,
            spec = spec
        )
        fillGuardRestrictions(
            restrictions = restrictions,
            moduleId = moduleId,
            dependency = dependency,
            spec = spec
        )
        fillDependencyRestrictions(
            restrictions = restrictions,
            moduleId = moduleId,
            dependency = dependency,
            spec = spec
        )
    }

    /**
     * Module restrictions are deny-by-default.
     * Each spec specifies individual allowances for dependencies
     */
    private fun fillModuleRestrictions(
        restrictions: MutableList<DependencyRestriction>,
        moduleId: String,
        dependency: Dependency,
        spec: ProjectGuardSpec,
    ) {
        spec.moduleRestrictionSpecs.forEach { restriction ->
            val matchesModule = hasModuleMatch(
                modulePath = moduleId,
                referencePath = restriction.modulePath
            )
            val isDependencyAllowed = restriction.allowed.any { exclusion ->
                hasModuleMatch(modulePath = dependency.id, referencePath = exclusion.modulePath)
            }
            if (!isDependencyAllowed && matchesModule) {
                restrictions.add(
                    DependencyRestriction.from(
                        dependency = dependency,
                        reason = restriction.reason,
                    )
                )
            }
        }
    }

    /**
     * Guard restrictions are allow-by-default
     * Each guard block specifies individual denials for dependencies
     */
    private fun fillGuardRestrictions(
        restrictions: MutableList<DependencyRestriction>,
        moduleId: String,
        dependency: Dependency,
        spec: ProjectGuardSpec,
    ) {
        spec.guardSpecs.forEach { restriction ->
            val matchesModule = hasModuleMatch(
                modulePath = moduleId,
                referencePath = restriction.modulePath
            )
            if (matchesModule) {
                restriction.denied.find { spec ->
                    hasModuleMatch(
                        modulePath = dependency.id,
                        referencePath = spec.modulePath
                    )
                }?.let { specDenied ->
                    restrictions.add(
                        DependencyRestriction.from(
                            dependency = dependency,
                            reason = specDenied.reason,
                        )
                    )
                }
            }
        }
    }

    /**
     * Dependency restrictions are deny-by-default.
     * Each dependency restriction specifies individual allowances for dependencies
     */
    private fun fillDependencyRestrictions(
        restrictions: MutableList<DependencyRestriction>,
        moduleId: String,
        dependency: Dependency,
        spec: ProjectGuardSpec,
    ) {
        spec.dependencyRestrictionSpecs.forEach { restriction ->
            val isDependencyRestricted = hasModuleMatch(
                modulePath = dependency.id,
                referencePath = restriction.dependencyPath
            )
            val isModuleAllowed = restriction.allowed.any { exclusion ->
                hasModuleMatch(modulePath = moduleId, referencePath = exclusion.modulePath)
            }
            if (isDependencyRestricted && !isModuleAllowed) {
                restrictions.add(
                    DependencyRestriction.from(
                        dependency = dependency,
                        reason = restriction.reason,
                    )
                )
            }
        }
    }

    private fun hasModuleMatch(
        modulePath: String,
        referencePath: String,
    ): Boolean {
        if (modulePath == referencePath) {
            return true
        }
        return modulePath.startsWith(referencePath)
    }

}
