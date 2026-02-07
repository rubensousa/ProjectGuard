package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.DenyScope
import com.rubensousa.dependencyguard.plugin.ModuleRestrictionScope
import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

internal class ModuleRestrictionScopeImpl(
    private val modulePath: String,
    private val onRestriction: (ModuleRestriction) -> Unit,
): ModuleRestrictionScope {

    override fun deny(
        dependencyPath: String,
        action: Action<DenyScope>
    ) {
        val scope = DenyScope()
        action.execute(scope)
        onRestriction(
            ModuleRestriction(
                modulePath = modulePath,
                dependencyPath = dependencyPath,
                reason = scope.denyReason,
                exclusions = scope.exclusions
            )
        )
    }

    override fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>
    ) {
        val dep = provider.get()
        deny(dependencyPath = "${dep.group}:${dep.name}", action)
    }

}
