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
import org.junit.Test

class SuppressionMapTest {

    private val suppressionMap = SuppressionMap()
    private val dependencyGraph = DependencyGraph()

    @Test
    fun `getSuppression returns null if dependency is not suppressed`() {
        // given
        suppressionMap.add(module = ":app", dependency = ":feature1")

        // when
        val suppression = suppressionMap.getSuppression(module = ":app", dependency = ":feature2")

        // then
        assertThat(suppression).isNull()
    }

    @Test
    fun `getSuppression returns existing suppression if dependency is suppressed`() {
        suppressionMap.add(":app", ":feature1", "Test")
        val suppression = suppressionMap.getSuppression(":app", ":feature1")
        assertThat(suppression).isNotNull()
        assertThat(suppression?.dependency).isEqualTo(":feature1")
        assertThat(suppression?.reason).isEqualTo("Test")
    }

    @Test
    fun `getSuppression returns null if module has no suppressions`() {
        assertThat(suppressionMap.getSuppression(":app", ":feature1")).isNull()
    }

    @Test
    fun `getBaseline returns empty configuration if map is empty`() {
        val suppressionMap = SuppressionMap()
        val baseline = suppressionMap.getBaseline()
        assertThat(baseline.suppressions).isEmpty()
    }

    @Test
    fun `getBaseline returns configuration with all suppressions`() {
        // given
        suppressionMap.add(":app", ":feature1", "Reason1")
        suppressionMap.add(":lib", ":feature1", "Reason3")

        // when
        val baseline = suppressionMap.getBaseline()

        // then
        assertThat(baseline.suppressions).hasSize(2)
        val appSuppression = baseline.suppressions[":app"]!!.first()
        assertThat(appSuppression.dependency).isEqualTo(":feature1")
        assertThat(appSuppression.reason).isEqualTo("Reason1")

        val libSuppression = baseline.suppressions[":lib"]!!.first()
        assertThat(libSuppression.dependency).isEqualTo(":feature1")
        assertThat(libSuppression.reason).isEqualTo("Reason3")
    }

    @Test
    fun `getBaseline returns suppressions sorted by module and dependency`() {
        // given
        suppressionMap.add(":lib", ":c", "Reason1")
        suppressionMap.add(":app", ":b", "Reason2")
        suppressionMap.add(":app", ":a", "Reason3")

        // when
        val baseline = suppressionMap.getBaseline()

        // then
        val entries = baseline.suppressions.entries.toList()
        assertThat(entries[0].key).isEqualTo(":app")
        assertThat(entries[0].value[0].dependency).isEqualTo(":a")
        assertThat(entries[0].value[1].dependency).isEqualTo(":b")
        assertThat(entries[1].key).isEqualTo(":lib")
        assertThat(entries[1].value[0].dependency).isEqualTo(":c")
    }

    @Test
    fun `set clears existing suppressions and adds new ones`() {
        // given
        suppressionMap.add(":app", ":feature1", "OldReason")
        val newBaseline = BaselineConfiguration(
            mapOf(
                ":app" to listOf(DependencySuppression(":feature2", "NewReason")),
                ":lib" to listOf(DependencySuppression(":feature1", "AnotherReason"))
            )
        )

        // when
        suppressionMap.set(newBaseline)
        assertThat(suppressionMap.getSuppression(":app", ":feature1")).isNull()
        val newAppSuppression = suppressionMap.getSuppression(":app", ":feature2")
        assertThat(newAppSuppression).isNotNull()
        assertThat(newAppSuppression?.reason).isEqualTo("NewReason")
        val newLibSuppression = suppressionMap.getSuppression(":lib", ":feature1")
        assertThat(newLibSuppression).isNotNull()
        assertThat(newLibSuppression?.reason).isEqualTo("AnotherReason")
    }

    @Test
    fun `isOutdated returns false for empty suppressions`() {
        // given
        dependencyGraph.addInternalDependency(module = ":domain", dependency = ":legacy")

        // then
        assertThat(suppressionMap.isOutdated(dependencyGraph)).isFalse()
    }

    @Test
    fun `isOutdated returns false for dependencies that still exist`() {
        // given
        dependencyGraph.addInternalDependency(module = ":domain", dependency = ":legacy")

        // when
        suppressionMap.add(module = ":domain", dependency = ":legacy")

        // then
        assertThat(suppressionMap.isOutdated(dependencyGraph)).isFalse()
    }

    @Test
    fun `isOutdated returns true for dependencies that no longer exist`() {
        // given
        suppressionMap.add(module = ":domain", dependency = ":legacy")

        // then
        assertThat(suppressionMap.isOutdated(dependencyGraph)).isTrue()
    }

}
