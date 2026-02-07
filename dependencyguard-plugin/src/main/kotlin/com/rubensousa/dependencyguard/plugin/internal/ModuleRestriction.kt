package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class ModuleRestriction(
    val modulePath: String,
    val dependencyPath: String,
    val exclusions: Set<String>,
    val reason: String,
): Serializable
