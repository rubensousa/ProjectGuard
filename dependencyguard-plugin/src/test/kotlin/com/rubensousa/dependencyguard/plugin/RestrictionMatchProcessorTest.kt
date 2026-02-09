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
