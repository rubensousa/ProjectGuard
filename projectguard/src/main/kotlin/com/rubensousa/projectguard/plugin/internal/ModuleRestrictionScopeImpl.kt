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
import org.gradle.api.provider.Provider

internal class ModuleRestrictionScopeImpl : ModuleRestrictionScope {

    private val allowed = mutableListOf<ModuleAllowSpec>()
    private var restrictionReason = UNSPECIFIED_REASON

    override fun reason(reason: String) {
        restrictionReason = reason
    }

    override fun allow(modulePath: String) {
        allowed.add(
            ModuleAllowSpec(
                modulePath = modulePath,
            )
        )
    }

    override fun allowModules(modulePaths: List<String>) {
        modulePaths.forEach { path -> allow(path) }
    }

    override fun allow(library: Provider<MinimalExternalModuleDependency>) {
        allow(library.getDependencyPath())
    }

    override fun allowLibs(libraries: List<Provider<MinimalExternalModuleDependency>>) {
        libraries.forEach { library -> allow(library) }
    }

    override fun applyRule(rule: RestrictModuleRule) {
        allowed.addAll(rule.getSpecs())
    }

    fun getAllowedDependencies() = allowed.toList()

    fun getReason() = restrictionReason


}
