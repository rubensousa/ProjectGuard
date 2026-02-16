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
import com.rubensousa.projectguard.plugin.internal.report.ConfigurationDependencies
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphDump
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphModuleDump
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TaskAggregateDependencyDumpTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val plugin = DependencyPluginSimulator(temporaryFolder)

    @Test
    fun `aggregate dependency dump is created`() {
        // given
        val firstModule = "module:a"
        val firstDependency = "domain:a"
        val secondModule = "module:b"
        val secondDependency = "domain:b"
        plugin.dumpDependencies(firstModule) {
            addDependency(firstModule, firstDependency)
        }
        plugin.dumpDependencies(secondModule) {
            addDependency(secondModule, secondDependency)
        }

        // when
        val outputFile = plugin.dumpAggregateDependencies()

        // then
        val dump = Json.decodeFromString<DependencyGraphDump>(outputFile.readText())
        assertThat(dump.modules).isEqualTo(
            listOf(
                DependencyGraphModuleDump(
                    module = firstModule,
                    configurations = listOf(
                        ConfigurationDependencies(
                            id = "compileClasspath",
                            dependencies = listOf(firstDependency)
                        ),
                    )
                ),
                DependencyGraphModuleDump(
                    module = secondModule,
                    configurations = listOf(
                        ConfigurationDependencies(
                            id = "compileClasspath",
                            dependencies = listOf(secondDependency)
                        ),
                    )
                )
            )
        )
    }

}
