package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardReport
import com.rubensousa.dependencyguard.plugin.internal.ModuleReport
import com.rubensousa.dependencyguard.plugin.internal.Match
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Final report should always be generated")
abstract class DependencyGuardAggregateReportTask : DefaultTask() {

    @get:InputFiles
    abstract val violationFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportLocation: RegularFileProperty

    private val json = Json { prettyPrint = true }

    @TaskAction
    fun generateReport() {
        val allViolations = violationFiles.files.flatMap { file ->
            if (file.exists()) {
                Json.decodeFromString<List<RestrictionMatch>>(file.readText())
            } else {
                emptyList()
            }
        }

        val report = DependencyGuardReport(
            modules = allViolations.groupBy { it.modulePath }
                .map { (modulePath, matches) ->
                    ModuleReport(
                        module = modulePath,
                        fatalMatches = matches.filter { !it.isExcluded }.map { match ->
                            mapMatch(match)
                        },
                        excludedMatches = matches.filter { it.isExcluded }.map { match ->
                            mapMatch(match)
                        }
                    )
                }.sortedBy { it.module }
        )
        val jsonReport = json.encodeToString(report)
        reportLocation.get().asFile.apply {
            parentFile.mkdirs()
            writeText(jsonReport)
        }
    }

    private fun mapMatch(match: RestrictionMatch): Match {
        return Match(
            dependency = match.dependencyPath,
            reason = match.reason,
        )
    }
}
