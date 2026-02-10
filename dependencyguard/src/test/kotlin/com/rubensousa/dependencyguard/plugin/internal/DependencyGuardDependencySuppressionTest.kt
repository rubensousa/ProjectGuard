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

package com.rubensousa.dependencyguard.plugin.internal

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.buildDependencyGraph
import com.rubensousa.dependencyguard.plugin.dependencyGuard
import kotlin.test.Test

class DependencyGuardDependencySuppressionTest {

    private val suppressionMap = SuppressionMap()
    private val restrictionChecker = RestrictionChecker(suppressionMap)

    @Test
    fun `module included in suppressions should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":other")
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
            restrictDependency(":other")
        }
        suppressionMap.add(":domain", ":other:b")
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
            restrictDependency(":other")
        }
        suppressionMap.add(":domain:a:c", ":other")
        val graph = buildDependencyGraph {
            addDependency(":domain:a:c", ":other")
        }

        // then
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain:a:c",
            dependencyGraph = graph,
            spec = spec
        )
        assertThat(matches).containsExactly(
            RestrictionMatch(
                module = ":domain:a:c",
                dependency = ":other",
                isSuppressed = true,
            )
        )
    }
}
