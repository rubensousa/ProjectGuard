package com.rubensousa.dependencyguard.plugin.internal

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

internal fun Provider<MinimalExternalModuleDependency>.getDependencyPath(): String {
    val library = get()
    return "${library.group}:${library.name}"
}
