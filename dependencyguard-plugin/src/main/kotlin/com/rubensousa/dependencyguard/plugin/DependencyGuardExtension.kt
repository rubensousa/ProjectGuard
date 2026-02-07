package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.ModuleRestriction
import com.rubensousa.dependencyguard.plugin.internal.ModuleRestrictionScopeImpl
import com.rubensousa.dependencyguard.plugin.internal.DependencyRestriction
import com.rubensousa.dependencyguard.plugin.internal.DependencyRestrictionScopeImpl
import com.rubensousa.dependencyguard.plugin.internal.getDependencyPath
import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

abstract class DependencyGuardExtension @Inject constructor(
    objects: ObjectFactory,
) : DependencyGuardScope {

    private val moduleRestrictions = objects.listProperty<ModuleRestriction>()
    private val dependencyRestrictions = objects.listProperty<DependencyRestriction>()

    override fun restrictModule(modulePath: String, action: Action<ModuleRestrictionScope>) {
        val scope = ModuleRestrictionScopeImpl()
        action.execute(scope)
        moduleRestrictions.add(
            ModuleRestriction(
                modulePath = modulePath,
                denied = scope.getDeniedDependencies(),
                suppressed = scope.getSuppressedDependencies()
            )
        )
    }

    override fun restrictDependency(
        dependencyPath: String,
        action: Action<DependencyRestrictionScope>,
    ) {
        val scope = DependencyRestrictionScopeImpl()
        action.execute(scope)
        dependencyRestrictions.add(
            DependencyRestriction(
                dependencyPath = dependencyPath,
                reason = scope.getReason(),
                allowed = scope.getAllowedModules(),
                suppressed = scope.getSuppressedModules()
            )
        )
    }

    override fun restrictDependency(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DependencyRestrictionScope>,
    ) {
        restrictDependency(
            dependencyPath = provider.getDependencyPath(),
            action = action
        )
    }

    internal fun getSpec(): DependencyGuardSpec {
        return DependencyGuardSpec(
            moduleRestrictions = moduleRestrictions.get(),
            dependencyRestrictions = dependencyRestrictions.get()
        )
    }

}
