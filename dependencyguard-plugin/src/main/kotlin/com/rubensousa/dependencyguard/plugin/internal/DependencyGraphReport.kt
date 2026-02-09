package com.rubensousa.dependencyguard.plugin.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class DependencyGraphReport(
    val module: String,
    val configurations: List<DependencyGraphConfiguration>,
)

@Serializable
internal data class DependencyGraphAggregateReport(
    val moduleReports: List<DependencyGraphReport>
)

@Serializable
internal data class DependencyGraphConfiguration(
    val id: String,
    val dependencies: List<String>,
)
