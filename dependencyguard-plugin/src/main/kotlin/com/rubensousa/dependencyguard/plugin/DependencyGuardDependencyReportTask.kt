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
