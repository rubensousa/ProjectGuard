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

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

interface GuardScope {

    fun deny(
        dependencyPath: String,
        action: Action<DenyScope>,
    )

    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DenyScope>,
    )

    // Required for groovy compatibility
    fun deny(
        dependencyPath: String,
    ) {
        deny(dependencyPath, defaultDenyScope)
    }

    // Required for groovy compatibility
    fun deny(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        deny(provider, defaultDenyScope)
    }

    fun applyRule(rule: GuardRule)

    companion object {
        internal val defaultDenyScope = Action<DenyScope> { }
    }
}
