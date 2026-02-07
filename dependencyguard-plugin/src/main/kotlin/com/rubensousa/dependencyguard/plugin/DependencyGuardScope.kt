package com.rubensousa.dependencyguard.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

private val defaultDependencyRestrictionScope = Action<DependencyRestrictionScope> {}

interface DependencyGuardScope {

    /**
     * Example:
     *
     * ```
     * restrictModule(":domain") {
     *      // Domain modules should not depend on UI modules
     *      deny(":ui")
     * }
     * ```
     */
    fun restrictModule(
        modulePath: String,
        action: Action<ModuleRestrictionScope>,
    )

    /**
     * Example:
     *
     * ```
     * restrictDependency(":legacy") {
     *      // Only legacy modules are allowed to depend on other legacy modules
     *      allow(":legacy")
     * }
     * ```
     */
    fun restrictDependency(
        dependencyPath: String,
        action: Action<DependencyRestrictionScope>,
    )

    /**
     * Example:
     *
     * ```
     * restrictDependency(libs.mockk) {
     *      // Only legacy modules are allowed to use mockk for tests
     *      allow(":legacy")
     * }
     * ```
     */
    fun restrictDependency(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DependencyRestrictionScope>,
    )

    // Just here for groovy support
    fun restrictDependency(
        dependencyPath: String
    ) {
        restrictDependency(dependencyPath, defaultDependencyRestrictionScope)
    }

    // Just here for groovy support
    fun restrictDependency(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        restrictDependency(provider, defaultDependencyRestrictionScope)
    }

}
