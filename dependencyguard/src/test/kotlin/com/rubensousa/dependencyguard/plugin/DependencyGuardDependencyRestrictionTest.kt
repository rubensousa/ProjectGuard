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

class DependencyGuardDependencyRestrictionTest {

    private val restrictionChecker = RestrictionChecker()

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
            RestrictionMatch(
                module = ":domain",
                dependency = ":legacy",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is a restriction for a child of a restricted dependency`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy:a")
        }

        // when
        val violations = restrictionChecker.findMatches(
            modulePath = ":domain:a",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(violations).containsExactly(
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":legacy:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `multiple restrictions are found`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
            restrictDependency(":deprecated")
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
            addDependency(":domain", ":deprecated")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                module = ":domain",
                dependency = ":legacy",
                isSuppressed = false
            ),
            RestrictionMatch(
                module = ":domain",
                dependency = ":deprecated",
                isSuppressed = false
            )
        )
    }


}
