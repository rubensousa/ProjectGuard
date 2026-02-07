package com.rubensousa.dependencyguard.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider


internal val defaultAllowScope = Action<AllowScope> { }
internal val defaultSuppressScope = Action<SuppressScope> { }

interface DependencyRestrictionScope {

    fun setReason(reason: String)

    // Required for groovy compatibility
    fun allow(
        modulePath: String,
    ) {
        allow(modulePath, defaultAllowScope)
    }

    fun allow(
        modulePath: String,
        action: Action<AllowScope>,
    )

    // Required for groovy compatibility
    fun suppress(
        modulePath: String,
    ) {
        suppress(modulePath, defaultSuppressScope)
    }

    fun suppress(
        modulePath: String,
        action: Action<SuppressScope>,
    )

}
