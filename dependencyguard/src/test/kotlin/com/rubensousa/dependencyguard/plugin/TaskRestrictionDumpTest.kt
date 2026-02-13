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
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDependencyReport
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDump
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionModuleReport
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TaskRestrictionDumpTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val plugin = DependencyPluginSimulator(temporaryFolder)

    @Test
    fun `restriction dump is created`() {
        // given
        val moduleId = ":domain"
        val fatalModuleId = ":legacy"
        val reason = "Some reason"
        plugin.dumpDependencies(moduleId) {
            addDependency(moduleId, fatalModuleId)
        }
        plugin.dumpAggregateDependencies()
        val spec = dependencyGuard {
            restrictDependency(fatalModuleId) {
                reason(reason)
            }
        }

        // when
        val outputFile = plugin.dumpRestrictions(moduleId, spec)

        // then
        val dump = Json.decodeFromString<RestrictionDump>(outputFile.readText())
        assertThat(dump.modules).isEqualTo(
            listOf(
                RestrictionModuleReport(
                    module = moduleId,
                    restrictions = listOf(
                        RestrictionDependencyReport(
                            dependency = fatalModuleId,
                            pathToDependency = null,
                            reason = reason
                        )
                    ),
                )
            )
        )
    }

}
