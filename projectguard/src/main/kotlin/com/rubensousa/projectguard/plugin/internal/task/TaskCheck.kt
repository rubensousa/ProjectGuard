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
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphDump
import com.rubensousa.projectguard.plugin.internal.report.HtmlReportGenerator
import com.rubensousa.projectguard.plugin.internal.report.RestrictionDump
import com.rubensousa.projectguard.plugin.internal.report.VerificationReportBuilder
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Baseline file might have changed")
internal abstract class TaskCheck : DefaultTask() {

    @get:InputFile
    internal abstract val baselineFile: RegularFileProperty

    @get:InputFile
    internal abstract val restrictionDumpFile: RegularFileProperty

    @get:InputFile
    internal abstract val dependenciesFile: RegularFileProperty

    @get:Input
    internal abstract val reportFilePath: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun projectGuardCheck() {
        val executor = CheckExecutor(
            baselineFile = baselineFile.get().asFile,
            restrictionDumpFile = restrictionDumpFile.get().asFile,
            dependenciesFile = dependenciesFile.get().asFile,
            reportDir = outputDir.get().asFile,
            reportFilePath = reportFilePath.get()
        )
        executor.execute().getOrThrow()
    }

}

internal class CheckExecutor(
    private val baselineFile: File,
    private val restrictionDumpFile: File,
    private val dependenciesFile: File,
    private val reportDir: File,
    private val reportFilePath: String = "",
) {

    fun execute(): Result<Unit> = runCatching {
        val dependencyGraphDump = Json.decodeFromString<DependencyGraphDump>(dependenciesFile.readText())
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
        val report = reportBuilder.build(dependencyGraphDump, restrictionDump)
        val fatalMatches = report.modules.flatMap { it.fatal }
        val suppressedMatches = report.modules.flatMap { it.suppressed }
        if (suppressedMatches.isNotEmpty()) {
            println("Found ${suppressedMatches.size} suppressed match(es)")
        }
        val htmlReportGenerator = HtmlReportGenerator()
        htmlReportGenerator.generate(report, reportDir)
        if (fatalMatches.isNotEmpty()) {
            throw VerificationException(
                "${fatalMatches.take(10).joinToString("\n\n") { it.getDescription() }} \n " +
                        "Found ${fatalMatches.size} fatal match(es). See full report at file:///$reportFilePath"
            )
        } else {
            println("No fatal matches found. See report at file:///$reportFilePath")
        }
    }

}
