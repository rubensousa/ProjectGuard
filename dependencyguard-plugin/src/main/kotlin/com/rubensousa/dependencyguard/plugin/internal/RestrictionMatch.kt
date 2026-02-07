package com.rubensousa.dependencyguard.plugin.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class RestrictionMatch(
    val modulePath: String,
    val dependencyPath: String,
    val reason: String = "Unspecified",
    val isSuppressed: Boolean = false,
    val suppressionReason: String = "Unspecified"
) {

    fun asText(): String {
        return """
                | Dependency restriction violation!
                | Module -> $modulePath
                | Violation -> $dependencyPath
                | Module(s) in '${modulePath}' cannot depend on module(s) '${dependencyPath}'
                | Reason: $reason
                """.trimMargin()
    }

}
