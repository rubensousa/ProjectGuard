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
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatchProcessor
import kotlin.test.Test

class RestrictionMatchProcessorTest {

    private val processor = RestrictionMatchProcessor()

    @Test
    fun `duplicate matches are excluded`() {
        // given
        val firstMatch = RestrictionMatch(
            module = "module:a",
            dependency = "module:b",
        )
        val secondMatch = firstMatch.copy(
            reason = "Another reason"
        )

        // when
        val processedMatches = processor.process(listOf(firstMatch, secondMatch))

        // then
        assertThat(processedMatches).isEqualTo(listOf(firstMatch))
    }

    @Test
    fun `different matches are included`() {
        // given
        val firstMatch = RestrictionMatch(
            module = "module:a",
            dependency = "module:b",
        )
        val secondMatch = RestrictionMatch(
            module = "module:a",
            dependency = "module:c",
        )

        // when
        val processedMatches = processor.process(listOf(firstMatch, secondMatch))

        // then
        assertThat(processedMatches).isEqualTo(listOf(firstMatch, secondMatch))
    }

}
