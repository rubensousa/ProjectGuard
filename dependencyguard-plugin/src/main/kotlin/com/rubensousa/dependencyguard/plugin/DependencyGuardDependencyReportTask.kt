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
import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphReport
import com.rubensousa.dependencyguard.plugin.internal.JsonFileWriter
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class DependencyGuardDependencyReportTask : DefaultTask() {

    @get:InputFiles
    abstract val dependencyFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val jsonWriter = JsonFileWriter()

    @TaskAction
    fun dependencyGuardDependencyDumpAggregate() {
        val reports = mutableListOf<DependencyGraphReport>()
        dependencyFiles.files.forEach { file ->
            if (file.exists()) {
                reports.add(Json.decodeFromString<DependencyGraphReport>(file.readText()))
            }
        }
        val aggregateReport = DependencyGraphAggregateReport(reports)
        val outputFile = output.asFile.get()
        jsonWriter.writeToFile(aggregateReport, outputFile)
    }

}
