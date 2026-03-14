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

package com.rubensousa.projectguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.projectguard.plugin.internal.BaselineConfiguration
import com.rubensousa.projectguard.plugin.internal.BaselineProcessor
import com.rubensousa.projectguard.plugin.internal.DependencySuppression
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TaskBaselineTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val plugin = DependencyPluginSimulator(temporaryFolder)

    @Test
    fun `baseline is created for current restrictions`() {
        // given
        val moduleId = ":domain"
        val secondModuleId = ":data"
        val fatalModuleId = ":legacy"
        val reason = "Some reason"
        plugin.dumpDependencies(moduleId) {
            addInternalDependency(moduleId, fatalModuleId)
        }
        plugin.dumpDependencies(secondModuleId) {
            addInternalDependency(secondModuleId, fatalModuleId)
        }
        plugin.dumpAggregateDependencies()
        val spec = projectGuard {
            restrictDependency(fatalModuleId) {
                reason(reason)
            }
        }
        plugin.dumpRestrictions(moduleId, spec)
        plugin.dumpRestrictions(secondModuleId, spec)
        plugin.dumpAggregateRestrictions()

        // when
        val outputFile = plugin.generateBaseline()

        // then
        val baselineProcessor = BaselineProcessor()
        val baseline = baselineProcessor.parse(outputFile)
        assertThat(baseline.suppressions).isEqualTo(
            mapOf(
                moduleId to listOf(
                    DependencySuppression(
                        dependency = fatalModuleId,
                        reason = "Suppressed from baseline"
                    )
                ),
                secondModuleId to listOf(
                    DependencySuppression(
                        dependency = fatalModuleId,
                        reason = "Suppressed from baseline"
                    ),
                )
            )
        )
    }

    @Test
    fun `baseline generation keeps the previous suppresion reasons`() {
        // given
        val baselineProcessor = BaselineProcessor()
        val moduleId = ":domain"
        val secondModuleId = ":data"
        val fatalModuleId = ":legacy"
        val firstSuppressionReason = "Domain still depends on this"
        val secondSuppressionReason = "Data still depends on this"
        val baselineFile = plugin.getBaselineFile()
        val currentBaseline = BaselineConfiguration(
            suppressions = mapOf(
                moduleId to listOf(
                    DependencySuppression(
                        dependency = fatalModuleId,
                        reason = firstSuppressionReason,
                    )
                ),
                secondModuleId to listOf(
                    DependencySuppression(
                        dependency = fatalModuleId,
                        reason = secondSuppressionReason
                    ),
                )
            )
        )
        baselineProcessor.write(baselineFile, currentBaseline)
        plugin.dumpDependencies(moduleId) {
            addInternalDependency(moduleId, fatalModuleId)
        }
        plugin.dumpDependencies(secondModuleId) {
            addInternalDependency(secondModuleId, fatalModuleId)
        }
        plugin.dumpAggregateDependencies()
        val spec = projectGuard { restrictDependency(fatalModuleId) }
        plugin.dumpRestrictions(moduleId, spec)
        plugin.dumpRestrictions(secondModuleId, spec)
        plugin.dumpAggregateRestrictions()

        // when
        val outputFile = plugin.generateBaseline()

        // then
        val baseline = baselineProcessor.parse(outputFile)
        assertThat(baseline.suppressions).isEqualTo(
            mapOf(
                moduleId to listOf(
                    DependencySuppression(
                        dependency = fatalModuleId,
                        reason = firstSuppressionReason
                    )
                ),
                secondModuleId to listOf(
                    DependencySuppression(
                        dependency = fatalModuleId,
                        reason = secondSuppressionReason
                    ),
                )
            )
        )
    }

}
