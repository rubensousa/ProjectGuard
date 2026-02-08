package com.rubensousa.dependencyguard.plugin.internal

import java.io.Serializable

internal class DependencyGraph(
    val configurationId: String,
    val nodes: MutableMap<String, MutableSet<String>> = mutableMapOf(),
) : Serializable {

    fun addDependency(
        module: String,
        dependency: String,
    ) {
        val existingDependencies = nodes.getOrPut(module) { mutableSetOf() }
        existingDependencies.add(dependency)
    }

    fun getDependencies(module: String): Set<String> {
        return nodes[module] ?: emptySet()
    }

    fun getAllDependencies(module: String): Set<String> {
        val visitedDependencies = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.addAll(getDependencies(module))
        while (stack.isNotEmpty()) {
            val currentModule = stack.removeFirst()
            if (visitedDependencies.contains(currentModule)) {
                continue
            }
            visitedDependencies.add(currentModule)
            getDependencies(currentModule).forEach { dependency ->
                stack.addFirst(dependency)
            }
        }
        return visitedDependencies
    }

    override fun toString(): String {
        return "DependencyGraph(configurationId='$configurationId', nodes=$nodes)"
    }

}
