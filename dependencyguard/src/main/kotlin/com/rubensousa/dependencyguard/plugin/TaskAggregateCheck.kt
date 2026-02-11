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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardReport
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class TaskAggregateCheck : DefaultTask() {

    @get:InputFile
    internal abstract val reportFile: RegularFileProperty

    @TaskAction
    fun dependencyGuardCheck() {
        val report = Json.decodeFromString<DependencyGuardReport>(reportFile.get().asFile.readText())
        var suppressedMatches = 0
        val fatalMatches = mutableListOf<RestrictionMatch>()
        report.modules.forEach { moduleReport ->
            suppressedMatches += moduleReport.suppressed.size
            moduleReport.fatal.forEach { fatalMatch ->
                fatalMatches.add(
                    RestrictionMatch(
                        module = moduleReport.module,
                        dependency = fatalMatch.dependency,
                        pathToDependency = fatalMatch.pathToDependency,
                        reason = fatalMatch.reason
                    )
                )
            }
        }
        if (suppressedMatches > 0) {
            logger.warn("Found $suppressedMatches suppressed match(es)")
        }
        if (fatalMatches.isNotEmpty()) {
            logger.error("Found ${fatalMatches.size} fatal match(es)")
            throw GradleException(fatalMatches.joinToString("\n\n") { it.asText() })
        }
    }

}
