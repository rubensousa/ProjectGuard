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

internal class SuppressionMap {

    private val suppressions = mutableMapOf<String, MutableMap<String, DependencySuppression>>()

    fun set(configuration: BaselineConfiguration) {
        suppressions.clear()
        configuration.suppressions.forEach { entry ->
            val moduleId = entry.key
            val suppressions = entry.value
            suppressions.forEach { suppression ->
                add(moduleId, suppression)
            }
        }
    }

    fun add(module: String, dependency: String, reason: String = UNSPECIFIED_REASON) {
        add(module, DependencySuppression(dependency, reason))
    }

    fun add(module: String, suppression: DependencySuppression) {
        val moduleSuppressions = suppressions.getOrPut(module) { mutableMapOf() }
        moduleSuppressions[suppression.dependency] = suppression
    }

    fun getSuppression(module: String, dependency: String): DependencySuppression? {
        val moduleSuppressions = suppressions[module] ?: return null
        return moduleSuppressions[dependency]
    }

    fun getBaseline(): BaselineConfiguration {
        val output = mutableMapOf<String, List<DependencySuppression>>()
        suppressions.keys.sorted().forEach { module ->
            suppressions[module]?.let { suppressions ->
                val outputList = mutableListOf<DependencySuppression>()
                suppressions.keys.sorted().forEach { dependency ->
                    suppressions[dependency]?.let { suppression ->
                        outputList.add(suppression)
                    }
                }
                output[module] = outputList
            }
        }
        return BaselineConfiguration(output)
    }

}
