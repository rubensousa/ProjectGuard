package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class DependencyRestriction(
    val dependencyPath: String,
    val reason: String,
    val allowed: List<ModuleSpec>,
    val suppressed: List<ModuleSpec>
): Serializable
