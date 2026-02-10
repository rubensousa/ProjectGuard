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
import com.rubensousa.dependencyguard.plugin.internal.SuppressionMap
import kotlin.test.Test

class DependencyGuardModuleSuppressionTest {

    private val suppressionMap = SuppressionMap()
    private val restrictionChecker = RestrictionChecker(suppressionMap)

    @Test
    fun `module included in suppression should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":other:b")
            }
        }
        suppressionMap.add(":domain", ":other:b")
        val graph = buildDependencyGraph {
            addDependency(":domain", ":other:b")
        }

        // then
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )
        assertThat(matches).containsExactly(
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
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )
        assertThat(matches).containsExactly(
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
                deny(":other:a")
            }
        }
        suppressionMap.add(":domain", ":other:a:c", "Suppression reason")
        val graph = buildDependencyGraph {
            addDependency(":domain", ":other:a:c")
        }

        // then
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )
        assertThat(matches).containsExactly(
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
                deny(":other:b")
                deny(":other:c")
            }
        }
        suppressionMap.add(":domain", ":other:b")
        suppressionMap.add(":domain", ":other:c")
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
