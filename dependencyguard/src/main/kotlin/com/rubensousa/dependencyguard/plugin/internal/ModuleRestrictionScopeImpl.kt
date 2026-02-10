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

package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.DenyScope
import com.rubensousa.dependencyguard.plugin.ModuleRestrictionScope
import com.rubensousa.dependencyguard.plugin.SuppressScope
import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

internal class ModuleRestrictionScopeImpl : ModuleRestrictionScope {

    private val denied = mutableListOf<ModuleSpec>()
    private val suppressed = mutableListOf<ModuleSpec>()

    override fun deny(
        dependencyPath: String,
        action: Action<DenyScope>,
    ) {
        val scope = DenyScopeImpl()
        action.execute(scope)
        denied.add(
            ModuleSpec(
                modulePath = dependencyPath,
                reason = scope.denyReason,
            )
        )
    }

    override fun suppress(dependencyPath: String, action: Action<SuppressScope>) {
        val scope = SuppressScopeImpl()
        action.execute(scope)
        suppressed.add(
            ModuleSpec(
                modulePath = dependencyPath,
                reason = scope.getReason(),
            )
        )
    }

    override fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>,
    ) {
        deny(dependencyPath = provider.getDependencyPath(), action)
    }

    override fun suppress(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<SuppressScope>,
    ) {
        suppress(dependencyPath = provider.getDependencyPath())
    }

    fun getDeniedDependencies() = denied.toList()

    fun getSuppressedDependencies() = suppressed.toList()

}
