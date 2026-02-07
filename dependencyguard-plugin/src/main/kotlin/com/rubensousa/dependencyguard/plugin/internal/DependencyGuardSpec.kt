package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class DependencyGuardSpec(
    val moduleRestrictions: List<ModuleRestriction>,
    val projectRestrictions: List<ProjectRestriction>,
): Serializable {

    fun isEmpty(): Boolean {
        return moduleRestrictions.isEmpty() && projectRestrictions.isEmpty()
    }
}