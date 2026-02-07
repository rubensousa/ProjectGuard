package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardDependencyRestrictionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyPath = ":legacy",
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is a restriction for a child of a restricted dependency`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a",
            dependencyPath = ":legacy:a",
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":legacy:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `multiple restrictions are found`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
            restrictDependency(":deprecated")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                isSuppressed = false
            )
        )
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyPath = ":deprecated",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":deprecated",
                isSuppressed = false
            )
        )
    }

}
