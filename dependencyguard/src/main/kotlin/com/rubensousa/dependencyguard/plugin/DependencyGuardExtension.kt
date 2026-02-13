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

package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.DependencyRestrictionScopeImpl
import com.rubensousa.dependencyguard.plugin.internal.DependencyRestrictionSpec
import com.rubensousa.dependencyguard.plugin.internal.ModuleRestrictionScopeImpl
import com.rubensousa.dependencyguard.plugin.internal.ModuleRestrictionSpec
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

    private val moduleRestrictions = objects.listProperty<ModuleRestrictionSpec>()
    private val dependencyRestrictions = objects.listProperty<DependencyRestrictionSpec>()

    override fun guard(modulePath: String, action: Action<ModuleRestrictionScope>) {
        val scope = ModuleRestrictionScopeImpl()
        action.execute(scope)
        moduleRestrictions.add(
            ModuleRestrictionSpec(
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
        dependencyRestrictions.add(
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

    internal fun getSpec(): DependencyGuardSpec {
        return DependencyGuardSpec(
            moduleRestrictions = moduleRestrictions.get(),
            dependencyRestrictions = dependencyRestrictions.get()
        )
    }

}
