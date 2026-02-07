package com.rubensousa.dependencyguard.plugin.internal

internal class RestrictionChecker {

    private val unspecifiedReason = "Unspecified"

    fun findMatches(
        modulePath: String,
        dependencyPath: String,
        spec: DependencyGuardSpec,
    ): List<RestrictionMatch> {
        val matches = mutableListOf<RestrictionMatch>()
        fillModuleRestrictionMatches(
            matches = matches,
            modulePath = modulePath,
            dependencyPath = dependencyPath,
            spec = spec
        )
        fillDependencyRestrictionMatches(
            matches = matches,
            modulePath = modulePath,
            dependencyPath = dependencyPath,
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
        modulePath: String,
        dependencyPath: String,
        spec: DependencyGuardSpec,
    ) {
        spec.moduleRestrictions.forEach { restriction ->
            val matchesModule = hasModuleMatch(
                modulePath = modulePath,
                referencePath = restriction.modulePath
            )
            if (matchesModule) {
                val denial = restriction.denied.find { spec ->
                    hasModuleMatch(
                        modulePath = dependencyPath,
                        referencePath = spec.modulePath
                    )
                }
                if (denial != null) {
                    matches.add(
                        RestrictionMatch(
                            modulePath = modulePath,
                            dependencyPath = dependencyPath,
                            reason = denial.reason,
                            isSuppressed = false,
                            suppressionReason = unspecifiedReason
                        )
                    )
                } else {
                    val suppression = restriction.suppressed.find { suppressedModule ->
                        hasModuleMatch(
                            modulePath = dependencyPath,
                            referencePath = suppressedModule.modulePath
                        )
                    }
                    if (suppression != null) {
                        matches.add(
                            RestrictionMatch(
                                modulePath = modulePath,
                                dependencyPath = dependencyPath,
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
        modulePath: String,
        dependencyPath: String,
        spec: DependencyGuardSpec,
    ) {
        spec.dependencyRestrictions.forEach { restriction ->
            val isDependencyRestricted = hasModuleMatch(
                modulePath = dependencyPath,
                referencePath = restriction.dependencyPath
            )
            val isModuleAllowed = restriction.allowed.any { exclusion ->
                hasModuleMatch(modulePath = modulePath, referencePath = exclusion.modulePath)
            }
            if (isDependencyRestricted && !isModuleAllowed) {
                val suppression = restriction.suppressed.find { suppressedModule ->
                    hasModuleMatch(
                        modulePath = modulePath,
                        referencePath = suppressedModule.modulePath
                    )
                }
                matches.add(
                    RestrictionMatch(
                        modulePath = modulePath,
                        dependencyPath = dependencyPath,
                        reason = restriction.reason,
                        isSuppressed = suppression != null,
                        suppressionReason = suppression?.reason ?: unspecifiedReason
                    )
                )
            }
        }
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
