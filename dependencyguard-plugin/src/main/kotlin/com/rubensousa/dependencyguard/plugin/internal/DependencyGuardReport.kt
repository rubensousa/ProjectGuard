package com.rubensousa.dependencyguard.plugin.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class DependencyGuardReport(
    val modules: List<ModuleReport>
)

@Serializable
internal data class ModuleReport(
    val module: String,
    val fatal: List<FatalMatch>,
    val suppressed: List<SuppressedMatch>
)

@Serializable
internal data class FatalMatch(
    val dependency: String,
    val pathToDependency: String,
    val reason: String,
)

@Serializable
internal data class SuppressedMatch(
    val dependency: String,
    val pathToDependency: String,
    val suppressionReason: String
)

