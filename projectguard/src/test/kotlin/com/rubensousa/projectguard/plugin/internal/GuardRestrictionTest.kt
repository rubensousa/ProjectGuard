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

package com.rubensousa.projectguard.plugin.internal

import com.google.common.truth.Truth.assertThat
import com.rubensousa.projectguard.plugin.buildDependencyGraph
import com.rubensousa.projectguard.plugin.projectGuard
import kotlin.test.Test

class GuardRestrictionTest {

    private val graph = DependencyGraph(
        configurationId = "implementation"
    )
    private val finder = DependencyRestrictionFinder()

    @Test
    fun `module is restricted to concrete child but not its sibling`() {
        // given
        val spec = projectGuard {
            guard(":domain") {
                deny(":other:a")
            }
        }
        graph.apply {
            addInternalDependency(":domain:a", ":other:a")
            addInternalDependency(":domain:a", ":other:c")
        }

        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(DirectDependencyRestriction(":other:a"))
    }

    @Test
    fun `child module is restricted because its parent is also restricted`() {
        // given
        val spec = projectGuard {
            guard(":domain") {
                deny(":other")
            }
        }
        graph.apply {
            addInternalDependency(":domain:a", ":other:a")
        }

        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(DirectDependencyRestriction(":other:a"))
    }

    @Test
    fun `there is no restriction if input module is not the restricted one`() {
        // given
        val spec = projectGuard {
            guard(":domain") {
                deny(":legacy")
            }
        }
        graph.apply {
            addInternalDependency(":domain", ":legacy")
            addInternalDependency(":another", ":legacy")
        }

        // when
        val restrictions = finder.find(
            moduleId = ":another",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).isEmpty()
    }

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = projectGuard {
            guard(":domain") {
                deny(":legacy")
            }
        }
        graph.apply {
            addInternalDependency(":domain", ":legacy")
        }

        // when
        val restrictions = finder.find(
            moduleId = ":domain",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(DirectDependencyRestriction(":legacy"))
    }

    @Test
    fun `there is a restriction for a match of a child`() {
        // given
        val spec = projectGuard {
            guard(":domain") {
                deny(":legacy")
            }
        }
        graph.addInternalDependency(":domain:a", ":legacy:a")

        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(DirectDependencyRestriction(":legacy:a"))
    }

    @Test
    fun `there is no restriction by default`() {
        // given
        val spec = projectGuard {}

        // when
        val restrictions = finder.find(
            moduleId = ":domain",
            graph = buildDependencyGraph {
                addInternalDependency(":domain", ":legacy")
            },
            spec = spec
        )

        // then
        assertThat(restrictions).isEmpty()
    }

    @Test
    fun `multiple restrictions are found`() {
        // given
        val spec = projectGuard {
            guard(":domain") {
                deny(":legacy")
            }
            guard(":domain:a") {
                deny(":deprecated")
            }
        }
        graph.apply {
            addInternalDependency(":domain:a", ":legacy")
            addInternalDependency(":domain:a", ":deprecated")
        }

        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(
            DirectDependencyRestriction(":legacy"),
            DirectDependencyRestriction(":deprecated"),
        )
    }

    @Test
    fun `restriction contains reason that was set`() {
        // given
        val reason = "Legacy should not be used"
        val spec = projectGuard {
            guard(":domain") {
                deny(":legacy") {
                    reason(reason)
                }
            }
        }
        graph.addInternalDependency(":domain:a", ":legacy:a")


        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(DirectDependencyRestriction(":legacy:a", reason))
    }

}
