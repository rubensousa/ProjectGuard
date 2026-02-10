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

class DependencyGuardDependencySuppressionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `module included in exclusions should be flagged as suppressed`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":other") {
                suppress(":domain")
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
    fun `module not included in exclusions should be restricted`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":other") {
                suppress(":other:b")
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
            restrictDependency(":other") {
                suppress(":domain:a")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a:c", ":other")
        }

        // then
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a:c",
            dependencyGraph = graph,
            spec = spec
        )
        assertThat(violations).containsExactly(
            RestrictionMatch(
                module = ":domain:a:c",
                dependency = ":other",
                isSuppressed = true,
            )
        )
    }
}
