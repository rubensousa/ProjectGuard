package com.rubensousa.dependencyguard.plugin.internal

internal class RestrictionChecker {

    fun findViolations(
        modulePath: String,
        dependencyPath: String,
        spec: DependencyGuardSpec,
    ): List<RestrictionMatch> {
        val matches = mutableListOf<RestrictionMatch>()
        spec.moduleRestrictions.forEach { restriction ->
            if (isModuleDependencyRestricted(
                    modulePath = modulePath,
                    dependencyPath = dependencyPath,
                    restriction = restriction
                )
            ) {
                matches.add(
                    RestrictionMatch(
                        modulePath = modulePath,
                        dependencyPath = dependencyPath,
                        reason = restriction.reason,
                        isExcluded = isDependencyExcluded(dependencyPath, restriction.exclusions)
                    )
                )
            }
        }
        spec.projectRestrictions.forEach { restriction ->
            val isDependencyRestricted = hasDependencyMatch(
                dependencyPath = dependencyPath,
                referencePath = restriction.dependencyPath
            )
            if (isDependencyRestricted) {
                matches.add(
                    RestrictionMatch(
                        modulePath = modulePath,
                        dependencyPath = dependencyPath,
                        reason = restriction.reason,
                        isExcluded = isDependencyExcluded(modulePath, restriction.exclusions)
                    )
                )
            }
        }
        return matches
    }

    internal fun isModuleDependencyRestricted(
        modulePath: String,
        dependencyPath: String,
        restriction: ModuleRestriction,
    ): Boolean {
        if (modulePath != restriction.modulePath && !modulePath.startsWith(restriction.modulePath)) {
            return false
        }
        return hasDependencyMatch(
            dependencyPath = dependencyPath,
            referencePath = restriction.dependencyPath
        )
    }

    private fun isDependencyExcluded(
        dependencyPath: String,
        exclusions: Set<String>,
    ): Boolean {
        return exclusions.any { exclusion ->
            dependencyPath.startsWith(exclusion)
        }
    }

    private fun hasDependencyMatch(
        dependencyPath: String,
        referencePath: String,
    ): Boolean {
        return dependencyPath.startsWith(referencePath) || dependencyPath == referencePath
    }

}