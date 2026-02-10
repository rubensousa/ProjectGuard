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

    private val baselineFilePath = "dependencyguard.yml"
    private val jsonReportFilePath = "reports/dependencyGuard/report.json"
    private val htmlReportFilePath = "reports/dependencyGuard/report.html"
    private val dependenciesFilePath = "reports/dependencyGuard/dependencies.json"
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

        rootProject.subprojects.forEach { targetProject ->
            val moduleTasks = createModuleTasks(
                targetProject = targetProject,
                extension = extension,
                aggregationTasks = aggregationTasks,
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

        // Check task must take the aggregate dependencies as input
        moduleTasks.check.configure {
            dependencyFile.set(aggregationTasks.dependencyDump.flatMap { task -> task.output })
        }

        // Report task must take the aggregate dependencies as input
        moduleTasks.report.configure {
            dependencyFile.set(aggregationTasks.dependencyDump.flatMap { task -> task.output })
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
                rootProject.layout.buildDirectory.file(jsonReportFilePath)
            )
        }
    }

    private fun createAggregateHtmlReportTask(
        rootProject: Project,
    ): TaskProvider<TaskReportHtml> {
        return rootProject.tasks.register(
            "dependencyGuardHtmlReport",
            TaskReportHtml::class.java
        ) {
            group = "reporting"
            description = "Generates an HTML report of all dependency matches."
            htmlReport.set(
                rootProject.layout.buildDirectory.file(htmlReportFilePath)
            )
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
        aggregationTasks: AggregationTasks,
    ): ModuleTasks {
        return ModuleTasks(
            check = createCheckTask(targetProject, extension, aggregationTasks.baselineFile),
            report = createModuleReportTask(targetProject, extension),
            dependencyDump = createDependencyDumpTask(targetProject)
        )
    }

    private fun createCheckTask(
        targetProject: Project,
        extension: DependencyGuardExtension,
        baselineFile: File,
    ): TaskProvider<TaskCheck> {
        return targetProject.tasks.register(
            "dependencyGuardCheck",
            TaskCheck::class.java
        ) {
            group = "verification"
            description = "Verifies if there are any dependency restrictions being violated"
            projectPath.set(project.path)
            specProperty.set(extension.getSpec())
            baselineFilePath.set(baselineFile.path)
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
        val check: TaskProvider<TaskCheck>,
    )

    private data class AggregationTasks(
        val baselineFile: File,
        val baseline: TaskProvider<TaskBaseline>,
        val report: TaskProvider<TaskAggregateReport>,
        val htmlReport: TaskProvider<TaskReportHtml>,
        val dependencyDump: TaskProvider<TaskAggregateDependencyDump>,
    )

}
