package com.rubensousa.dependencyguard.plugin.internal

internal class RestrictionChecker {

    private val unspecifiedReason = "Unspecified"

    fun findMatches(
        modulePath: String,
        dependencyGraph: DependencyGraph,
        spec: DependencyGuardSpec,
    ): List<RestrictionMatch> {
        val matches = mutableListOf<RestrictionMatch>()
        dependencyGraph.getDependencyMatches(modulePath).forEach { dependencyMatch ->
            matches.addAll(
                findDependencyMatches(
                    moduleId = modulePath,
                    dependencyId = dependencyMatch.dependencyId,
                    pathToDependency = dependencyMatch.path,
                    spec = spec
                )
            )
        }
        return matches
    }

    private fun findDependencyMatches(
        moduleId: String,
        dependencyId: String,
        pathToDependency: List<String>,
        spec: DependencyGuardSpec,
    ): List<RestrictionMatch> {
        val matches = mutableListOf<RestrictionMatch>()
        fillModuleRestrictionMatches(
            matches = matches,
            moduleId = moduleId,
            dependencyId = dependencyId,
            pathToDependency = pathToDependency,
            spec = spec
        )
        fillDependencyRestrictionMatches(
            matches = matches,
            moduleId = moduleId,
            dependencyId = dependencyId,
            pathToDependency = pathToDependency,
            spec = spec
        )
        return matches
    }

    /**
     * Module restrictions are allow-by-default
     * Each module restriction specifies individual denials for dependencies
     */
    private fun fillModuleRestrictionMatches(
        matches: MutableList<RestrictionMatch>,
        moduleId: String,
        dependencyId: String,
        pathToDependency: List<String>,
        spec: DependencyGuardSpec,
    ) {
        spec.moduleRestrictions.forEach { restriction ->
            val matchesModule = hasModuleMatch(
                modulePath = moduleId,
                referencePath = restriction.modulePath
            )
            if (matchesModule) {
                val denial = restriction.denied.find { spec ->
                    hasModuleMatch(
                        modulePath = dependencyId,
                        referencePath = spec.modulePath
                    )
                }
                if (denial != null) {
                    matches.add(
                        RestrictionMatch(
                            module = moduleId,
                            dependency = dependencyId,
                            pathToDependency = buildDependencyPath(dependencyId, pathToDependency),
                            reason = denial.reason,
                            isSuppressed = false,
                            suppressionReason = unspecifiedReason
                        )
                    )
                } else {
                    val suppression = restriction.suppressed.find { suppressedModule ->
                        hasModuleMatch(
                            modulePath = dependencyId,
                            referencePath = suppressedModule.modulePath
                        )
                    }
                    if (suppression != null) {
                        matches.add(
                            RestrictionMatch(
                                module = moduleId,
                                dependency = dependencyId,
                                pathToDependency = buildDependencyPath(dependencyId, pathToDependency),
                                reason = unspecifiedReason,
                                isSuppressed = true,
                                suppressionReason = suppression.reason
                            )
                        )
                    }
                }

            }
        }
    }

    /**
     * Dependency restrictions are deny-by-default.
     * Each dependency restriction specifies individual allowances for dependencies
     */
    private fun fillDependencyRestrictionMatches(
        matches: MutableList<RestrictionMatch>,
        moduleId: String,
        dependencyId: String,
        pathToDependency: List<String>,
        spec: DependencyGuardSpec,
    ) {
        spec.dependencyRestrictions.forEach { restriction ->
            val isDependencyRestricted = hasModuleMatch(
                modulePath = dependencyId,
                referencePath = restriction.dependencyPath
            )
            val isModuleAllowed = restriction.allowed.any { exclusion ->
                hasModuleMatch(modulePath = moduleId, referencePath = exclusion.modulePath)
            }
            if (isDependencyRestricted && !isModuleAllowed) {
                val suppression = restriction.suppressed.find { suppressedModule ->
                    hasModuleMatch(
                        modulePath = moduleId,
                        referencePath = suppressedModule.modulePath
                    )
                }
                matches.add(
                    RestrictionMatch(
                        module = moduleId,
                        dependency = dependencyId,
                        pathToDependency = buildDependencyPath(dependencyId, pathToDependency),
                        reason = restriction.reason,
                        isSuppressed = suppression != null,
                        suppressionReason = suppression?.reason ?: unspecifiedReason
                    )
                )
            }
        }
    }

    private fun buildDependencyPath(
        dependencyId: String,
        pathToDependency: List<String>,
    ): String {
        if (pathToDependency.isEmpty()) {
            return dependencyId
        }
        return pathToDependency.joinToString(separator = " -> ") { it }
    }

    private fun hasModuleMatch(
        modulePath: String,
        referencePath: String,
    ): Boolean {
        if (modulePath == referencePath) {
            return true
        }
        return modulePath.startsWith(referencePath)
    }

}
