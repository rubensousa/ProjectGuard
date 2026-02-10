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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DependencyGuardIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var rootBuildFile: File
    private lateinit var settingsFile: File
    private lateinit var gradleRunner: GradleRunner
    private val checkTask = ":consumer:dependencyGuardCheck"

    @Before
    fun setup() {
        settingsFile = temporaryFolder.newFile("settings.gradle.kts")
        rootBuildFile = temporaryFolder.newFile("build.gradle.kts")
        gradleRunner = GradleRunner.create()
            .withProjectDir(temporaryFolder.root)
            .withPluginClasspath()
            .withGradleVersion("8.13")
        rootBuildFile.writeText(
            """
            plugins {
                id("com.rubensousa.dependencyguard") apply true
            }
            subprojects {
                apply(plugin = "java-library")
                apply(plugin = "com.rubensousa.dependencyguard")
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `transitive dependencies are found and cause check to fail`() {
        // given
        createModule("consumer")
        createModule("libraryA")
        createModule("libraryB")

        rootBuildFile.appendText(
            """
            dependencyGuard {
                guard(":consumer") {
                    deny(":libraryB")
                }
            }
            """.trimIndent()
        )

        addDependency(from = "consumer", to = "libraryA")
        addDependency(from = "libraryA", to = "libraryB")

        // when
        val result = runGradleTask(expectSuccess = false)

        // then
        assertThat(result.task(checkTask)?.outcome!!).isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `direct dependencies are found and cause check to fail`() {
        // given
        createModule("consumer")
        createModule("library")

        rootBuildFile.appendText(
            """
            dependencyGuard {
                restrictDependency(":library")
            }
            """.trimIndent()
        )

        addDependency(from = "consumer", to = "library")

        // when
        val result = runGradleTask(expectSuccess = false)

        // then
        assertThat(result.task(checkTask)?.outcome!!).isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `check task succeeds when no violations are found`() {
        // given
        createModule("consumer")
        createModule("library")

        rootBuildFile.appendText(
            """
            dependencyGuard {
                restrictDependency(":another")
            }
            """.trimIndent()
        )

        addDependency(from = "consumer", to = "library")

        // when
        val result = runGradleTask(expectSuccess = true)

        // then
        assertThat(result.task(checkTask)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    private fun runGradleTask(expectSuccess: Boolean): BuildResult {
        val runner = gradleRunner.withArguments(checkTask)
        return if (expectSuccess) {
            runner.build()
        } else {
            runner.buildAndFail()
        }
    }

    private fun createModule(name: String) {
        temporaryFolder.newFolder(name)
        temporaryFolder.newFile("$name/build.gradle.kts")
        settingsFile.appendText("\ninclude(\":$name\")")
    }

    private fun addDependency(from: String, to: String, configuration: String = "implementation") {
        temporaryFolder.getRoot().resolve("$from/build.gradle.kts").appendText(
            """
            dependencies {
                $configuration(project(":$to"))
            }
            """.trimIndent()
        )
    }

}
