package com.rubensousa.dependencyguard.plugin

import com.google.common.truth.Truth.assertThat
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
        extension.restrict(":app") {
            deny(":legacy")
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.moduleRestrictions
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().modulePath).isEqualTo(":app")
        assertThat(restrictions.first().dependencyPath).isEqualTo(":legacy")
    }

    @Test
    fun `extension correctly configures project restrictions`() {
        // given
        val project = ProjectBuilder.builder().build()
        val extension = project.extensions.create(
            "dependencyGuard",
            DependencyGuardExtension::class.java
        )

        // when
        extension.restrictAll {
            deny(":feature")
        }

        // then
        val spec = extension.getSpec()
        val restrictions = spec.projectRestrictions
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions.first().dependencyPath).isEqualTo(":feature")
    }
}