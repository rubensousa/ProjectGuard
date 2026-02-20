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

import com.rubensousa.projectguard.plugin.internal.DependencyGraph
import com.rubensousa.projectguard.plugin.internal.report.ConfigurationDependencies
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphDump
import com.rubensousa.projectguard.plugin.internal.report.DependencyGraphModuleDump
import com.rubensousa.projectguard.plugin.internal.report.DependencyReferenceDump
import com.rubensousa.projectguard.plugin.internal.report.JsonFileWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
internal abstract class TaskDependencyDump : DefaultTask() {

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val dependencyGraph: Property<DependencyGraph>

    @get:OutputFile
    internal abstract val outputFile: RegularFileProperty

    @TaskAction
    fun projectGuardDependencyDump() {
        val executor = DependencyDumpExecutor(
            moduleId = projectPath.get(),
            outputFile = outputFile.get().asFile,
            dependencyGraph = dependencyGraph.get()
        )
        executor.execute()
    }

}

internal class DependencyDumpExecutor(
    private val moduleId: String,
    private val outputFile: File,
    private val dependencyGraph: DependencyGraph,
) {

    private val jsonWriter = JsonFileWriter()

    fun execute() {
        val dependencyDump = DependencyGraphDump(
            modules = listOf(
                DependencyGraphModuleDump(
                    module = moduleId,
                    configurations = dependencyGraph.getConfigurations().map { configuration ->
                        ConfigurationDependencies(
                            id = configuration.id,
                            dependencies = configuration.getDependencies(moduleId).map { dependency ->
                                DependencyReferenceDump(
                                    id = dependency.id,
                                    isLibrary = dependency.isLibrary
                                )
                            }
                        )
                    }
                )
            ),
        )
        jsonWriter.writeToFile(dependencyDump, outputFile)
    }

}

