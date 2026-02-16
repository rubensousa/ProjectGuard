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
import kotlin.test.Test

class DependencyGraphTest {

    private val graph = DependencyGraph(
        configurationId = "implementation"
    )

    @Test
    fun `transitive dependencies are returned`() {
        // given
        val consumer = "consumer"
        val directDependencyA = "dependencyA"
        graph.addDependency(module = consumer, dependency = directDependencyA)
        // A -> B
        val transitiveDependencyB = "dependencyB"
        graph.addDependency(module = directDependencyA, dependency = transitiveDependencyB)
        // A -> C
        val transitiveDependencyC = "dependencyC"
        graph.addDependency(module = directDependencyA, dependency = transitiveDependencyC)
        // B -> D
        val transitiveDependencyD = "dependencyD"
        graph.addDependency(module = transitiveDependencyB, dependency = transitiveDependencyD)
        // C -> E
        val transitiveDependencyE = "dependencyE"
        graph.addDependency(module = transitiveDependencyC, dependency = transitiveDependencyE)

        // when
        val dependencies = graph.getAllDependencies(consumer).map { it.id }

        // then
        assertThat(dependencies).containsExactly(
            directDependencyA,
            transitiveDependencyB,
            transitiveDependencyC,
            transitiveDependencyD,
            transitiveDependencyE
        )
    }

    @Test
    fun `path to transitive dependency is correct`() {
        // given
        val consumer = "consumer"
        val directDependencyA = "dependencyA"
        graph.addDependency(module = consumer, dependency = directDependencyA)
        // A -> B
        val transitiveDependencyB = "dependencyB"
        graph.addDependency(module = directDependencyA, dependency = transitiveDependencyB)
        // B -> C
        val transitiveDependencyC = "dependencyC"
        graph.addDependency(module = transitiveDependencyB, dependency = transitiveDependencyC)
        // C -> D
        val transitiveDependencyD = "dependencyD"
        graph.addDependency(module = transitiveDependencyC, dependency = transitiveDependencyD)

        // when
        val dependencies = graph.getAllDependencies(consumer)

        // then
        val dependency = dependencies.find { it.id == transitiveDependencyB }!! as TransitiveDependency
        assertThat(dependency.path)
            .isEqualTo(listOf(directDependencyA, transitiveDependencyB))
    }

    @Test
    fun `shortest path to transitive dependency is returned`() {
        // given
        val consumer = "consumer"
        val directDependencyA = "dependencyA"
        graph.addDependency(module = consumer, dependency = directDependencyA)
        // A -> B
        val transitiveDependencyB = "dependencyB"
        graph.addDependency(module = directDependencyA, dependency = transitiveDependencyB)
        // B -> C
        val transitiveDependencyC = "dependencyC"
        graph.addDependency(module = transitiveDependencyB, dependency = transitiveDependencyC)
        // C -> D
        val transitiveDependencyD = "dependencyD"
        graph.addDependency(module = transitiveDependencyC, dependency = transitiveDependencyD)
        // A -> D
        graph.addDependency(module = directDependencyA, dependency = transitiveDependencyD)

        // when
        val dependencies = graph.getAllDependencies(consumer)

        // then
        val dependency = dependencies.find { it.id == transitiveDependencyD }!! as TransitiveDependency
        assertThat(dependency.path)
            .isEqualTo(listOf(directDependencyA, transitiveDependencyD))
    }

}
