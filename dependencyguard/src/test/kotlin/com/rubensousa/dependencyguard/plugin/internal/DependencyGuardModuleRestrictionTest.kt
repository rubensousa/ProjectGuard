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

class DependencyGuardModuleRestrictionTest {

    private val suppressionMap = SuppressionMap()
    private val restrictionChecker = RestrictionChecker(suppressionMap)

    @Test
    fun `module is restricted to concrete child but not its parent`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":other:a")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":other:a")
            addDependency(":domain:a", ":other")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":other:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `child module is restricted because its parent is also restricted`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":other")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":other:a")
        }
        
        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":other:a",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is no restriction if input module is not the restricted one`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":legacy")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
            addDependency(":another", ":legacy")
        }
        
        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":another",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(matches).isEmpty()
    }

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":legacy")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain", ":legacy")
        }

        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(matches).containsExactly(
            RestrictionMatch(
                module = ":domain",
                dependency = ":legacy",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is a restriction for a match of a child`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":legacy")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy")
        }

        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain:a",
            dependencyGraph = graph,
            spec = spec
        )

        // then
        assertThat(matches).containsExactly(
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":legacy",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `there is no restriction by default`() {
        // given
        val spec = dependencyGuard {}

        // when
        val matches = restrictionChecker.findMatches(
            modulePath = ":domain",
            dependencyGraph = buildDependencyGraph {
                addDependency(":domain", ":legacy")
            },
            spec = spec
        )

        // then
        assertThat(matches).isEmpty()
    }

    @Test
    fun `multiple restrictions are found`() {
        // given
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":legacy")
            }
            guard(":domain:a") {
                deny(":deprecated")
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy")
            addDependency(":domain:a", ":deprecated")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":legacy",
                isSuppressed = false
            ),
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":deprecated",
                isSuppressed = false
            )
        )
    }

    @Test
    fun `restriction contains reason that was set`() {
        // given
        val reason = "Legacy should not be used"
        val spec = dependencyGuard {
            guard(":domain") {
                deny(":legacy") {
                    reason(reason)
                }
            }
        }
        val graph = buildDependencyGraph {
            addDependency(":domain:a", ":legacy:a")
        }

        // then
        assertThat(
            restrictionChecker.findMatches(
                modulePath = ":domain:a",
                dependencyGraph = graph,
                spec = spec
            )
        ).containsExactly(
            RestrictionMatch(
                module = ":domain:a",
                dependency = ":legacy:a",
                reason = reason,
                isSuppressed = false
            ),
        )
    }

}
