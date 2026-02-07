package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardDependencySuppressionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `module included in exclusions should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":other") {
                suppress(":domain")
            }
        }

        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyPath = ":other:b",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:b",
                isSuppressed = true
            )
        )
    }

    @Test
    fun `module not included in exclusions should be restricted`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":other") {
                suppress(":other:b")
            }
        }
        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyPath = ":other:a",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `child module of suppressed module should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":other") {
                suppress(":domain:a")
            }
        }
        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a:c",
            dependencyPath = ":other",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a:c",
                dependencyPath = ":other",
                isSuppressed = true,
            )
        )
    }
}
