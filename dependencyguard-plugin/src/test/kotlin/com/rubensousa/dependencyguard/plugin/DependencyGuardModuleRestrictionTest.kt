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

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyPath = ":other:a",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":other:a",
                isSuppressed = false
            )
        )
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyPath = ":other",
                spec = spec
            )
        ).isEmpty()
    }

    @Test
    fun `child module is restricted because its parent is also restricted`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":other")
            }
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyPath = ":other:a",
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

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":another",
            dependencyPath = ":legacy",
            spec = spec
        )

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":legacy")
            }
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
    fun `there is a restriction for a match of a child`() {
        // given
        val spec = dependencyGuard {
            restrictModule(":domain") {
                deny(":legacy")
            }
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a",
            dependencyPath = ":legacy",
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
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
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyPath = ":legacy",
            spec = spec
        )

        // then
        assertThat(violations).isEmpty()
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

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyPath = ":legacy",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":legacy",
                isSuppressed = false
            )
        )
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyPath = ":deprecated",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":deprecated",
                isSuppressed = false
            )
        )
    }

}
