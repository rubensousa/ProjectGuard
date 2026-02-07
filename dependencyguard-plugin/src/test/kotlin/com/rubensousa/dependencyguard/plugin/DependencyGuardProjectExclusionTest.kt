package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardProjectExclusionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `module included in exclusions should be flagged as excluded`() {
        // given
        val spec = dependencyGuard {
            restrictAll {
                deny(":other") {
                    except(":domain")
                }
            }
        }

        // then
        val violations = restrictionChecker.findViolations(
            modulePath = ":domain",
            dependencyPath = ":other:b",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:b",
                isExcluded = true
            )
        )
    }

    @Test
    fun `module not included in exclusions should be restricted`() {
        // given
        val spec = dependencyGuard {
            restrictAll {
                deny(":other") {
                    except(":other:b")
                }
            }
        }
        // then
        val violations = restrictionChecker.findViolations(
            modulePath = ":domain",
            dependencyPath = ":other:a",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:a",
                isExcluded = false
            )
        )
    }

    @Test
    fun `child module of excluded module should be flagged as excluded`() {
        // given
        val spec = dependencyGuard {
            restrictAll {
                deny(":other") {
                    except(":domain:a")
                }
            }
        }
        // then
        val violations = restrictionChecker.findViolations(
            modulePath = ":domain:a:c",
            dependencyPath = ":other",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain:a:c",
                dependencyPath = ":other",
                isExcluded = true
            )
        )
    }
}
