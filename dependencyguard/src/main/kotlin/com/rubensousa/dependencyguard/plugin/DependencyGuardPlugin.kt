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

import com.rubensousa.dependencyguard.plugin.internal.report.DependencyGraphBuilder
import com.rubensousa.dependencyguard.plugin.internal.task.TaskAggregateDependencyDump
import com.rubensousa.dependencyguard.plugin.internal.task.TaskAggregateRestrictionDump
import com.rubensousa.dependencyguard.plugin.internal.task.TaskBaseline
import com.rubensousa.dependencyguard.plugin.internal.task.TaskCheck
import com.rubensousa.dependencyguard.plugin.internal.task.TaskCreateBaselineFile
import com.rubensousa.dependencyguard.plugin.internal.task.TaskDependencyDump
import com.rubensousa.dependencyguard.plugin.internal.task.TaskHtmlReport
import com.rubensousa.dependencyguard.plugin.internal.task.TaskRestrictionDump
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * How the plugin works:
 *
 * - Generate a dependency dump of all modules
 * - Aggregate the dependency dumps to form the entire project dependency graph
 * - Save the restrictions to individual module reports by checking them against the spec defined by [DependencyGuardExtension]
 * - Aggregate the individual reports to form the global project state
 * - Read the baseline file
 * - Compare baseline with current restrictions
 *
 * Public facing tasks:
 *
 * - dependencyGuardCheck
 * - dependencyGuardBaseline
 * - dependencyGuardHtmlReport
 */
class DependencyGuardPlugin : Plugin<Project> {

    private val pluginId = "dependencyguard"
    private val baselineFilePath = "$pluginId-baseline.yml"
    private val htmlAggregateReportFilePath = "reports/$pluginId"
    private val dependenciesFilePath = "reports/$pluginId/dependencies.json"
    private val jsonReportFilePath = "reports/$pluginId/report.json"
    private val graphBuilder = DependencyGraphBuilder()

    override fun apply(target: Project) {
        val rootProject = target.rootProject
        if (rootProject != target) {
            // Apply only in the root project for now
            return
        }
        val extension = getExtension(rootProject)
        val aggregationTasks = createAggregationTasks(rootProject)
        val individualModuleTasks = mutableListOf<ModuleTasks>()
        rootProject.subprojects.forEach { targetProject ->
            val moduleTasks = createModuleTasks(
                targetProject = targetProject,
                extension = extension,
            )
            individualModuleTasks.add(moduleTasks)
            setupModuleTasks(
                aggregationTasks = aggregationTasks,
                moduleTasks = moduleTasks
            )
        }
        setupAggregationTasks(
            rootProject = rootProject,
            aggregationTasks = aggregationTasks,
            moduleTasks = individualModuleTasks
        )
    }

    /**
     * The extension is global and should only be accessible from the root project
     */
    private fun getExtension(rootProject: Project): DependencyGuardExtension {
        return rootProject.extensions.findByType(DependencyGuardExtension::class.java)
            ?: rootProject.extensions.create(
                "dependencyGuard",
                DependencyGuardExtension::class.java
            )
    }

    private fun setupModuleTasks(
        aggregationTasks: AggregationTasks,
        moduleTasks: ModuleTasks,
    ) {
        // Report task must take the aggregate dependencies as input
        moduleTasks.restrictionDump.configure {
            dependenciesFile.set(aggregationTasks.dependencyDump.flatMap { task -> task.outputFile })
        }

        // HTML report task takes the individual module report
        moduleTasks.htmlReport.configure {
            restrictionDumpFile.set(moduleTasks.restrictionDump.flatMap { task -> task.outputFile })
            baselineFile.set(aggregationTasks.baselineCreate.flatMap { task -> task.baselineFile })
            outputDir.set(project.layout.buildDirectory.dir(htmlAggregateReportFilePath))
            mustRunAfter(aggregationTasks.restrictionDump)
        }

        moduleTasks.check.configure {
            // Check task must take the report and baseline as input
            restrictionDumpFile.set(moduleTasks.restrictionDump.flatMap { task -> task.outputFile })
            baselineFile.set(aggregationTasks.baselineCreate.flatMap { task -> task.baselineFile })
            reportFilePath.set(getProjectReportFilePath(project))
            // Run the html report after the check task
            finalizedBy(moduleTasks.htmlReport)
        }
    }

