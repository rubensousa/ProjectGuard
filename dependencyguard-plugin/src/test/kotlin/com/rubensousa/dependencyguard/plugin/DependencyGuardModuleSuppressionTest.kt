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
    fun `module not included in suppressions should be restricted`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":other")
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
            restrictModule(":domain") {
                suppress(":other:a") {
                    setReason("Suppression reason")
                }
            }
        }
        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyPath = ":other:a:c",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:a:c",
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
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyPath = ":other:c",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:c",
                isSuppressed = true
            )
        )
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyPath = ":other:b",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:b",
                isSuppressed = true
            )
        )
    }
}
