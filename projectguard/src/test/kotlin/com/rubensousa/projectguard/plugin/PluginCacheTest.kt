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
import org.junit.Ignore
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
    fun `outputs from projectGuardDependencyDump are up-to-date on next execution`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val libraryDependencyTask = ":b:projectGuardDependencyDump"
        pluginRunner.runTask(libraryDependencyTask)

        // when
        val nextResult = pluginRunner.runTask(libraryDependencyTask)

        // then
        assertThat(nextResult).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `outputs from projectGuardDependencyDump are re-used from cache`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val libraryDependencyTask = ":b:projectGuardDependencyDump"
        pluginRunner.runTask(libraryDependencyTask)

        // when
        pluginRunner.deleteBuildDirs()
        val result = pluginRunner.runTask(libraryDependencyTask)

        // then
        assertThat(result).isEqualTo(TaskOutcome.FROM_CACHE)
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
        pluginRunner.deleteBuildDirs()
        val result = pluginRunner.runTask(libraryDependencyTask)

        // then
        assertThat(result).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `outputs from projectGuardAggregateDependencyDump are up-to-date on next execution`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateDependencyDump"
        pluginRunner.runTask(task)

        // when
        val result = pluginRunner.runTask(task)

        // then
        assertThat(result).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Ignore("Not working for now")
    @Test
    fun `outputs from projectGuardAggregateDependencyDump are re-used from cache`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateDependencyDump"
        pluginRunner.runTask(task)

        // when
        pluginRunner.deleteBuildDirs()
        val result = pluginRunner.runTask(task)

        // then
        assertThat(result).isEqualTo(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `outputs from projectGuardAggregateDependencyDump are not re-used if dependencies changed`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.createModule("c")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateDependencyDump"
        pluginRunner.runTask(task)

        // when
        pluginRunner.addDependency(from = "b", to = "c")

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `outputs from projectGuardRestrictionDump are up-to-date if nothing changed`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":a:projectGuardRestrictionDump"
        pluginRunner.runTask(task)

        // when
        val result = pluginRunner.runTask(task)

        // then
        assertThat(result).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `outputs from projectGuardRestrictionDump are cached`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":a:projectGuardRestrictionDump"
        pluginRunner.runTask(task)

        // when
        pluginRunner.deleteBuildDirs()
        val result = pluginRunner.runTask(task)

        // then
        assertThat(result).isEqualTo(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `outputs from projectGuardRestrictionDump are not re-used if dependencies changed`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.createModule("c")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":b:projectGuardRestrictionDump"
        pluginRunner.runTask(task)

        // when
        pluginRunner.addDependency(from = "b", to = "c")
        pluginRunner.deleteBuildDirs()

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `outputs from projectGuardRestrictionDump are not re-used if rules changed`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":b:projectGuardRestrictionDump"
        pluginRunner.runTask(task)

        // when
        rootBuildFile.appendText(
            """
            projectGuard {
                restrictDependency(":a")
            }
            """.trimIndent()
        )
        pluginRunner.deleteBuildDirs()

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `outputs from projectGuardAggregateRestrictionDump are re-used if nothing changed`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateRestrictionDump"

        // when
        pluginRunner.runTask(task)

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `outputs from projectGuardAggregateRestrictionDump are re-used if dependencies changed but there are no restrictions`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.createModule("c")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateRestrictionDump"
        pluginRunner.runTask(task)

        // when
        pluginRunner.addDependency(from = "b", to = "c")

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `outputs from projectGuardAggregateRestrictionDump are re-used if rules changed but there are not restrictions`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateRestrictionDump"
        pluginRunner.runTask(task)

        // when
        rootBuildFile.appendText(
            """
            projectGuard {
                restrictDependency(":a")
            }
            """.trimIndent()
        )

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `outputs from projectGuardAggregateRestrictionDump are not re-used if new rules trigger new restrictions`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        pluginRunner.addDependency(from = "a", to = "b")
        val task = ":projectGuardAggregateRestrictionDump"
        pluginRunner.runTask(task)

        // when
        rootBuildFile.appendText(
            """
            projectGuard {
                restrictDependency(":b")
            }
            """.trimIndent()
        )

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `outputs from projectGuardAggregateRestrictionDump are not re-used if new dependencies trigger new restrictions`() {
        // given
        pluginRunner.createModule("a")
        pluginRunner.createModule("b")
        val task = ":projectGuardAggregateRestrictionDump"
        rootBuildFile.appendText(
            """
            projectGuard {
                restrictDependency(":b")
            }
            """.trimIndent()
        )
        pluginRunner.runTask(task)

        // when
        pluginRunner.addDependency(from = "a", to = "b")

        // then
        assertThat(pluginRunner.runTask(task)).isEqualTo(TaskOutcome.SUCCESS)
    }

}
