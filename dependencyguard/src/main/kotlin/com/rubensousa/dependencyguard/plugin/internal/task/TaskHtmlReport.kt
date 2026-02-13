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

package com.rubensousa.dependencyguard.plugin.internal.task

import com.rubensousa.dependencyguard.plugin.internal.BaselineConfiguration
import com.rubensousa.dependencyguard.plugin.internal.SuppressionMap
import com.rubensousa.dependencyguard.plugin.internal.YamlProcessor
import com.rubensousa.dependencyguard.plugin.internal.report.HtmlReportGenerator
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDump
import com.rubensousa.dependencyguard.plugin.internal.report.VerificationReportBuilder
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "HTML report should always be regenerated")
internal abstract class TaskHtmlReport : DefaultTask() {

    @get:InputFile
    abstract val restrictionDumpFile: RegularFileProperty

    @get:InputFile
    abstract val baselineFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun dependencyGuardHtmlReport() {
        val executor = HtmlReportExecutor(
            restrictionDumpFile = restrictionDumpFile.get().asFile,
            baselineFile = baselineFile.get().asFile,
            outputFile = outputDir.get().asFile
        )
        executor.execute()
    }

}

internal class HtmlReportExecutor(
    private val restrictionDumpFile: File,
    private val baselineFile: File,
    private val outputFile: File,
) {

    fun execute() {
        val yamlProcessor = YamlProcessor()
        val suppressionMap = SuppressionMap()
        runCatching {
            yamlProcessor.parse(baselineFile, BaselineConfiguration::class.java)
        }.onSuccess { config ->
            suppressionMap.set(config)
        }
        val restrictionDump = Json.decodeFromString<RestrictionDump>(restrictionDumpFile.readText())
        val verificationReportBuilder = VerificationReportBuilder(suppressionMap)
        val verificationReport = verificationReportBuilder.build(restrictionDump)
        val htmlReportGenerator = HtmlReportGenerator()
        htmlReportGenerator.generate(verificationReport, outputFile)
    }
}