    private fun setupAggregationTasks(
        rootProject: Project,
        aggregationTasks: AggregationTasks,
        moduleTasks: List<ModuleTasks>,
    ) {
        aggregationTasks.dependencyDump.configure {
            // Take all individual dependency dumps to aggregate them
            dependencyFiles.from(moduleTasks.map { task -> task.dependencyDump.flatMap { it.outputFile } })
            outputFile.set(rootProject.layout.buildDirectory.file(dependenciesFilePath))
        }

        // Ensure that the baseline file always exists.
        aggregationTasks.baselineCreate.configure {
            val file = rootProject.file(baselineFilePath)
            baselineFile.set(file)
            // Mark task out of date only when the baseline file does not exist
            outputs.upToDateWhen { file.exists() }
        }

        // Baseline task needs to take the aggregate report of all matches
        aggregationTasks.baselineDump.configure {
            restrictionDumpFile.set(aggregationTasks.restrictionDump.flatMap { task -> task.outputFile })
            outputFile.set(aggregationTasks.baselineCreate.flatMap { task -> task.baselineFile })
        }

        aggregationTasks.restrictionDump.configure {
            // Aggregate report must take the individual module reports
            dumpFiles.from(moduleTasks.map { it.restrictionDump.flatMap { task -> task.outputFile } })
            outputFile.set(rootProject.layout.buildDirectory.file(jsonReportFilePath))
        }

        // Aggregate dependency dump must take the individual module contribution
        aggregationTasks.dependencyDump.configure {
            outputFile.set(rootProject.layout.buildDirectory.file(dependenciesFilePath))
        }

        // Aggregate html report takes the aggregate json and prettifies it
        aggregationTasks.htmlReport.configure {
            restrictionDumpFile.set(aggregationTasks.restrictionDump.flatMap { task -> task.outputFile })
            baselineFile.set(aggregationTasks.baselineCreate.flatMap { task -> task.baselineFile })
            outputDir.set(rootProject.layout.buildDirectory.dir(htmlAggregateReportFilePath))
        }

        aggregationTasks.check.configure {
            restrictionDumpFile.set(aggregationTasks.restrictionDump.flatMap { task -> task.outputFile })
            baselineFile.set(aggregationTasks.baselineCreate.flatMap { task -> task.baselineFile })
            reportFilePath.set(getProjectReportFilePath(project))
            setMustRunAfter(moduleTasks.map { it.htmlReport })
            // Run the html report after the check task
            finalizedBy(aggregationTasks.htmlReport)
        }
    }

    private fun getProjectReportFilePath(project: Project): String {
        val file = project.layout.buildDirectory.asFile.get()
        val dir = File(file, "reports/$pluginId")
        dir.mkdirs()
        val reportFile = File(dir, "index.html")
        return reportFile.absolutePath.replace(File.separatorChar, '/')
    }

    private fun createAggregationTasks(
        rootProject: Project,
    ): AggregationTasks {
        return AggregationTasks(
            dependencyDump = createAggregateDependencyDumpTask(rootProject),
            restrictionDump = createAggregateRestrictionTask(rootProject),
            htmlReport = createHtmlReportTask(rootProject),
            baselineDump = createBaselineTask(rootProject),
            baselineCreate = createBaselineReferenceTask(rootProject),
            check = createCheckTask(rootProject)
        )
    }

    private fun createAggregateDependencyDumpTask(
        rootProject: Project,
    ): TaskProvider<TaskAggregateDependencyDump> {
        return rootProject.tasks.register(
            "dependencyGuardAggregateDependencyDump",
            TaskAggregateDependencyDump::class.java
        ) {
            group = "other"
            description = "Generates an aggregate JSON report of all dependencies of this project."
            outputs.upToDateWhen { false }
        }
    }

