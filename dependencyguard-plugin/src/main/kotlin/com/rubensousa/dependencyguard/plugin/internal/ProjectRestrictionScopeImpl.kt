package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.DenyScope
import com.rubensousa.dependencyguard.plugin.ProjectRestrictionScope
import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

internal class ProjectRestrictionScopeImpl(
    private val onProjectRestriction: (ProjectRestriction) -> Unit,
): ProjectRestrictionScope {

    override fun deny(
        dependencyPath: String,
        action: Action<DenyScope>,
    ) {
        val scope = DenyScope()
        action.execute(scope)
        onProjectRestriction(
            ProjectRestriction(
                dependencyPath = dependencyPath,
                reason = scope.denyReason,
                exclusions = scope.exclusions
            )
        )
    }

    override fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>,
    ) {
        val library = provider.get()
        deny(dependencyPath = "${library.group}:${library.name}", action)
    }

}
