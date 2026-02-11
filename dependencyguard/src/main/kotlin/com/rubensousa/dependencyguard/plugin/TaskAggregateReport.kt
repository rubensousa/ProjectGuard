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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardReportBuilder
import com.rubensousa.dependencyguard.plugin.internal.JsonFileWriter
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Final report should always be generated")
abstract class TaskAggregateReport : DefaultTask() {

    @get:InputFiles
    abstract val reportFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportLocation: RegularFileProperty

    @TaskAction
    fun dependencyGuardAggregateReport() {
        val matches = reportFiles.files.flatMap { file ->
            if (file.exists()) {
                runCatching {
                    Json.decodeFromString<List<RestrictionMatch>>(file.readText())
                }.onFailure {
                    logger.warn("Unable to parse file ${file.path}")
                }.getOrNull().orEmpty()
            } else {
                emptyList()
            }
        }
        val report = DependencyGuardReportBuilder().build(matches)
        val jsonWriter = JsonFileWriter()
        jsonWriter.writeToFile(report, reportLocation.get().asFile)
    }

}
