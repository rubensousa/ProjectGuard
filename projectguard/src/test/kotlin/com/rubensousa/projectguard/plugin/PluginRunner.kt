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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.rules.TemporaryFolder

class PluginRunner(
    private val temporaryFolder: TemporaryFolder,
) {

    private val settingsFile by lazy { temporaryFolder.newFile("settings.gradle.kts") }
    private val gradleRunner by lazy {
        GradleRunner.create()
            .withProjectDir(temporaryFolder.root)
            .withPluginClasspath()
            .withGradleVersion("8.13")
    }
    private var lastResult: BuildResult? = null

    fun createModule(name: String) {
        temporaryFolder.newFolder(name)
        temporaryFolder.newFile("$name/build.gradle.kts")
        settingsFile.appendText("\ninclude(\":$name\")")
    }

    fun assertCheckFails(module: String) {
        val task = createCheckTask(module)
        val result = gradleRunner.withArguments(task).buildAndFail()
        assertThat(result.task(task)!!.outcome).isEqualTo(TaskOutcome.FAILED)
        lastResult = result
    }

    fun assertCheckFailureContains(module: String, message: String) {
        assertCheckFails(module)
        assertThat(lastResult!!.output).contains(message)
    }

    fun assertCheckSucceeds(module: String) {
        val task = createCheckTask(module)
        val result = gradleRunner.withArguments(task).build()
        assertThat(result.task(task)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        lastResult = result
    }

    fun addDependency(from: String, to: String, configuration: String = "implementation") {
        temporaryFolder.getRoot().resolve("$from/build.gradle.kts").appendText(
            """
            dependencies {
                $configuration(project(":$to"))
            }
            """.trimIndent()
        )
    }

    private fun createCheckTask(module: String): String {
        return ":$module:projectGuardCheck"
    }

}
