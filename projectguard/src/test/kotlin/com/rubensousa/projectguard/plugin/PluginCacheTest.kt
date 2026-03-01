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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PluginCacheTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val pluginRunner = PluginRunner(temporaryFolder)
    private lateinit var rootBuildFile: File

    @Before
    fun setup() {
        rootBuildFile = temporaryFolder.newFile("build.gradle.kts")
        rootBuildFile.writeText(
            """
            plugins {
                id("com.rubensousa.projectguard") apply true
            }
            subprojects {
                apply(plugin = "java-library")
                apply(plugin = "com.rubensousa.projectguard")
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `outputs from projectGuardDependencyDump are re-used`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val libraryDependencyTask = ":b:projectGuardDependencyDump"

        // when
        pluginRunner.runTask(libraryDependencyTask)

        // then
        assertThat(pluginRunner.runTask(libraryDependencyTask)).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `outputs from projectGuardDependencyDump are not re-used if dependencies changed`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.createModule("c")
        pluginRunner.addDependency(from = "a", to = "b")
        val libraryDependencyTask = ":b:projectGuardDependencyDump"
        pluginRunner.runTask(libraryDependencyTask)

        // when
        pluginRunner.addDependency(from = "b", to = "c")

        // then
        assertThat(pluginRunner.runTask(libraryDependencyTask)).isEqualTo(TaskOutcome.SUCCESS)
    }
}
