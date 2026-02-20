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

package com.rubensousa.projectguard.plugin.internal

import com.rubensousa.projectguard.plugin.ModuleRestrictionScope
import com.rubensousa.projectguard.plugin.RestrictModuleRule
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.provider.Provider

internal class ModuleRestrictionScopeImpl : ModuleRestrictionScope {

    private val allowed = mutableListOf<ModuleAllowSpec>()
    private var restrictionReason = UNSPECIFIED_REASON
    private var allowExternalLibraries = false

    override fun reason(reason: String) {
        restrictionReason = reason
    }

    override fun allow(vararg modulePath: String) {
        allowed.addAll(modulePath.map { path ->
            ModuleAllowSpec(modulePath = path)
        })
    }

    override fun allow(vararg moduleDelegation: DelegatingProjectDependency) {
        allow(modulePath = moduleDelegation.map { module -> module.path }.toTypedArray())
    }

    override fun allowExternalLibraries() {
        allowExternalLibraries = true
    }

    override fun allow(vararg library: Provider<MinimalExternalModuleDependency>) {
        allow(modulePath = library.map { lib ->
            lib.getDependencyPath()
        }.toTypedArray())
    }

    override fun applyRule(rule: RestrictModuleRule) {
        allowed.addAll(rule.getSpecs())
    }

    fun getAllowedDependencies() = allowed.toList()

    fun getReason() = restrictionReason

    fun areExternalLibrariesAllowed() = allowExternalLibraries


}
