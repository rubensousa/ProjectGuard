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

class ModuleRestrictionTest {

    private val graph = DependencyGraph(configurationId = "implementation")
    private val finder = DependencyRestrictionFinder()

    @Test
    fun `restrictModule denies all by default`() {
        // given
        val spec = projectGuard {
            restrictModule(":domain")
        }
        graph.addInternalDependency(":domain", ":legacy")
        graph.addInternalDependency(":domain", ":data")


        // when
        val restrictions = finder.find(
            moduleId = ":domain",
            graph = graph,
            spec = spec
        )

        // then
        assertThat(restrictions).containsExactly(
            DirectDependencyRestriction(":legacy"),
            DirectDependencyRestriction(":data")
        )
    }

    @Test
    fun `there is a restriction for a child of a restricted group`() {
        // given
        val spec = projectGuard {
            restrictModule(":domain")
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
    fun `multiple restrictions are found`() {
        // given
        val spec = projectGuard {
            restrictModule(":domain")
            restrictModule(":data")
        }
        val graph = buildDependencyGraph {
            addInternalDependency(":domain", ":legacy")
            addInternalDependency(":data", ":legacy")
        }

        // when
        val domainRestrictions = finder.find(moduleId = ":domain", graph = graph, spec = spec)
        val dataRestrictions = finder.find(moduleId = ":data", graph = graph, spec = spec)

        // then
        assertThat(domainRestrictions).containsExactly(DirectDependencyRestriction(":legacy"))
        assertThat(dataRestrictions).containsExactly(DirectDependencyRestriction(":legacy"))
    }

    @Test
    fun `there is a restriction for a transitive dependency`() {
        // given
        val spec = projectGuard {
            restrictModule(":domain") {
                allow(":domain")
            }
        }
        graph.addInternalDependency(":domain:a", ":domain:b")
        graph.addInternalDependency(":domain:b", ":legacy:a")

        // when
        val restrictions = finder.find(moduleId = ":domain:a", graph = graph, spec = spec)

        // then
        assertThat(restrictions).containsExactly(
            TransitiveDependencyRestriction(
                dependencyId = ":legacy:a",
                pathToDependency = listOf(
                    ":domain:b",
                    ":legacy:a"
                )
            )
        )
    }

    @Test
    fun `restriction contains reason that was set`() {
        // given
        val reason = "Domain should not have other dependencies"
        val spec = projectGuard {
            restrictModule(":domain") {
                reason(reason)
            }
        }
        graph.addInternalDependency(":domain:a", ":legacy:a")

        // when
        val restrictions = finder.find(moduleId = ":domain:a", graph = graph, spec = spec)

        // then
        assertThat(restrictions).containsExactly(
            DirectDependencyRestriction(
                dependencyId = ":legacy:a",
                reason = reason,
            ),
        )
    }

    @Test
    fun `restriction supports multiple allows`() {
        // given
        val spec = projectGuard {
            restrictModule(":domain") {
                allowModules(listOf(":domain:a", ":domain:b"))
            }
        }

        // Ok
        graph.addInternalDependency(":domain:c", ":domain:a")
        graph.addInternalDependency(":domain:b", ":domain:a")

        // Not ok
        graph.addInternalDependency(":domain:d", ":domain:c")

        // when
        val restrictionsB = finder.find(moduleId = ":domain:b", graph = graph, spec = spec)
        val restrictionsC = finder.find(moduleId = ":domain:c", graph = graph, spec = spec)
        val restrictionsD = finder.find(moduleId = ":domain:d", graph = graph, spec = spec)


        // then
        assertThat(restrictionsB).isEmpty()
        assertThat(restrictionsC).isEmpty()
        assertThat(restrictionsD).containsExactly(
            DirectDependencyRestriction(
                dependencyId = ":domain:c",
            ),
        )
    }

    @Test
    fun `duplicate restrictions just return a single match`() {
        // given
        val spec = projectGuard {
            restrictModule(":domain")
            restrictModule(":domain:a")
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

}
