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

class DependencyRestrictionTest {

    private val graph = DependencyGraph(
        configurationId = "implementation"
    )
    private val finder = DependencyRestrictionFinder()

    @Test
    fun `there is a restriction for a direct match`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }
        graph.addDependency(":domain", ":legacy")

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
    fun `there is a restriction for a child of a restricted group`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }
        graph.addDependency(":domain:a", ":legacy:a")

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

        // when
        val restrictions = finder.find(
            moduleId = ":domain",
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
    fun `there is a restriction for a transitive dependency`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy")
        }
        graph.addDependency(":data:a", ":domain:a")
        graph.addDependency(":domain:a", ":legacy:a")

        // when
        val restrictions = finder.find(
            moduleId = ":data:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(
            TransitiveDependencyRestriction(
                dependencyId = ":legacy:a",
                pathToDependency = listOf(
                    ":domain:a",
                    ":legacy:a"
                )
            )
        )
    }

    @Test
    fun `restriction contains reason that was set`() {
        // given
        val reason = "Legacy should not be used"
        val spec = dependencyGuard {
            restrictDependency(":legacy") {
                reason("Legacy should not be used")
                allow(":legacy")
            }
        }
        graph.addDependency(":domain:a", ":legacy:a")

        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(
            DirectDependencyRestriction(
                dependencyId = ":legacy:a",
                reason = reason,
            ),
        )
    }

    @Test
    fun `duplicate restrictions just return a single match`() {
        // given
        val spec = dependencyGuard {
            restrictDependency(":legacy") {
                allow(":legacy")
            }
            restrictDependency(":legacy:a") {
                allow(":legacy")
            }
        }
        graph.addDependency(":domain:a", ":legacy:a")

        // when
        val restrictions = finder.find(
            moduleId = ":domain:a",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(DirectDependencyRestriction(":legacy:a"))
    }

}
