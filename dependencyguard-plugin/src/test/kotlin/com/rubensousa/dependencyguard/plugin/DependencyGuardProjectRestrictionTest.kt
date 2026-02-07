package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardProjectRestrictionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            restrictAll {
                deny(":legacy")
            }
        }

        // when
        val violations = restrictionChecker.findViolations(
            modulePath = ":domain",
            dependencyPath = ":legacy",
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                isExcluded = false
            )
        )
    }

    @Test
    fun `there is a restriction for a child of a restricted dependency`() {
        // given
        val spec = dependencyGuard {
            restrictAll {
                deny(":legacy")
            }
        }

        // when
        val violations = restrictionChecker.findViolations(
            modulePath = ":domain:a",
            dependencyPath = ":legacy:a",
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a",
                dependencyPath = ":legacy:a",
                isExcluded = false
            )
        )
    }

    @Test
    fun `multiple restrictions are found`() {
        // given
        val spec = dependencyGuard {
            restrictAll {
                deny(":legacy")
            }
            restrictAll {
                deny(":deprecated")
            }
        }

        // then
        assertThat(
            restrictionChecker.findViolations(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":legacy",
                isExcluded = false
            )
        )
        assertThat(
            restrictionChecker.findViolations(
                modulePath = ":domain",
                dependencyPath = ":deprecated",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":deprecated",
                isExcluded = false
            )
        )
    }

}
