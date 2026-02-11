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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphAggregateReport
import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphBuilder
import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatchProcessor
import com.rubensousa.dependencyguard.plugin.internal.SuppressionConfiguration
import com.rubensousa.dependencyguard.plugin.internal.SuppressionMap
import com.rubensousa.dependencyguard.plugin.internal.YamlProcessor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Report should always be generated")
abstract class TaskReport : DefaultTask() {

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val specProperty: Property<DependencyGuardSpec>

    @get:InputFile
    internal abstract val dependencyFile: RegularFileProperty

    @get:OutputFile
    internal abstract val reportFile: RegularFileProperty

    @get:Input
    internal abstract val baselineFilePath: Property<String>

    @TaskAction
    fun dependencyGuardReport() {
        val spec = specProperty.get()
        reportFile.get().asFile.delete()
        val currentModulePath = projectPath.get()
        val aggregateReport = Json.decodeFromString<DependencyGraphAggregateReport>(
            dependencyFile.get().asFile.readText()
        )
        val graphBuilder = DependencyGraphBuilder()
        val graphs = graphBuilder.buildFromReport(aggregateReport)
        val matches = mutableListOf<RestrictionMatch>()
        val suppressionMap = SuppressionMap()
        val baselineFile = File(baselineFilePath.get())
        if (baselineFile.exists()) {
            val yamlProcessor = YamlProcessor()
            suppressionMap.set(
                yamlProcessor.parse(
                    baselineFile,
                    SuppressionConfiguration::class.java
                )
            )
        }
        val restrictionChecker = RestrictionChecker(suppressionMap)
        val processor = RestrictionMatchProcessor()
        graphs.forEach { graph ->
            matches.addAll(
                restrictionChecker.findMatches(
                    modulePath = currentModulePath,
                    dependencyGraph = graph,
                    spec = spec
                )
            )
        }
        val processedMatches = processor.process(matches)
        reportFile.get().asFile.writeText(Json.encodeToString(processedMatches))
    }
}
