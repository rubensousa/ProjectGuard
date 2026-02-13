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

import com.rubensousa.dependencyguard.plugin.internal.report.JsonFileWriter
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionDump
import com.rubensousa.dependencyguard.plugin.internal.report.RestrictionModuleReport
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Final report should always be generated")
abstract class TaskAggregateRestrictionDump : DefaultTask() {

    @get:InputFiles
    abstract val dumpFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun dependencyGuardAggregateRestrictionDump() {
        val executor = AggregateRestrictionDumpExecutor(
            inputFiles = dumpFiles.files,
            outputFile = outputFile.get().asFile
        )
        executor.execute()
    }

}

internal class AggregateRestrictionDumpExecutor(
    private val inputFiles: Set<File>,
    private val outputFile: File,
) {

    fun execute() {
        val modules = mutableListOf<RestrictionModuleReport>()
        inputFiles.forEach { file ->
            runCatching {
                Json.decodeFromString<RestrictionDump>(file.readText())
            }.onSuccess { dump ->
                modules.addAll(dump.modules)
            }.onFailure {
                println("Unable to parse file ${file.path}")
            }
        }
        val dump = RestrictionDump(modules.sortedBy { it.module })
        val jsonWriter = JsonFileWriter()
        jsonWriter.writeToFile(dump, outputFile)
    }

}
