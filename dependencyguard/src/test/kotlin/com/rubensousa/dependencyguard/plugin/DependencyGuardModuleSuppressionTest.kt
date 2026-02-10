/*
 * Copyright 2026 Rúben Sousa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            guard(":domain") {
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
            guard(":domain") {
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
            guard(":domain") {
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
            guard(":domain") {
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