    private fun createAggregateRestrictionTask(
        rootProject: Project,
    ): TaskProvider<TaskAggregateRestrictionDump> {
        return rootProject.tasks.register(
            "dependencyGuardAggregateRestrictionDump",
            TaskAggregateRestrictionDump::class.java
        ) {
            group = "other"
            description = "Generates an aggregate JSON report of all dependency matches."
            outputs.upToDateWhen { false }
        }
    }

    private fun createBaselineTask(
        rootProject: Project,
    ): TaskProvider<TaskBaseline> {
        return rootProject.tasks.register(
            "dependencyGuardBaseline",
            TaskBaseline::class.java
        ) {
            group = "verification"
            description = "Generates a YAML file containing the baseline of suppressions for this project."
            outputs.upToDateWhen { false }
        }
    }

    private fun createBaselineReferenceTask(
        rootProject: Project,
    ): TaskProvider<TaskCreateBaselineFile> {
        return rootProject.tasks.register(
            "dependencyGuardCreateBaselineFile",
            TaskCreateBaselineFile::class.java
        ) {
            group = "other"
            description = "Ensures that the baseline file exists"
            outputs.upToDateWhen { false }
        }
    }

    private fun createModuleTasks(
        targetProject: Project,
        extension: DependencyGuardExtension,
    ): ModuleTasks {
        return ModuleTasks(
            check = createCheckTask(targetProject),
            restrictionDump = createModuleRestrictionTask(targetProject, extension),
            htmlReport = createHtmlReportTask(targetProject),
            dependencyDump = createDependencyDumpTask(targetProject)
        )
    }

    private fun createCheckTask(project: Project): TaskProvider<TaskCheck> {
        return project.tasks.register(
            "dependencyGuardCheck",
            TaskCheck::class.java
        ) {
            group = "verification"
            description = "Verifies if there are any dependency restrictions"
        }
    }

    private fun createModuleRestrictionTask(
        targetProject: Project,
        extension: DependencyGuardExtension,
    ): TaskProvider<TaskRestrictionDump> {
        return targetProject.tasks.register(
            "dependencyGuardRestrictionDump",
            TaskRestrictionDump::class.java
        ) {
            group = "other"
            description = "Generates a JSON report of all dependency restrictions for this module."
            projectPath.set(project.path)
            specProperty.set(extension.getSpec())
            outputFile.set(
                project.layout.buildDirectory.file(jsonReportFilePath)
            )
            outputs.upToDateWhen { false }
        }
    }

    private fun createHtmlReportTask(
        project: Project,
    ): TaskProvider<TaskHtmlReport> {
        return project.tasks.register(
            "dependencyGuardHtmlReport",
            TaskHtmlReport::class.java
        ) {
            group = "reporting"
            description = "Generates an HTML report of all dependency matches."
            outputs.upToDateWhen { false }
        }
    }

    private fun createDependencyDumpTask(
        targetProject: Project,
    ): TaskProvider<TaskDependencyDump> {
        return targetProject.tasks.register(
            "dependencyGuardDependencyDump",
            TaskDependencyDump::class.java
        ) {
            group = "other"
            description = "Generates a JSON containing the dependencies of this module."
            projectPath.set(project.path)
            dependencies.set(graphBuilder.buildFromProject(project))
            outputFile.set(
                project.layout.buildDirectory.file(dependenciesFilePath)
            )
            outputs.upToDateWhen { false }
        }
    }

    private data class ModuleTasks(
        val dependencyDump: TaskProvider<TaskDependencyDump>,
        val restrictionDump: TaskProvider<TaskRestrictionDump>,
        val htmlReport: TaskProvider<TaskHtmlReport>,
        val check: TaskProvider<TaskCheck>,
    )

    private data class AggregationTasks(
        val baselineCreate: TaskProvider<TaskCreateBaselineFile>,
        val baselineDump: TaskProvider<TaskBaseline>,
        val restrictionDump: TaskProvider<TaskAggregateRestrictionDump>,
        val check: TaskProvider<TaskCheck>,
        val htmlReport: TaskProvider<TaskHtmlReport>,
        val dependencyDump: TaskProvider<TaskAggregateDependencyDump>,
    )

}
