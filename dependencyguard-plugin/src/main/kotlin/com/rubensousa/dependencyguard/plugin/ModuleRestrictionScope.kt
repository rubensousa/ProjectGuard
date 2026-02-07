package com.rubensousa.dependencyguard.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

internal val defaultDenyScope = Action<DenyScope> { }

interface ModuleRestrictionScope {

    fun deny(
        dependencyPath: String,
        action: Action<DenyScope>
    )

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>
    )

    fun suppress(
        dependencyPath: String,
        action: Action<SuppressScope>,
    )

    fun suppress(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<SuppressScope>
    )

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

    // Required for groovy compatibility
    fun suppress(
        dependencyPath: String,
    ) {
        suppress(dependencyPath, defaultSuppressScope)
    }

    // Required for groovy compatibility
    fun suppress(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        suppress(provider, defaultSuppressScope)
    }
}