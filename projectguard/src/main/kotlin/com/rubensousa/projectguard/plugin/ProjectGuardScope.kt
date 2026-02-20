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

interface ProjectGuardScope {

    /**
     * Example:
     *
     * ```
     * val allowKotlinLibraries = restrictModuleRule {
     *    allow(libs.kotlinx.coroutines)
     *    allow(libs.kotlinx.datetime)
     * }
     * restrictModule(":domain") {
     *      // Domain modules can have access to kotlin libraries
     *      applyRule(allowKotlinLibraries)
     * }
     * ```
     */
    fun restrictModuleRule(action: Action<ModuleRestrictionScope>): RestrictModuleRule

    /**
     * Example:
     *
     * Prevent a module from depending on all other dependencies
     *
     * ```
     * restrictModule(":domain") {
     *      // Domain modules can only depend on another domain modules
     *      allow(":domain")
     * }
     * ```
     */
    fun restrictModule(
        modulePath: String,
        action: Action<ModuleRestrictionScope> = Action<ModuleRestrictionScope> { },
    )

    /**
     * Example:
     *
     * Prevent a module from depending on all other dependencies
     *
     * ```
     * restrictModule(projects.domain) {
     *      // Domain modules can only depend on another domain modules
     *      allow(":domain")
     * }
     * ```
     */
    fun restrictModule(
        moduleDelegation: DelegatingProjectDependency,
        action: Action<ModuleRestrictionScope> = Action<ModuleRestrictionScope> { }
    ) = restrictModule(modulePath = moduleDelegation.path, action = action)

    // Just here for groovy support
    fun restrictModule(modulePath: String, closure: Closure<ModuleRestrictionScope>) {
        restrictModule(modulePath, ConfigureUtil.configureUsing(closure))
    }

    /**
     * Example:
     *
     * ```
     * val denyAndroidLibraries = guardRule {
     *    deny("androidx")
     * }
     * guard(":domain") {
     *      // Domain modules should not depend on android libraries
     *      applyRule(denyAndroidLibraries)
     * }
     * ```
     */
    fun guardRule(action: Action<GuardScope>): GuardRule

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
     * guard(projects.domain) {
     *      // Domain modules should not depend on UI modules
     *      deny(":ui")
     * }
     * ```
     */
    fun guard(
        moduleDelegation: DelegatingProjectDependency,
        action: Action<GuardScope>
    ) = guard(modulePath = moduleDelegation.path, action = action)

    /**
     * Example:
     *
     * ```
     * val allowLegacyConsumers = restrictDependencyRule {
     *    allow(":legacy")
     *    allow(":old-feature")
     * }
     * restrictDependency(":legacy") {
     *      applyRule(allowLegacyConsumers)
     * }
     * ```
     */
    fun restrictDependencyRule(action: Action<DependencyRestrictionScope>): RestrictDependencyRule

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
        action: Action<DependencyRestrictionScope> = Action<DependencyRestrictionScope> { },
    )

    /**
     * Example:
     *
     * ```
     * restrictDependency(projects.legacy) {
     *      // Only legacy modules are allowed to depend on other legacy modules
     *      allow(":legacy")
     * }
     * ```
     */
    fun restrictDependency(
        dependencyDelegation: DelegatingProjectDependency,
        action: Action<DependencyRestrictionScope> = Action<DependencyRestrictionScope> { }
    ) = restrictDependency(dependencyPath = dependencyDelegation.path, action = action)

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
        action: Action<DependencyRestrictionScope> = Action<DependencyRestrictionScope> { },
    )

    // Just here for groovy support
    fun restrictDependency(
        dependencyPath: String,
        closure: Closure<DependencyRestrictionScope>
    ) {
        restrictDependency(dependencyPath, ConfigureUtil.configureUsing(closure))
    }

    // Just here for groovy support
    fun restrictDependency(
        provider: Provider<MinimalExternalModuleDependency>,
        closure: Closure<DependencyRestrictionScope>
    ) {
        restrictDependency(provider, ConfigureUtil.configureUsing(closure))
    }
}
