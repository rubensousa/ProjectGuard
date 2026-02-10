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

package com.rubensousa.dependencyguard.plugin.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test

class YamlProcessorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val yamlProcessor = YamlProcessor()

    @Test
    fun `suppression list is written and parsed back`() {
        // given
        val suppressionMap = SuppressionMap()
        suppressionMap.add("module", "dependency")
        suppressionMap.add("anothermodule", "anotherdependency")
        val configuration = suppressionMap.getConfiguration()
        val file = temporaryFolder.newFile("suppressions.yml")

        // when
        yamlProcessor.write(file, configuration)

        // then
        val parsedConfiguration = yamlProcessor.parse(file, SuppressionConfiguration::class.java)
        assertThat(parsedConfiguration).isEqualTo(configuration)
    }


}
