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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TaskHtmlReportTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val plugin = DependencyPluginSimulator(temporaryFolder)

    @Test
    fun `html report is generated for module`() {
        // given
        val moduleId = ":domain"
        val fatalModuleId = ":legacy"
        plugin.dumpDependencies(moduleId) { addDependency(moduleId, fatalModuleId) }
        plugin.dumpAggregateDependencies()
        // Empty spec to allow the invalid combination
        val spec = projectGuard {}
        plugin.dumpRestrictions(moduleId, spec)

        // when
        val outputDir = plugin.htmlReport(moduleId)

        // then
        assertThat(File(outputDir, "index.html").exists()).isTrue()
        assertThat(File(outputDir, "script.js").exists()).isTrue()
        assertThat(File(outputDir, "style.css").exists()).isTrue()
    }

}
