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

class PluginIntegrationTest {

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
    fun `transitive dependencies are found and cause check to fail`() {
        // given
        pluginRunner.createModule("consumer")
        pluginRunner.createModule("libraryA")
        pluginRunner.createModule("libraryB")

        rootBuildFile.appendText(
            """
            projectGuard {
                guard(":consumer") {
                    deny(":libraryB")
                }
            }
            """.trimIndent()
        )

        // when
        pluginRunner.addDependency(from = "consumer", to = "libraryA")
        pluginRunner.addDependency(from = "libraryA", to = "libraryB")

        // then
        pluginRunner.assertCheckFails("consumer")
    }

    @Test
    fun `direct dependencies are found and cause check to fail`() {
        // given
        pluginRunner.createModule("consumer")
        pluginRunner.createModule("library")

        rootBuildFile.appendText(
            """
            projectGuard {
                restrictDependency(":library")
            }
            """.trimIndent()
        )

        // when
        pluginRunner.addDependency(from = "consumer", to = "library")

        // then
        pluginRunner.assertCheckFails("consumer")
    }

    @Test
    fun `check task succeeds when no matches are found`() {
        // given
        pluginRunner.createModule("consumer")
        pluginRunner.createModule("library")

        rootBuildFile.appendText(
            """
            projectGuard {
                restrictDependency(":another")
            }
            """.trimIndent()
        )

        // when
        pluginRunner.addDependency(from = "consumer", to = "library")

        // then
        pluginRunner.assertCheckSucceeds("consumer")
    }
}
