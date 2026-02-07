package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal data class TaskDependencies(
    val name: String,
    val projectPaths: List<String>,
    val externalLibraries: List<String>
) : Serializable