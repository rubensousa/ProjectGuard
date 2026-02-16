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

package com.rubensousa.projectguard.plugin.internal.report

import kotlinx.serialization.Serializable

@Serializable
internal data class RestrictionDump(
    val modules: List<RestrictionModuleReport>
)

@Serializable
internal data class RestrictionModuleReport(
    val module: String,
    val restrictions: List<RestrictionDependencyReport>
)

@Serializable
internal data class RestrictionDependencyReport(
    val dependency: String,
    val pathToDependency: String?, // Null -> direct dependency
    val reason: String,
)
