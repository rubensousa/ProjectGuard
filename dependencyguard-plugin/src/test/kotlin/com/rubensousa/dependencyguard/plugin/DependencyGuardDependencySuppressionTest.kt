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
        val graph = buildDependencyGraph {
            addDependency(":domain", ":other:b")
        }

        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
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
        val graph = buildDependencyGraph {
            addDependency(":domain", ":other:a")
        }

        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
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
        val graph = buildDependencyGraph {
            addDependency(":domain:a:c", ":other")
        }

        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a:c",
            dependencyGraph = graph,
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
