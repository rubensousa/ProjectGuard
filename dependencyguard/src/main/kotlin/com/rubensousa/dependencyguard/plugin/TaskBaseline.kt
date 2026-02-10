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
import com.rubensousa.dependencyguard.plugin.internal.SuppressionMap
import com.rubensousa.dependencyguard.plugin.internal.YamlProcessor
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class TaskBaseline : DefaultTask() {

    @get:InputFile
    internal abstract val jsonReport: RegularFileProperty

    @get:OutputFile
    internal abstract val baselineFileReference: RegularFileProperty

    @TaskAction
    fun dependencyGuardBaseline() {
        val jsonReportFile = jsonReport.get().asFile
        if (!jsonReportFile.exists()) {
            return
        }
        val aggregatedReport = Json.decodeFromString<DependencyGuardReport>(
            jsonReportFile.readText()
        )
        val file = baselineFileReference.asFile.get()
        val yamlProcessor = YamlProcessor()
        val suppressionMap = SuppressionMap()
        aggregatedReport.modules.forEach { report ->
            report.fatal.forEach { fatalMatch ->
                suppressionMap.add(
                    module = report.module,
                    dependency = fatalMatch.dependency,
                    reason = "${report.module} -> ${fatalMatch.pathToDependency}"
                )
            }
        }
        yamlProcessor.write(file, suppressionMap.getConfiguration())
    }

}
