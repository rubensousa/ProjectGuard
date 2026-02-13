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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.DependencyRestrictionFinder
import com.rubensousa.dependencyguard.plugin.internal.DirectDependencyRestriction
import com.rubensousa.dependencyguard.plugin.internal.TransitiveDependencyRestriction
import com.rubensousa.dependencyguard.plugin.internal.report.DependencyGraphBuilder
import com.rubensousa.dependencyguard.plugin.internal.report.DependencyGraphDump
import com.rubensousa.dependencyguard.plugin.internal.report.JsonFileWriter
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDependencyReport
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDump
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionModuleReport
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

@DisableCachingByDefault(because = "Dump should always be generated")
internal abstract class TaskRestrictionDump : DefaultTask() {

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val specProperty: Property<DependencyGuardSpec>

    @get:InputFile
    internal abstract val dependenciesFile: RegularFileProperty

    @get:OutputFile
    internal abstract val outputFile: RegularFileProperty

    @TaskAction
    fun dependencyGuardRestrictionDump() {
        val executor = RestrictionDumpExecutor(
            moduleId = projectPath.get(),
            dependenciesFile = dependenciesFile.get().asFile,
            spec = specProperty.get(),
            outputFile = outputFile.get().asFile
        )
        executor.execute()
    }
}

internal class RestrictionDumpExecutor(
    val moduleId: String,
    val dependenciesFile: File,
    val spec: DependencyGuardSpec,
    val outputFile: File,
) {

    fun execute() {
        val dependencyGraphDump = Json.decodeFromString<DependencyGraphDump>(dependenciesFile.readText())
        val graphBuilder = DependencyGraphBuilder()
        val graphs = graphBuilder.buildFromDump(dependencyGraphDump)
        val restrictionFinder = DependencyRestrictionFinder()
        val restrictions = restrictionFinder.find(
            moduleId = moduleId,
            graphs = graphs,
            spec = spec
        )
        val module = RestrictionModuleReport(
            module = moduleId,
            restrictions = restrictions.map { restriction ->
                when (restriction) {
                    is DirectDependencyRestriction -> RestrictionDependencyReport(
                        dependency = restriction.dependencyId,
                        pathToDependency = null,
                        reason = restriction.reason
                    )

                    is TransitiveDependencyRestriction -> RestrictionDependencyReport(
                        dependency = restriction.dependencyId,
                        pathToDependency = restriction.getPathToDependencyText(),
                        reason = restriction.reason
                    )
                }
            }
        )
        val dump = RestrictionDump(listOf(module))
        val jsonWriter = JsonFileWriter()
        jsonWriter.writeToFile(dump, outputFile)
    }

}
