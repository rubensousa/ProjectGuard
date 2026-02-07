package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class ModuleSpec(
    val modulePath: String,
    val reason: String,
): Serializable
