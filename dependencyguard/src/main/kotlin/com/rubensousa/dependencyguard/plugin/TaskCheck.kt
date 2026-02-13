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

import com.rubensousa.dependencyguard.plugin.internal.BaselineConfiguration
import com.rubensousa.dependencyguard.plugin.internal.SuppressionMap
import com.rubensousa.dependencyguard.plugin.internal.YamlProcessor
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDump
import com.rubensousa.dependencyguard.plugin.internal.report.VerificationReportBuilder
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Baseline file might have changed")
abstract class TaskCheck : DefaultTask() {

    @get:InputFile
    internal abstract val baselineFile: RegularFileProperty

    @get:InputFile
    internal abstract val restrictionDumpFile: RegularFileProperty

    @TaskAction
    fun dependencyGuardCheck() {
        val executor = CheckExecutor(
            baselineFile = baselineFile.get().asFile,
            restrictionDumpFile = restrictionDumpFile.get().asFile,
            logger = logger
        )
        executor.execute().getOrThrow()
    }

}

internal class CheckExecutor(
    private val baselineFile: File,
    private val restrictionDumpFile: File,
    private val logger: Logger? = null,
) {

    fun execute(): Result<Unit> = runCatching {
        val yamlProcessor = YamlProcessor()
        val suppressionMap = SuppressionMap()
        runCatching {
            yamlProcessor.parse(baselineFile, BaselineConfiguration::class.java)
        }.onSuccess { config ->
            suppressionMap.set(config)
        }.onFailure {
            println("Skipping baseline since it could not be found or was improperly structured!")
        }
        val restrictionDump = Json.decodeFromString<RestrictionDump>(restrictionDumpFile.readText())
        val reportBuilder = VerificationReportBuilder(suppressionMap)
        val report = reportBuilder.build(restrictionDump)
        val fatalMatches = report.modules.flatMap { it.fatal }
        val suppressedMatches = report.modules.flatMap { it.suppressed }
        if (suppressedMatches.isNotEmpty()) {
            println("Found ${suppressedMatches.size} suppressed match(es)")
        }
        if (fatalMatches.isNotEmpty()) {
            logger?.error("Found ${fatalMatches.size} fatal match(es)")
            throw GradleException(fatalMatches.joinToString("\n\n") { it.getDescription() })
        }
    }

}
