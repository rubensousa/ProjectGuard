package com.rubensousa.dependencyguard.plugin.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class RestrictionMatch(
    val module: String,
    val dependency: String,
    val pathToDependency: String = dependency,
    val reason: String = "Unspecified",
    val isSuppressed: Boolean = false,
    val suppressionReason: String = "Unspecified",
) {

    fun asText(): String {
        return """
                | Dependency restriction violation!
                | Module -> $module
                | Violation -> $pathToDependency
                | Module(s) in '${module}' cannot depend on module(s) '${dependency}'
                | Reason: $reason
                """.trimMargin()
    }

}
