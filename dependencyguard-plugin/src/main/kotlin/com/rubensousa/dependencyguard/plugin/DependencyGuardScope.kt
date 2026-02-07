package com.rubensousa.dependencyguard.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

internal val defaultDenyScope = Action<DenyScope> { }

/**
 * Examples:
 *
 * ```
 * restrict(":domain") {
 *      deny(":data")
 * }
 *
 * restrictAll {
 *      // No single module can depend on legacy
 *      deny(":legacy")
 * }
 *
 * ```
 */
interface DependencyGuardScope {

    fun restrict(modulePath: String, action: Action<ModuleRestrictionScope>)

    /**
     * Usage:
     *
     * ```

     * ```
     */
    fun restrictAll(action: Action<ProjectRestrictionScope>)

}

interface ProjectRestrictionScope {

    // Required for groovy compatibility
    fun deny(
        dependencyPath: String,
    ) {
        deny(dependencyPath, defaultDenyScope)
    }

    // Required for groovy compatibility
    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        deny(provider, defaultDenyScope)
    }

    fun deny(
        dependencyPath: String,
        action: Action<DenyScope>,
    )

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>
    )
}


interface ModuleRestrictionScope {

    // Required for groovy compatibility
    fun deny(
        dependencyPath: String,
    ) {
        deny(dependencyPath, defaultDenyScope)
    }

    // Required for groovy compatibility
    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        deny(provider, defaultDenyScope)
    }

    fun deny(
        dependencyPath: String,
        action: Action<DenyScope>
    )

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>
    )
}
