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
        extension.restrictModule(":app") {
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
                setReason("Reason A")
            }
            suppress(":legacy:b") {
                setReason("Reason B")
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
        assertThat(restrictions.first().suppressed).containsExactly(
            ModuleSpec(
                ":legacy:b",
                "Reason B"
            )
        )
    }
}