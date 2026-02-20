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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test

class DependencyGraphTest {

    private val graph = DependencyGraph()

    @Test
    fun `transitive dependencies are returned`() {
        // given
        val consumer = "consumer"
        val directDependencyA = "dependencyA"
        graph.addInternalDependency(module = consumer, dependency = directDependencyA)
        // A -> B
        val transitiveDependencyB = "dependencyB"
        graph.addInternalDependency(module = directDependencyA, dependency = transitiveDependencyB)
        // A -> C
        val transitiveDependencyC = "dependencyC"
        graph.addInternalDependency(module = directDependencyA, dependency = transitiveDependencyC)
        // B -> D
        val transitiveDependencyD = "dependencyD"
        graph.addInternalDependency(module = transitiveDependencyB, dependency = transitiveDependencyD)
        // C -> E
        val transitiveDependencyE = "dependencyE"
        graph.addInternalDependency(module = transitiveDependencyC, dependency = transitiveDependencyE)

        // when
        val dependencies = graph.getDependencies(consumer).map { it.id }

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
        graph.addInternalDependency(module = consumer, dependency = directDependencyA)
        // A -> B
        val transitiveDependencyB = "dependencyB"
        graph.addInternalDependency(module = directDependencyA, dependency = transitiveDependencyB)
        // B -> C
        val transitiveDependencyC = "dependencyC"
        graph.addInternalDependency(module = transitiveDependencyB, dependency = transitiveDependencyC)
        // C -> D
        val transitiveDependencyD = "dependencyD"
        graph.addInternalDependency(module = transitiveDependencyC, dependency = transitiveDependencyD)

        // when
        val dependencies = graph.getDependencies(consumer)

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
        graph.addInternalDependency(module = consumer, dependency = directDependencyA)
        // A -> B
        val transitiveDependencyB = "dependencyB"
        graph.addInternalDependency(module = directDependencyA, dependency = transitiveDependencyB)
        // B -> C
        val transitiveDependencyC = "dependencyC"
        graph.addInternalDependency(module = transitiveDependencyB, dependency = transitiveDependencyC)
        // C -> D
        val transitiveDependencyD = "dependencyD"
        graph.addInternalDependency(module = transitiveDependencyC, dependency = transitiveDependencyD)
        // A -> D
        graph.addInternalDependency(module = directDependencyA, dependency = transitiveDependencyD)

        // when
        val dependencies = graph.getDependencies(consumer)

        // then
        val dependency = dependencies.find { it.id == transitiveDependencyD }!! as TransitiveDependency
        assertThat(dependency.path)
            .isEqualTo(listOf(directDependencyA, transitiveDependencyD))
    }

    @Test
    fun `implementation of a test dependency is considered a transitive dependency`() {
        // given
        val consumer = "consumer"
        val consumerDependency = "dependencyA"
        val dependencyOfConsumerDependency = "dependencyB"
        graph.addInternalDependency(
            configurationId = DependencyConfiguration.TEST,
            module = consumer,
            dependency = consumerDependency
        )
        graph.addInternalDependency(
            configurationId = DependencyConfiguration.COMPILE,
            module = consumerDependency,
            dependency = dependencyOfConsumerDependency
        )

        // when
        val dependencies = graph.getDependencies(consumer)

        // then
        assertThat(dependencies).isEqualTo(
            listOf(
                DirectDependency(id = consumerDependency, isLibrary = false),
                TransitiveDependency(
                    id = dependencyOfConsumerDependency,
                    isLibrary = false,
                    path = listOf(consumerDependency, dependencyOfConsumerDependency)
                )
            )
        )
    }

    @Test
    fun `test implementation of a test dependency is not considered a transitive dependency`() {
        // given
        val consumer = "consumer"
        val consumerDependency = "dependencyA"
        val dependencyOfConsumerDependency = "dependencyB"
        graph.addInternalDependency(
            configurationId = DependencyConfiguration.TEST,
            module = consumer,
            dependency = consumerDependency
        )
        graph.addInternalDependency(
            configurationId = DependencyConfiguration.TEST,
            module = consumerDependency,
            dependency = dependencyOfConsumerDependency
        )

        // when
        val dependencies = graph.getDependencies(consumer)

        // then
        assertThat(dependencies).isEqualTo(
            listOf(
                DirectDependency(id = consumerDependency, isLibrary = false),
            )
        )
    }

    @Test
    fun `test implementation of a direct dependency is not considered a transitive dependency`() {
        // given
        val consumer = "consumer"
        val consumerDependency = "dependencyA"
        val dependencyOfConsumerDependency = "dependencyB"
        graph.addInternalDependency(
            configurationId = DependencyConfiguration.COMPILE,
            module = consumer,
            dependency = consumerDependency
        )
        graph.addInternalDependency(
            configurationId = DependencyConfiguration.TEST,
            module = consumerDependency,
            dependency = dependencyOfConsumerDependency
        )

        // when
        val dependencies = graph.getDependencies(consumer)

        // then
        assertThat(dependencies).isEqualTo(
            listOf(
                DirectDependency(id = consumerDependency, isLibrary = false),
            )
        )
    }

    @Test
    fun `graph can be deserialized`() {
        // given
        graph.addInternalDependency(
            module = "consumer",
            dependency = "dependencyA"
        )

        // when
        val byteArray = ByteArrayOutputStream()
        ObjectOutputStream(byteArray).use { stream -> stream.writeObject(graph) }

        // then
        val deserializedGraph = ObjectInputStream(ByteArrayInputStream(byteArray.toByteArray())).use { stream ->
            stream.readObject()
        } as DependencyGraph
        assertThat(graph).isEqualTo(deserializedGraph)
    }

    @Test
    fun `libraries are identified as such`() {
        // given
        val consumer = "consumer"
        val consumerDependency = "dependencyA"
        graph.addLibraryDependency(
            configurationId = DependencyConfiguration.TEST,
            module = consumer,
            dependency = consumerDependency
        )

        // when
        val dependencies = graph.getDependencies(consumer)

        // then
        assertThat(dependencies).isEqualTo(
            listOf(
                DirectDependency(id = consumerDependency, isLibrary = true),
            )
        )
    }

}
