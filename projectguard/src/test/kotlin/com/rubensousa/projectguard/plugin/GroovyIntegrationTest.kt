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

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GroovyIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val pluginRunner = PluginRunner(temporaryFolder)
    private lateinit var rootBuildFile: File

    @Before
    fun setup() {
        rootBuildFile = temporaryFolder.newFile("build.gradle")
        rootBuildFile.writeText(
            """
            plugins {
                id 'com.rubensousa.projectguard'
                id 'jacoco-testkit-coverage'
            }
            subprojects {
                apply plugin: 'java-library'
                apply plugin: 'com.rubensousa.projectguard'
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `check fails for guard restriction`() {
        // given
        val module = "consumer"
        val dependency = "libraryA"
        val reason = "Bla bla"
        pluginRunner.createModule(module)
        pluginRunner.createModule(dependency)
        rootBuildFile.appendText(
            """
            projectGuard {
                guard(":$module") {
                    deny(":$dependency") {
                        reason("$reason")
                    }
                }
            }
            """.trimIndent()
        )

        // when
        pluginRunner.addDependency(from = module, to = dependency)

        // then
        pluginRunner.assertCheckFails(module)
        pluginRunner.assertTaskOutputContains(reason)
    }

    @Test
    fun `check succeeds for guard restriction that does not match`() {
        // given
        val module = "consumer"
        val dependency = "libraryA"
        pluginRunner.createModule(module)
        pluginRunner.createModule(dependency)
        rootBuildFile.appendText(
            """
            projectGuard {
                guard(":$module") {
                    deny(":another")
                }
            }
            """.trimIndent()
        )

        // when
        pluginRunner.addDependency(from = module, to = dependency)

        // then
        pluginRunner.assertCheckSucceeds(module)
        pluginRunner.assertTaskOutputContains("No fatal matches found")
    }

}
