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

package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.BaselineConfiguration
import com.rubensousa.dependencyguard.plugin.internal.DependencySuppression
import com.rubensousa.dependencyguard.plugin.internal.YamlProcessor
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
            addDependency(moduleId, fatalModuleId)
        }
        plugin.dumpDependencies(secondModuleId) {
            addDependency(secondModuleId, fatalModuleId)
        }
        plugin.dumpAggregateDependencies()
        val spec = dependencyGuard {
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
        val yamlProcessor = YamlProcessor()
        val baseline = yamlProcessor.parse(outputFile, BaselineConfiguration::class.java)
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

}
