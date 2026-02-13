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
import com.rubensousa.dependencyguard.plugin.internal.report.DependencyGraphBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import kotlin.test.Test

class DependencyGraphBuilderTest {

    private val graphBuilder = DependencyGraphBuilder()
    private lateinit var rootProject: Project
    private lateinit var legacyProject: Project
    private lateinit var consumerProject: Project

    @Before
    fun setup() {
        rootProject = ProjectBuilder.builder()
            .withName("root")
            .build()
        consumerProject = ProjectBuilder.builder()
            .withName("consumer")
            .withParent(rootProject)
            .build()
        rootProject.evaluationDependsOnChildren()
        consumerProject.plugins.apply("java-library")
        legacyProject = ProjectBuilder.builder()
            .withName("legacy")
            .withParent(rootProject)
            .build()
    }

    @Test
    fun `graph is built correctly for implementation configuration`() {
        // given
        val legacyProjectA = consumerProject.addLegacyDependency("a")
        val legacyProjectB = consumerProject.addLegacyDependency("b")

        // when
        val graphs = graphBuilder.buildFromProject(consumerProject)

        // then
        val compileGraph = graphs.findCompilationGraph()!!
        assertThat(compileGraph.getDependencies(consumerProject.path)).isEqualTo(
            setOf(legacyProjectA.path, legacyProjectB.path)
        )
    }

    @Test
    fun `graph is built correctly for testImplementation configuration`() {
        // given
        val legacyProjectA = consumerProject.addLegacyDependency("a")
        val legacyProjectC = consumerProject.addLegacyTestDependency("c")

        // when
        val graphs = graphBuilder.buildFromProject(consumerProject)

        // then
        val testGraph = graphs.findTestGraph()!!
        assertThat(testGraph.getDependencies(consumerProject.path)).isEqualTo(
            setOf(legacyProjectA.path, legacyProjectC.path)
        )
    }

    private fun List<DependencyGraph>.findCompilationGraph(): DependencyGraph? {
        return this.find { it.configurationId == "compileClasspath" }
    }

    private fun List<DependencyGraph>.findTestGraph(): DependencyGraph? {
        return this.find { it.configurationId == "testCompileClasspath" }
    }

    private fun Project.addLegacyDependency(dependency: String): Project {
        val legacyProject = createLegacySubProject(dependency)
        dependencies.add("implementation", legacyProject)
        return legacyProject
    }

    private fun Project.addLegacyTestDependency(dependency: String): Project {
        val legacyProject = createLegacySubProject(dependency)
        dependencies.add("testImplementation", legacyProject)
        return legacyProject
    }

    private fun createLegacySubProject(name: String): Project {
        val project = ProjectBuilder.builder()
            .withName(name)
            .withParent(legacyProject)
            .build()
        project.plugins.apply("java-library")
        return project
    }
}
