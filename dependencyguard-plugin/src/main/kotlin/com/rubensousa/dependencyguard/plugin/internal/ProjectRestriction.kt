package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class ProjectRestriction(
    val dependencyPath: String,
    val reason: String,
    val exclusions: Set<String>,
): Serializable
