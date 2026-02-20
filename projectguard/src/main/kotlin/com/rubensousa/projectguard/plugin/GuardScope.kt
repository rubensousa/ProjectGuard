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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.provider.Provider
import org.gradle.util.internal.ConfigureUtil

private val emptyDenyScope = Action<DenyScope> { }

interface GuardScope {

    fun deny(
        dependencyPath: String,
        action: Action<DenyScope>,
    )

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>,
    )

    fun applyRule(rule: GuardRule)

    fun deny(
        dependencyDelegation: DelegatingProjectDependency,
        action: Action<DenyScope>,
    ) {
        deny(dependencyPath = dependencyDelegation.path, action = action)
    }

    // Required for groovy compatibility
    fun deny(dependencyPath: String) {
        deny(dependencyPath, emptyDenyScope)
    }

    fun deny(dependencyDelegation: DelegatingProjectDependency) {
        deny(dependencyDelegation.path, emptyDenyScope)
    }

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        deny(provider, emptyDenyScope)
    }

    fun deny(
        dependencyDelegation: DelegatingProjectDependency,
        closure: Closure<DenyScope>,
    ) {
        deny(dependencyDelegation.path, ConfigureUtil.configureUsing(closure))
    }

    fun deny(
        dependencyPath: String,
        closure: Closure<DenyScope>,
    ) {
        deny(dependencyPath, ConfigureUtil.configureUsing(closure))
    }

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        closure: Closure<DenyScope>,
    ) {
        deny(provider, ConfigureUtil.configureUsing(closure))
    }

}
