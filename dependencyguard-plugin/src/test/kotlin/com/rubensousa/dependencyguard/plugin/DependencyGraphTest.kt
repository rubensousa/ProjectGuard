package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.DependencyGraph
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
        val dependencies = graph.getDependencyMatches(consumer).map { it.dependencyId }

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
        val dependencies = graph.getDependencyMatches(consumer)

        // then
        assertThat(dependencies.find { it.dependencyId == transitiveDependencyB }!!.path)
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
        val dependencies = graph.getDependencyMatches(consumer)

        // then
        assertThat(dependencies.find { it.dependencyId == transitiveDependencyD }!!.path)
            .isEqualTo(listOf(directDependencyA, transitiveDependencyD))
    }

}
