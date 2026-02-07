package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class DependencyGuardSpec(
    val moduleRestrictions: List<ModuleRestriction>,
    val dependencyRestrictions: List<DependencyRestriction>,
): Serializable {

    fun isEmpty(): Boolean {
        return moduleRestrictions.isEmpty() && dependencyRestrictions.isEmpty()
    }
}