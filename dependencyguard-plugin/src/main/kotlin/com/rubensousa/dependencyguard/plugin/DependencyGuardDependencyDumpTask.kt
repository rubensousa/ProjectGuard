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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraph
import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphConfiguration
import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphReport
import com.rubensousa.dependencyguard.plugin.internal.JsonFileWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class DependencyGuardDependencyDumpTask : DefaultTask() {

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val dependencies: ListProperty<DependencyGraph>

    @get:OutputFile
    internal abstract val dependenciesFile: RegularFileProperty

    private val jsonWriter = JsonFileWriter()

    @TaskAction
    fun dependencyGuardDependencyDump() {
        val module = projectPath.get()
        val file = dependenciesFile.get().asFile
        file.delete()
        val graphReport = DependencyGraphReport(
            module = module,
            configurations = dependencies.get().map { graph ->
                DependencyGraphConfiguration(
                    id = graph.configurationId,
                    dependencies = graph.getDependencies(module).toList()
                )
            }
        )
        if (graphReport.configurations.isNotEmpty()) {
            jsonWriter.writeToFile(graphReport, file)
        }
    }

}
