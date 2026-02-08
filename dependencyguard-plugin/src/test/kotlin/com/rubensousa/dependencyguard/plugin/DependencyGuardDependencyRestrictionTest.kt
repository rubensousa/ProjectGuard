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
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
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
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy:a")
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a",
            dependencyGraph = graph,
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
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
            addDependency(":domain", ":deprecated")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                isSuppressed = false
            ),
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":deprecated",
                isSuppressed = false
            )
        )
    }


}
