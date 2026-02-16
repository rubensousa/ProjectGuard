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
import com.rubensousa.projectguard.plugin.internal.DependencyGraph
import com.rubensousa.projectguard.plugin.internal.report.ConfigurationDependencies
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphDump
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphModuleDump
import com.rubensousa.projectguard.plugin.internal.task.DependencyDumpExecutor
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TaskDependencyDumpTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val inputModule = "module"
    private val dependencies = mutableListOf<DependencyGraph>()
    private lateinit var outputFile: File
    private lateinit var executor: DependencyDumpExecutor

    @Before
    fun setup() {
        outputFile = temporaryFolder.newFile("dependencies.json")
        executor = DependencyDumpExecutor(
            moduleId = inputModule,
            outputFile = outputFile,
            dependencyGraphs = dependencies,
        )
    }

    @Test
    fun `dependency dump is created`() {
        // given
        val firstDependency = "domain:a"
        val secondDependency = "domain:b"
        dependencies.add(
            DependencyGraph(
                configurationId = "implementation",
            ).apply {
                addDependency(inputModule, firstDependency)
            },
        )
        dependencies.add(
            DependencyGraph(
                configurationId = "testImplementation",
            ).apply {
                addDependency(inputModule, secondDependency)
            },
        )

        // when
        executor.execute()

        // then
        val dump = Json.decodeFromString<DependencyGraphDump>(outputFile.readText())
        assertThat(dump.modules).isEqualTo(
            listOf(
                DependencyGraphModuleDump(
                    module = inputModule,
                    configurations = listOf(
                        ConfigurationDependencies(
                            id = "implementation",
                            dependencies = listOf(firstDependency)
                        ),
                        ConfigurationDependencies(
                            id = "testImplementation",
                            dependencies = listOf(secondDependency)
                        )
                    )
                )
            )
        )
    }

}
