package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphAggregateReport
import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphBuilder
import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatchProcessor
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class DependencyGuardCheckTask : DefaultTask() {

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val specProperty: Property<DependencyGuardSpec>

    @get:InputFile
    internal abstract val dependencyFile: RegularFileProperty

    @TaskAction
    fun dependencyGuardCheck() {
        val spec = specProperty.get()
        if (spec.isEmpty()) {
            return
        }
        val aggregateReport = Json.decodeFromString<DependencyGraphAggregateReport>(
            dependencyFile.get().asFile.readText()
        )
        val graphBuilder = DependencyGraphBuilder()
        val graphs = graphBuilder.buildFromReport(aggregateReport)
        val currentModulePath = projectPath.get()
        val matches = mutableListOf<RestrictionMatch>()
        val restrictionChecker = RestrictionChecker()
        graphs.forEach { graph ->
            matches.addAll(
                restrictionChecker.findMatches(
                    modulePath = currentModulePath,
                    dependencyGraph = graph,
                    spec = spec
                )
            )
        }
        val processor = RestrictionMatchProcessor()
        val processedMatches = processor.process(matches)
        val suppressedViolations = processedMatches.filter { it.isSuppressed }
        val fatalViolations = processedMatches.filter { !it.isSuppressed }
        if (suppressedViolations.isNotEmpty()) {
            logger.warn("Found ${suppressedViolations.size} suppressed violation(s)")
        }
        if (fatalViolations.isNotEmpty()) {
            logger.error("Found ${fatalViolations.size} fatal violation(s)")
            throw GradleException(fatalViolations.joinToString("\n\n") { it.asText() })
        }
    }

}
