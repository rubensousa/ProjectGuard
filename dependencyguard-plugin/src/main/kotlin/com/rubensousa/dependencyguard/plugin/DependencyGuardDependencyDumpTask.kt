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
