package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlin.test.Test

class DependencyGuardModuleExclusionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `module included in exclusions should be flagged as excluded`() {
        // given
        val spec = dependencyGuard {
            restrict(":domain") {
                deny(":other") {
                    except(":other:b")
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
            restrict(":domain") {
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
            restrict(":domain") {
                deny(":other") {
                    except(":other:a")
                }
            }
        }
        // then
        val violations = restrictionChecker.findViolations(
            modulePath = ":domain",
            dependencyPath = ":other:a:c",
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:a:c",
                isExcluded = true
            )
        )
    }

    @Test
    fun `multiple exclusions are respected`() {
        // given
        val spec = dependencyGuard {
            restrict(":domain") {
                deny(":other") {
                    except(":other:b")
                    except(":other:c")
                }
            }
        }
        assertThat(
            restrictionChecker.findViolations(
                modulePath = ":domain",
                dependencyPath = ":other:c",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:c",
                isExcluded = true
            )
        )
        assertThat(
            restrictionChecker.findViolations(
                modulePath = ":domain",
                dependencyPath = ":other:b",
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                modulePath = ":domain",
                dependencyPath = ":other:b",
                isExcluded = true
            )
        )
    }
}
