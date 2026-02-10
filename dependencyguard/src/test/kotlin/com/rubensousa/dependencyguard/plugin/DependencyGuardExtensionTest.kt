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

package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
import com.rubensousa.dependencyguard.plugin.internal.ModuleSpec
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DependencyGuardExtensionTest {

    @Test
    fun `extension correctly configures module restrictions`() {
        // given
        val project = ProjectBuilder.builder().build()
        val extension = project.extensions.create(
            "dependencyGuard",
            DependencyGuardExtension::class.java
        )

        // when
        extension.guard(":app") {
            deny(":legacy")
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.moduleRestrictions
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().modulePath).isEqualTo(":app")
        assertThat(restrictions.first().denied.first().modulePath).isEqualTo(":legacy")
    }

    @Test
    fun `extension correctly configures dependency restrictions`() {
        // given
        val project = ProjectBuilder.builder().build()
        val extension = project.extensions.create(
            "dependencyGuard",
            DependencyGuardExtension::class.java
        )

        // when
        extension.restrictDependency(":legacy") {
            allow(":legacy:a") {
                this.reason("Reason A")
            }
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.dependencyRestrictions
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().dependencyPath).isEqualTo(":legacy")
        assertThat(restrictions.first().allowed).containsExactly(
            ModuleSpec(
                ":legacy:a",
                "Reason A"
            )
        )
    }
}