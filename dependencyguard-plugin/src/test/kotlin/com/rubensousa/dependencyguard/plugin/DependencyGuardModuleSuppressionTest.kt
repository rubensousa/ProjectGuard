package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardModuleSuppressionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `module included in suppression should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                suppress(":other:b")
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
                module = ":domain",
                dependency = ":other:b",
                isSuppressed = true
            )
        )
    }

    @Test
    fun `module not included in suppressions should be restricted`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":other")
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
                module = ":domain",
                dependency = ":other:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `child module of suppressed module should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                suppress(":other:a") {
                    setReason("Suppression reason")
                }
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":other:a:c")
        }

        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                module = ":domain",
                dependency = ":other:a:c",
                isSuppressed = true,
                suppressionReason = "Suppression reason"
            )
        )
    }

    @Test
    fun `multiple suppressions are respected`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                suppress(":other:b")
                suppress(":other:c")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":other:b")
            addDependency(":domain", ":other:c")
        }
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                module = ":domain",
                dependency = ":other:c",
                isSuppressed = true
            ),
            RestrictionMatch(
                module = ":domain",
                dependency = ":other:b",
                isSuppressed = true
            )
        )
    }
}
