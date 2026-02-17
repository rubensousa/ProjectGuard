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

package com.rubensousa.projectguard.plugin.internal.report

import com.rubensousa.projectguard.plugin.internal.SuppressionMap

internal class VerificationReportBuilder(
    private val suppressionMap: SuppressionMap,
) {

    fun build(
        dependencyGraphDump: DependencyGraphDump,
        restrictionDump: RestrictionDump,
    ): VerificationReport {
        val totalFatalMatches = mutableMapOf<String, MutableList<FatalMatch>>()
        val totalSuppressedMatches = mutableMapOf<String, MutableList<SuppressedMatch>>()
        val reports = mutableSetOf<String>()
        restrictionDump.modules.forEach { moduleReport ->
            moduleReport.restrictions.forEach { restriction ->
                val fatalMatches = totalFatalMatches.getOrPut(moduleReport.module) {
                    mutableListOf()
                }
                val suppressedMatches = totalSuppressedMatches.getOrPut(moduleReport.module) {
                    mutableListOf()
                }
                val suppression = suppressionMap.getSuppression(
                    module = moduleReport.module,
                    dependency = restriction.dependency
                )
                if (suppression != null) {
                    suppressedMatches.add(
                        SuppressedMatch(
                            dependency = restriction.dependency,
                            pathToDependency = restriction.pathToDependency ?: restriction.dependency,
                            suppressionReason = suppression.reason
                        )
                    )
                } else {
                    fatalMatches.add(
                        FatalMatch(
                            moduleId = moduleReport.module,
                            dependency = restriction.dependency,
                            pathToDependency = restriction.pathToDependency ?: restriction.dependency,
                            reason = restriction.reason
                        )
                    )
                }
                reports.add(moduleReport.module)
            }
        }
        val sortedReports = reports.sortedBy { it }
            .map { moduleId ->
                VerificationModuleReport(
                    module = moduleId,
                    fatal = totalFatalMatches[moduleId]?.sortedBy { it.dependency } ?: emptyList(),
                    suppressed = totalSuppressedMatches[moduleId]?.sortedBy { it.dependency } ?: emptyList(),
                )
            }
        val graph = mutableMapOf<String, MutableSet<DependencyReferenceDump>>()
        dependencyGraphDump.modules.forEach { report ->
            report.configurations.forEach { configuration ->
                val moduleDependencies = graph.getOrPut(report.module) { mutableSetOf() }
                moduleDependencies.addAll(configuration.dependencies.map { dependency ->
                    DependencyReferenceDump(dependency.id, dependency.isLibrary)
                })
            }
        }
        return VerificationReport(sortedReports, graph.mapValues { entry ->
            entry.value.sortedBy { it.id }
        })
    }

}
