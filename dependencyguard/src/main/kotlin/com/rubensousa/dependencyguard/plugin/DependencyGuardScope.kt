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

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

interface DependencyGuardScope {

    /**
     * Example:
     *
     * ```
     * guard(":domain") {
     *      // Domain modules should not depend on UI modules
     *      deny(":ui")
     * }
     * ```
     */
    fun guard(
        modulePath: String,
        action: Action<GuardScope>,
    )

    /**
     * Example:
     *
     * ```
     * restrictDependency(":legacy") {
     *      // Only legacy modules are allowed to depend on other legacy modules
     *      allow(":legacy")
     * }
     * ```
     */
    fun restrictDependency(
        dependencyPath: String,
        action: Action<DependencyRestrictionScope>,
    )

    /**
     * Example:
     *
     * ```
     * restrictDependency(libs.mockk) {
     *      // Only legacy modules are allowed to use mockk for tests
     *      allow(":legacy")
     * }
     * ```
     */
    fun restrictDependency(
        provider: Provider<MinimalExternalModuleDependency>,
        action: Action<DependencyRestrictionScope>,
    )

    // Just here for groovy support
    fun restrictDependency(
        dependencyPath: String,
    ) {
        restrictDependency(dependencyPath, defaultDependencyRestrictionScope)
    }

    // Just here for groovy support
    fun restrictDependency(
        provider: Provider<MinimalExternalModuleDependency>,
    ) {
        restrictDependency(provider, defaultDependencyRestrictionScope)
    }

    companion object {
        private val defaultDependencyRestrictionScope = Action<DependencyRestrictionScope> {}
    }
}
