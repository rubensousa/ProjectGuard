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

internal const val UNSPECIFIED_REASON = "Unspecified"

internal sealed interface DependencyRestriction {
    val dependencyId: String
    val reason: String

    fun getId(moduleId: String): String {
        return "module:$moduleId|dependency:${dependencyId}"
    }

    fun getText(moduleId: String): String

    companion object {

        fun from(dependency: com.rubensousa.projectguard.plugin.internal.Dependency, reason: String): DependencyRestriction {
            return when (dependency) {
                is com.rubensousa.projectguard.plugin.internal.DirectDependency -> DirectDependencyRestriction(
                    dependencyId = dependency.id,
                    reason = reason,
                )

                is com.rubensousa.projectguard.plugin.internal.TransitiveDependency -> TransitiveDependencyRestriction(
                    dependencyId = dependency.id,
                    pathToDependency = dependency.path,
                    reason = reason,
                )
            }
        }
    }
}

internal data class DirectDependencyRestriction(
    override val dependencyId: String,
    override val reason: String = UNSPECIFIED_REASON,
) : DependencyRestriction {

    override fun getText(moduleId: String): String {
        return """
                | Dependency restriction found!
                | Module -> $moduleId
                | Match -> $dependencyId
                | Module '$moduleId' cannot depend on '$dependencyId'
                | Reason: $reason
                """.trimMargin()
    }

}

internal data class TransitiveDependencyRestriction(
    override val dependencyId: String,
    val pathToDependency: List<String>,
    override val reason: String = UNSPECIFIED_REASON,
) : DependencyRestriction {

    override fun getText(moduleId: String): String {
        return """
                | Transitive dependency restriction found!
                | Module -> $moduleId
                | Match -> $dependencyId from ${getPathToDependencyText()}
                | Module '$moduleId' cannot depend on '$dependencyId'
                | Reason: $reason
                """.trimMargin()
    }

    fun getPathToDependencyText(): String {
        return pathToDependency.joinToString(separator = " -> ") { it }
    }

}
