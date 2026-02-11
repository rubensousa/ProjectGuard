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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

class DependencyGuardPlugin : Plugin<Project> {

    private val pluginId = "dependencyguard"
    private val baselineFilePath = "$pluginId.yml"
    private val jsonAggregateReportFilePath = "reports/$pluginId/project-report.json"
    private val htmlAggregateReportFilePath = "reports/$pluginId/project-report.html"
    private val dependenciesFilePath = "reports/$pluginId/dependencies.json"
    private val jsonReportFilePath = "reports/$pluginId/report.json"
    private val htmlReportFilePath = "reports/$pluginId/report.html"
    private val graphBuilder = DependencyGraphBuilder()

    override fun apply(target: Project) {
        val rootProject = target.rootProject
        if (rootProject != target) {
            // Apply only in the root project for now
            return
        }
        val extension = getExtension(rootProject)
        val aggregationTasks = createAggregationTasks(rootProject)

        // Baseline task needs to take the aggregate report of all matches
        aggregationTasks.baseline.configure {
            jsonReport.set(aggregationTasks.report.flatMap { task -> task.reportLocation })
        }

        // Aggregate html report takes the aggregate json and prettifies it
        aggregationTasks.htmlReport.configure {
            jsonReport.set(aggregationTasks.report.flatMap { task -> task.reportLocation })
        }

        aggregationTasks.check.configure {
            reportFile.set(aggregationTasks.report.flatMap { task -> task.reportLocation })
            // Run the html report after the check task
            finalizedBy(aggregationTasks.htmlReport)
        }

        rootProject.subprojects.forEach { targetProject ->
            val moduleTasks = createModuleTasks(
                targetProject = targetProject,
                extension = extension,
            )
            connectTasks(
                aggregationTasks = aggregationTasks,
                moduleTasks = moduleTasks
            )
        }
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

    private fun connectTasks(
        aggregationTasks: AggregationTasks,
        moduleTasks: ModuleTasks,
    ) {
        // Aggregate report must take the individual module contribution
        aggregationTasks.report.configure {
            reportFiles.from(moduleTasks.report.flatMap { task -> task.reportFile })
        }

        // Aggregate dependency dump must take the individual module contribution
        aggregationTasks.dependencyDump.configure {
            dependencyFiles.from(moduleTasks.dependencyDump.flatMap { task -> task.dependenciesFile })
        }

        // Report task must take the aggregate dependencies as input
        moduleTasks.report.configure {
            dependencyFile.set(aggregationTasks.dependencyDump.flatMap { task -> task.output })
            baselineFilePath.set(aggregationTasks.baselineFile.path)
        }

        // HTML report task takes the individual module report
        moduleTasks.htmlReport.configure {
            jsonReport.set(moduleTasks.report.flatMap { task -> task.reportFile })
        }

        // Check task must take the report as input
        moduleTasks.check.configure {
            reportFile.set(moduleTasks.report.flatMap { task -> task.reportFile })
            // Run the html report after the check task
            finalizedBy(moduleTasks.htmlReport)
        }
    }

    private fun createAggregationTasks(
        rootProject: Project,
    ): AggregationTasks {
        val baselineFile = rootProject.file(baselineFilePath)
        return AggregationTasks(
            baselineFile = baselineFile,
            dependencyDump = createAggregateDependencyDumpTask(rootProject),
            report = createAggregateReportTask(rootProject),
            htmlReport = createAggregateHtmlReportTask(rootProject),
            baseline = createBaselineTask(baselineFile, rootProject),
            check = createAggregateCheckTask(rootProject)
        )
    }

    private fun createAggregateDependencyDumpTask(
        rootProject: Project,
    ): TaskProvider<TaskAggregateDependencyDump> {
        return rootProject.tasks.register(
            "dependencyGuardAggregateDependencyDump",
            TaskAggregateDependencyDump::class.java
        ) {
            group = "reporting"
            description = "Generates an aggregate JSON report of all dependencies of this project."
            output.set(
                rootProject.layout.buildDirectory.file(dependenciesFilePath)
            )
        }
    }

    private fun createAggregateReportTask(
        rootProject: Project,
    ): TaskProvider<TaskAggregateReport> {
        return rootProject.tasks.register(
            "dependencyGuardAggregateReport",
            TaskAggregateReport::class.java
        ) {
            group = "reporting"
            description = "Generates an aggregate JSON report of all dependency matches."
            reportLocation.set(
                rootProject.layout.buildDirectory.file(jsonAggregateReportFilePath)
            )
        }
    }

    private fun createAggregateHtmlReportTask(
        rootProject: Project,
    ): TaskProvider<TaskAggregateHtmlReport> {
        return rootProject.tasks.register(
            "dependencyGuardAggregateHtmlReport",
            TaskAggregateHtmlReport::class.java
        ) {
            group = "reporting"
            description = "Generates an HTML report of all dependency matches."
            htmlReport.set(
                rootProject.layout.buildDirectory.file(htmlAggregateReportFilePath)
            )
        }
    }

    private fun createAggregateCheckTask(
        rootProject: Project,
    ): TaskProvider<TaskAggregateCheck> {
        return rootProject.tasks.register(
            "dependencyGuardCheck",
            TaskAggregateCheck::class.java
        ) {
            group = "verification"
            description = "Verifies if there are any dependency restrictions being violated"
        }
    }

    private fun createBaselineTask(
        baselineFile: File,
        rootProject: Project,
    ): TaskProvider<TaskBaseline> {
        return rootProject.tasks.register(
            "dependencyGuardBaseline",
            TaskBaseline::class.java
        ) {
            group = "verification"
            description = "Generates a YAML file containing the baseline of suppressions for this project."
            baselineFileReference.set(baselineFile)
        }
    }

    private fun createModuleTasks(
        targetProject: Project,
        extension: DependencyGuardExtension,
    ): ModuleTasks {
        return ModuleTasks(
            check = createCheckTask(targetProject),
            report = createModuleReportTask(targetProject, extension),
            htmlReport = createModuleHtmlReportTask(targetProject),
            dependencyDump = createDependencyDumpTask(targetProject)
        )
    }

    private fun createCheckTask(
        targetProject: Project,
    ): TaskProvider<TaskCheck> {
        return targetProject.tasks.register(
            "dependencyGuardCheck",
            TaskCheck::class.java
        ) {
            group = "verification"
            description = "Verifies if there are any dependency restrictions being violated"
            projectPath.set(project.path)
        }
    }

    private fun createModuleReportTask(
        targetProject: Project,
        extension: DependencyGuardExtension,
    ): TaskProvider<TaskReport> {
        return targetProject.tasks.register(
            "dependencyGuardModuleReport",
            TaskReport::class.java
        ) {
            group = "reporting"
            description = "Generates a JSON report of all dependency restrictions for this module."
            projectPath.set(project.path)
            specProperty.set(extension.getSpec())
            reportFile.set(
                project.layout.buildDirectory.file(jsonReportFilePath)
            )
        }
    }

    private fun createModuleHtmlReportTask(
        targetProject: Project,
    ): TaskProvider<TaskReportHtml> {
        return targetProject.tasks.register(
            "dependencyGuardHtmlReport",
            TaskReportHtml::class.java
        ) {
            group = "reporting"
            description = "Generates an HTML report of all dependency matches."
            htmlReport.set(
                targetProject.layout.buildDirectory.file(htmlReportFilePath)
            )
        }
    }

    private fun createDependencyDumpTask(
        targetProject: Project,
    ): TaskProvider<TaskDependencyDump> {
        return targetProject.tasks.register(
            "dependencyGuardDependencyDump",
            TaskDependencyDump::class.java
        ) {
            group = "reporting"
            description = "Generates a JSON containing the dependencies of this module."
            projectPath.set(project.path)
            dependencies.set(graphBuilder.buildFromProject(project))
            dependenciesFile.set(
                project.layout.buildDirectory.file(dependenciesFilePath)
            )
        }
    }

    private data class ModuleTasks(
        val dependencyDump: TaskProvider<TaskDependencyDump>,
        val report: TaskProvider<TaskReport>,
        val htmlReport: TaskProvider<TaskReportHtml>,
        val check: TaskProvider<TaskCheck>,
    )

    private data class AggregationTasks(
        val baselineFile: File,
        val baseline: TaskProvider<TaskBaseline>,
        val report: TaskProvider<TaskAggregateReport>,
        val check: TaskProvider<TaskAggregateCheck>,
        val htmlReport: TaskProvider<TaskAggregateHtmlReport>,
        val dependencyDump: TaskProvider<TaskAggregateDependencyDump>,
    )

}
