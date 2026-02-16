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

package com.rubensousa.projectguard.plugin.internal.task

import com.rubensousa.projectguard.plugin.internal.BaselineConfiguration
import com.rubensousa.projectguard.plugin.internal.SuppressionMap
import com.rubensousa.projectguard.plugin.internal.YamlProcessor
import com.rubensousa.projectguard.plugin.internal.report.RestrictionDump
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
internal abstract class TaskBaseline : DefaultTask() {

    @get:InputFile
    internal abstract val restrictionDumpFile: RegularFileProperty

    @get:OutputFile
    internal abstract val outputFile: RegularFileProperty

    @TaskAction
    fun projectGuardBaseline() {
        val executor = BaselineExecutor(
            inputFile = restrictionDumpFile.get().asFile,
            outputFile = outputFile.get().asFile
        )
        executor.execute()
    }

}

internal class BaselineExecutor(
    private val inputFile: File,
    private val outputFile: File,
) {

    private val defaultSuppressionReason = "Suppressed from baseline"

    fun execute() {
        val restrictionDump = Json.decodeFromString<RestrictionDump>(inputFile.readText())
        val yamlProcessor = YamlProcessor()
        val currentReasons = mutableMapOf<String, MutableMap<String, String>>()
        runCatching {
            val currentConfiguration = yamlProcessor.parse(outputFile, BaselineConfiguration::class.java)
            currentConfiguration.suppressions.forEach { (module, dependencies) ->
                dependencies.forEach { (dependency, reason) ->
                    val dependencyReasons = currentReasons.getOrPut(module) { mutableMapOf() }
                    dependencyReasons[dependency] = reason
                }
            }
        }
        val suppressionMap = SuppressionMap()
        restrictionDump.modules.forEach { report ->
            report.restrictions.forEach { dependencyReport ->
                suppressionMap.add(
                    module = report.module,
                    dependency = dependencyReport.dependency,
                    reason = currentReasons[report.module]?.let { dependencyReasons ->
                        dependencyReasons[dependencyReport.dependency] ?: defaultSuppressionReason
                    } ?: defaultSuppressionReason
                )
            }
        }
        yamlProcessor.write(outputFile, suppressionMap.getBaseline())
    }

}
