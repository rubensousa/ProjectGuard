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
        val dependencies = graph.getAllDependencies(consumer)

        // then
        assertThat(dependencies).containsExactly(
            directDependencyA,
            transitiveDependencyB,
            transitiveDependencyC,
            transitiveDependencyD,
            transitiveDependencyE
        )
    }

}
