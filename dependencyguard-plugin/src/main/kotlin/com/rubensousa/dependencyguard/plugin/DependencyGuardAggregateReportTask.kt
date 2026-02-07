package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardReport
import com.rubensousa.dependencyguard.plugin.internal.FatalMatch
import com.rubensousa.dependencyguard.plugin.internal.ModuleReport
import com.rubensousa.dependencyguard.plugin.internal.SuppressedMatch
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
    abstract val reportFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportLocation: RegularFileProperty

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    @TaskAction
    fun generateReport() {
        val restrictionMatches = reportFiles.files.flatMap { file ->
            if (file.exists()) {
                Json.decodeFromString<List<RestrictionMatch>>(file.readText())
            } else {
                emptyList()
            }
        }

        val report = DependencyGuardReport(
            modules = restrictionMatches.groupBy { it.modulePath }
                .map { (modulePath, matches) ->
                    ModuleReport(
                        module = modulePath,
                        fatal = matches.filter { !it.isSuppressed }.map { match ->
                            FatalMatch(
                                dependency = match.dependencyPath,
                                reason = match.reason,
                            )
                        },
                        suppressed = matches.filter { it.isSuppressed }.map { match ->
                            SuppressedMatch(
                                dependency = match.dependencyPath,
                                suppressionReason = match.suppressionReason,
                            )
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

}
