package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class ModuleRestriction(
    val modulePath: String,
    val denied: List<ModuleSpec>,
    val suppressed: List<ModuleSpec>,
): Serializable
