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

import kotlinx.serialization.Serializable

@Serializable
internal data class DependencyGuardReport(
    val modules: List<ModuleReport>
)

@Serializable
internal data class ModuleReport(
    val module: String,
    val fatal: List<FatalMatch>,
    val suppressed: List<SuppressedMatch>
)

@Serializable
internal data class FatalMatch(
    val dependency: String,
    val pathToDependency: String,
    val reason: String,
)

@Serializable
internal data class SuppressedMatch(
    val dependency: String,
    val pathToDependency: String,
    val suppressionReason: String
)

