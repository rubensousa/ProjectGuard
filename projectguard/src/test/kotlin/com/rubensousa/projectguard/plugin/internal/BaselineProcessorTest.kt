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

package com.rubensousa.projectguard.plugin.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test

class BaselineProcessorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val baselineProcessor = BaselineProcessor()

    @Test
    fun `suppression list is written and parsed back`() {
        // given
        val suppressionMap = SuppressionMap()
        suppressionMap.add("module", "dependency")
        suppressionMap.add("anothermodule", "anotherdependency")
        val configuration = suppressionMap.getBaseline()
        val file = temporaryFolder.newFile("suppressions.yml")

        // when
        baselineProcessor.write(file, configuration)

        // then
        val parsedConfiguration = baselineProcessor.parse(file)
        assertThat(parsedConfiguration).isEqualTo(configuration)
    }

    @Test
    fun `suppression without any module dependency is parsed correctly`() {
        // given
        val file = temporaryFolder.newFile("suppressions.yml")
        file.writeText("""
            suppressions:
              module:
        """.trimIndent())

        // when
        val parsedConfiguration = baselineProcessor.parse(file)

        // then
        assertThat(parsedConfiguration.getModuleSuppressions("module")).isEmpty()
    }

    @Test
    fun `suppression without any module is parsed correctly`() {
        // given
        val file = temporaryFolder.newFile("suppressions.yml")
        file.writeText("""
            suppressions:
        """.trimIndent())

        // when
        val parsedConfiguration = baselineProcessor.parse(file)

        // then
        assertThat(parsedConfiguration.suppressions).isEmpty()
    }

}
