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

import kotlin.collections.component1
import kotlin.collections.component2

internal class DependencyGuardReportBuilder {

    fun build(matches: List<RestrictionMatch>): DependencyGuardReport {
        return DependencyGuardReport(
            modules = matches.groupBy { match -> match.module }
                .map { (modulePath, matches) ->
                    ModuleReport(
                        module = modulePath,
                        fatal = matches.filter { !it.isSuppressed }.map { match ->
                            FatalMatch(
                                dependency = match.dependency,
                                pathToDependency = match.pathToDependency,
                                reason = match.reason,
                            )
                        },
                        suppressed = matches.filter { it.isSuppressed }.map { match ->
                            SuppressedMatch(
                                dependency = match.dependency,
                                pathToDependency = match.pathToDependency,
                                suppressionReason = match.suppressionReason,
                            )
                        }
                    )
                }.sortedBy { match -> match.module }
        )
    }

}
