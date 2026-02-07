package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.ModuleRestriction
import com.rubensousa.dependencyguard.plugin.internal.ModuleRestrictionScopeImpl
import com.rubensousa.dependencyguard.plugin.internal.ProjectRestriction
import com.rubensousa.dependencyguard.plugin.internal.ProjectRestrictionScopeImpl
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

abstract class DependencyGuardExtension @Inject constructor(
    objects: ObjectFactory
): DependencyGuardScope {

    private val moduleRestrictions = objects.listProperty<ModuleRestriction>()
    private val projectRestrictions = objects.listProperty<ProjectRestriction>()

    override fun restrict(modulePath: String, action: Action<ModuleRestrictionScope>) {
        val scope = ModuleRestrictionScopeImpl(
            modulePath = modulePath,
            onRestriction = { restriction ->
                moduleRestrictions.add(restriction)
            },
        )
        action.execute(scope)
    }

    override fun restrictAll(action: Action<ProjectRestrictionScope>) {
        val scope = ProjectRestrictionScopeImpl(
            onProjectRestriction = { restriction ->
                projectRestrictions.add(restriction)
            },
        )
        action.execute(scope)
    }

    internal fun getSpec(): DependencyGuardSpec {
        return DependencyGuardSpec(
            moduleRestrictions = moduleRestrictions.get(),
            projectRestrictions = projectRestrictions.get()
        )
    }

}
