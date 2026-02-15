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
import com.rubensousa.dependencyguard.plugin.internal.ModuleAllowSpec
import com.rubensousa.dependencyguard.plugin.internal.ModuleDenialSpec
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DependencyGuardExtensionTest {

    @Test
    fun `extension correctly configures guard spec`() {
        // given
        val extension = createExtension()

        // when
        extension.guard(":app") {
            deny(":legacy")
        }

        // then
        val spec = extension.getSpec()
        val guards = spec.guardSpecs
        assertThat(guards).hasSize(1)
        assertThat(guards.first().modulePath).isEqualTo(":app")
        assertThat(guards.first().denied.first().modulePath).isEqualTo(":legacy")
    }

    @Test
    fun `extension correctly configures module restriction`() {
        // given
        val extension = createExtension()

        // when
        extension.restrictModule(":domain") {
            allow("kotlin")
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.moduleRestrictionSpecs
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().modulePath).isEqualTo(":domain")
        assertThat(restrictions.first().allowed.first().modulePath).isEqualTo("kotlin")
    }

    @Test
    fun `extension correctly configures dependency restrictions`() {
        // given
        val extension = createExtension()

        // when
        extension.restrictDependency(":legacy") {
            allow(":legacy:a")
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.dependencyRestrictionSpecs
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().dependencyPath).isEqualTo(":legacy")
        assertThat(restrictions.first().allowed).containsExactly(ModuleAllowSpec(":legacy:a"))
    }

    @Test
    fun `extension configures re-usable guard rule`() {
        // given
        val extension = createExtension()

        // when
        val rule = extension.guardRule {
            deny("androidx") { reason("androidx reason") }
            deny("coil") { reason("coil reason") }
        }
        extension.guard(":domain") {
            applyRule(rule)
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.guardSpecs
        assertThat(restrictions).hasSize(1)
        val restriction = restrictions.first()
        assertThat(restriction.denied).isEqualTo(
            listOf(
                ModuleDenialSpec(
                    modulePath = "androidx",
                    reason = "androidx reason"
                ),
                ModuleDenialSpec(
                    modulePath = "coil",
                    reason = "coil reason"
                )
            )
        )
    }

    @Test
    fun `extension configures re-usable restrict module rule`() {
        // given
        val extension = createExtension()

        // when
        val rule = extension.restrictModuleRule {
            allow("kotlin")
        }
        extension.restrictModule(":domain") {
            applyRule(rule)
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.moduleRestrictionSpecs
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().modulePath).isEqualTo(":domain")
        assertThat(restrictions.first().allowed.first().modulePath).isEqualTo("kotlin")
    }

    @Test
    fun `extension configures re-usable restrict dependency rule`() {
        // given
        val extension = createExtension()

        // when
        val rule = extension.restrictDependencyRule {
            allow("old-feature")
        }
        extension.restrictDependency(":legacy") {
            applyRule(rule)
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.dependencyRestrictionSpecs
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().dependencyPath).isEqualTo(":legacy")
        assertThat(restrictions.first().allowed.first().modulePath).isEqualTo("old-feature")
    }

    private fun createExtension(): DependencyGuardExtension {
        val project = ProjectBuilder.builder().build()
        return project.extensions.create(
            "dependencyGuard",
            DependencyGuardExtension::class.java
        )
    }
}