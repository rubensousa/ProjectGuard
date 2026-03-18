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

import com.android.build.gradle.LibraryExtension
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import kotlin.test.Test

class DependencyGraphBuilderAndroidTest {

    private val graphBuilder = DependencyGraphBuilder()
    private lateinit var rootProject: Project
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
        consumerProject.plugins.apply("android-library")
        consumerProject.extensions.findByType<LibraryExtension>()!!.apply {
            namespace = "test.library"
            compileSdk = 34
        }
        consumerProject.repositories.apply {
            mavenCentral()
        }
        rootProject.evaluationDependsOnChildren()
    }

    @Test
    fun `transitive dependencies of a library dependency are not included`() {
        // given
        consumerProject.dependencies.add("testImplementation", "io.mockk:mockk:1.14.9")

        // when
        val graph = graphBuilder.buildFromComponents(consumerProject.getResolvedConfigurations())

        // then
        val testConfiguration = graph.getConfigurations().find { it.id == DependencyConfiguration.TEST_COMPILE }
        assertThat(testConfiguration?.getDependencies(consumerProject.path).toIds()).containsExactly("io.mockk:mockk")
    }

}
