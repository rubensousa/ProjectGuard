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

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider

internal fun Project.getResolvedConfigurations(): Map<String, Provider<ResolvedComponentResult>> {
    val output = mutableMapOf<String, Provider<ResolvedComponentResult>>()
    project.configurations.forEach { config ->
        if (config.isCanBeResolved) {
            output[config.name] = config.incoming.resolutionResult.rootComponent
        }
    }
    return output
}

internal fun Set<DirectDependency>?.toIds(): List<String> {
    return this?.map { it.id } ?: emptyList()
}
