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
    private val modules = mutableListOf<String>()

    fun createModule(name: String) {
        temporaryFolder.newFolder(name)
        temporaryFolder.newFile("$name/build.gradle.kts")
        settingsFile.appendText("\ninclude(\":$name\")")
        modules.add(name)
    }

    fun deleteBuildDirs() {
        modules.forEach { module ->
            temporaryFolder.getRoot()
                .resolve("$module/build/")
                .deleteRecursively()
        }
        temporaryFolder.getRoot()
            .resolve("build")
            .deleteRecursively()
    }

    fun assertProjectGuardCheckFails(module: String) {
        val task = getProjectGuardCheckTask(module)
        val result = gradleRunner.withArguments(task).buildAndFail()
        assertThat(result.task(task)!!.outcome).isEqualTo(TaskOutcome.FAILED)
        lastResult = result
    }

    fun assertProjectGuardCheckSucceeds(module: String) {
        val task = getProjectGuardCheckTask(module)
        val result = gradleRunner.withArguments(task).build()
        assertThat(result.task(task)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        lastResult = result
    }

    fun assertAssembleTaskFails(module: String) {
        val result = gradleRunner.withArguments(":$module:assemble").buildAndFail()
        assertThat(result.task(getProjectGuardCheckTask(module))!!.outcome).isEqualTo(TaskOutcome.FAILED)
        lastResult = result
    }

    fun assertCheckTaskFails(module: String) {
        val result = gradleRunner.withArguments(":$module:check").buildAndFail()
        assertThat(result.task(getProjectGuardCheckTask(module))!!.outcome).isEqualTo(TaskOutcome.FAILED)
        lastResult = result
    }

    fun assertTaskOutputContains(message: String) {
        assertThat(lastResult!!.output).contains(message)
    }

    fun runTask(task: String): TaskOutcome {
        val result = gradleRunner.withArguments("--build-cache", task).build()
        lastResult = result
        return result.task(task)!!.outcome
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

    private fun getProjectGuardCheckTask(module: String): String {
        return ":$module:projectGuardCheck"
    }

}
