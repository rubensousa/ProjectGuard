package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardModuleRestrictionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `module is restricted to concrete child but not its parent`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":other:a")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":other:a")
            addDependency(":domain:a", ":other")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":other:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `child module is restricted because its parent is also restricted`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":other")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":other:a")
        }
        
        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":other:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is no restriction if input module is not the restricted one`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":legacy")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
            addDependency(":another", ":legacy")
        }
        
        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":another",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(matches).isEmpty()
    }

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":legacy")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
        }

        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(matches).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is a restriction for a match of a child`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":legacy")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy")
        }

        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain:a",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(matches).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":legacy",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is no restriction by default`() {
        // given
        val spec = dependencyGuard {}

        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = buildDependencyGraph {
                addDependency(":domain", ":legacy")
            },
            spec = spec
        )

        // then
        assertThat(matches).isEmpty()
    }

    @Test
    fun `multiple restrictions are found`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":legacy")
            }
            restrictModule(":domain:a") {
                deny(":deprecated")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy")
            addDependency(":domain:a", ":deprecated")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":legacy",
                isSuppressed = false
            ),
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":deprecated",
                isSuppressed = false
            )
        )
    }

}
