package com.rubensousa.dependencyguard.plugin.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class DependencyGuardReport(
    val modules: List<ModuleReport>
)

@Serializable
internal data class ModuleReport(
    val module: String,
    val fatalMatches: List<Match>,
    val excludedMatches: List<Match>
)

@Serializable
internal data class Match(
    val dependency: String,
    val reason: String,
)

