/*
 * Copyright 2026 Rúben Sousa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rubensousa.projectguard.plugin

import com.rubensousa.projectguard.plugin.internal.DependencyRestrictionScopeImpl
import com.rubensousa.projectguard.plugin.internal.DependencyRestrictionSpec
import com.rubensousa.projectguard.plugin.internal.GuardScopeImpl
import com.rubensousa.projectguard.plugin.internal.GuardSpec
import com.rubensousa.projectguard.plugin.internal.ModuleRestrictionScopeImpl
import com.rubensousa.projectguard.plugin.internal.ModuleRestrictionSpec
import com.rubensousa.projectguard.plugin.internal.ProjectGuardSpec
import com.rubensousa.projectguard.plugin.internal.getDependencyPath
import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

abstract class ProjectGuardExtension @Inject constructor(
    objects: ObjectFactory,
) : ProjectGuardScope {

    private val guardSpecs = objects.listProperty<GuardSpec>()
    private val moduleRestrictionSpecs = objects.listProperty<ModuleRestrictionSpec>()
    private val dependencyRestrictionSpecs = objects.listProperty<DependencyRestrictionSpec>()

    override fun restrictModule(modulePath: String, action: Action<ModuleRestrictionScope>) {
        val scope = ModuleRestrictionScopeImpl()
        action.execute(scope)
        moduleRestrictionSpecs.add(
            ModuleRestrictionSpec(
                modulePath = modulePath,
                reason = scope.getReason(),
                allowed = scope.getAllowedDependencies(),
                allowExternalLibraries = scope.areExternalLibrariesAllowed()
            )
        )
    }

    override fun guard(modulePath: String, action: Action<GuardScope>) {
        val scope = GuardScopeImpl()
        action.execute(scope)
        guardSpecs.add(
            GuardSpec(
                modulePath = modulePath,
                denied = scope.getDeniedDependencies(),
            )
        )
    }

    override fun restrictDependency(
        dependencyPath: String,
        action: Action<DependencyRestrictionScope>,
    ) {
        val scope = DependencyRestrictionScopeImpl()
        action.execute(scope)
        dependencyRestrictionSpecs.add(
            DependencyRestrictionSpec(
                dependencyPath = dependencyPath,
                reason = scope.getReason(),
                allowed = scope.getAllowedModules(),
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

    override fun guardRule(action: Action<GuardScope>): GuardRule {
        val rule = GuardRule()
        val scope = GuardScopeImpl()
        action.execute(scope)
        rule.setDenials(scope.getDeniedDependencies())
        return rule
    }

    override fun restrictModuleRule(
        action: Action<ModuleRestrictionScope>,
    ): RestrictModuleRule {
        val rule = RestrictModuleRule()
        val scope = ModuleRestrictionScopeImpl()
        action.execute(scope)
        rule.setSpecs(scope.getAllowedDependencies())
        return rule
    }

    override fun restrictDependencyRule(
        action: Action<DependencyRestrictionScope>,
    ): RestrictDependencyRule {
        val rule = RestrictDependencyRule()
        val scope = DependencyRestrictionScopeImpl()
        action.execute(scope)
        rule.setSpecs(scope.getAllowedModules())
        return rule
    }

    internal fun getSpec(): ProjectGuardSpec {
        return ProjectGuardSpec(
            guardSpecs = guardSpecs.get(),
            moduleRestrictionSpecs = moduleRestrictionSpecs.get(),
            dependencyRestrictionSpecs = dependencyRestrictionSpecs.get()
        )
    }

}
