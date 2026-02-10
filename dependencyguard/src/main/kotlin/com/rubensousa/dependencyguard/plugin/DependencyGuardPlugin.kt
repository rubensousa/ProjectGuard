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

class DependencyGuardPlugin : Plugin<Project> {

    private val baselineFilePath = "dependencyguard.yml"
    private val jsonReportFilePath = "reports/dependencyGuard/report.json"
    private val htmlReportFilePath = "reports/dependencyGuard/report.html"
    private val dependenciesFilePath = "reports/dependencyGuard/dependencies.json"

    override fun apply(target: Project) {
        val rootProject = target.rootProject
        val extension = rootProject.extensions.findByType(DependencyGuardExtension::class.java)
            ?: rootProject.extensions.create(
                "dependencyGuard",
                DependencyGuardExtension::class.java
            )
        val graphBuilder = DependencyGraphBuilder()

        // Apply the reporting plugin logic only once on the root project
        if (target == rootProject) {
            applyReportPlugin(
                rootProject = rootProject,
                extension = extension,
                graphBuilder = graphBuilder
            )
        }
    }

    private fun applyReportPlugin(
        rootProject: Project,
        extension: DependencyGuardExtension,
        graphBuilder: DependencyGraphBuilder,
    ) {
        val jsonTask = rootProject.tasks.register(
            "dependencyGuardJsonReport",
            DependencyGuardAggregateReportTask::class.java
        ) {
            group = "reporting"
            description = "Generates an aggregate JSON report of all dependency matches."
            reportLocation.set(
                rootProject.layout.buildDirectory.file(jsonReportFilePath)
            )
        }

        val dependencyAggregateTask = rootProject.tasks.register(
            "dependencyGuardDependencyReport",
            DependencyGuardDependencyReportTask::class.java
        ) {
            group = "reporting"
            description = "Generates an aggregate JSON report of all dependencies of this project."
            output.set(
                rootProject.layout.buildDirectory.file(dependenciesFilePath)
            )
        }

        val htmlTask = rootProject.tasks.register(
            "dependencyGuardHtmlReport",
            DependencyGuardHtmlReportTask::class.java
        ) {
            group = "reporting"
            description = "Generates an HTML report of all dependency matches."
            jsonReport.set(jsonTask.flatMap { it.reportLocation })
            htmlReport.set(
                rootProject.layout.buildDirectory.file(htmlReportFilePath)
            )
        }

        val baselineFile = rootProject.file(baselineFilePath)

        val suppressTask = rootProject.tasks.register(
            "dependencyGuardBaseline",
            DependencyGuardBaselineTask::class.java
        ) {
            group = "verification"
            description = "Generates a YAML file containing the baseline of suppressions for this project."
            suppressionsReference.set(baselineFile)
        }

        suppressTask.configure {
            jsonReport.set(jsonTask.flatMap { it.reportLocation })
        }

        // Create a lifecycle task to run both reports
        rootProject.tasks.register("dependencyGuardReport") {
            group = "reporting"
            description = "Generates JSON and HTML reports of all dependency restrictions."
            dependsOn(jsonTask, htmlTask)
        }


        // For all projects, create a worker report task that feeds into the aggregate JSON task
        rootProject.subprojects.forEach { project ->
            val checkTask = project.tasks.register(
                "dependencyGuardCheck",
                DependencyGuardCheckTask::class.java
            ) {
                group = "verification"
                description = "Verifies if there are any dependency restrictions being violated"
                projectPath.set(project.path)
                specProperty.set(extension.getSpec())
                baselineFilePath.set(baselineFile.path)
            }

            // Check task must take the aggregate dependencies as input
            checkTask.configure {
                dependencyFile.set(dependencyAggregateTask.flatMap { it.output })
            }

            val moduleReportTask = project.tasks.register(
                "dependencyGuardModuleReport",
                DependencyGuardModuleReportTask::class.java
            ) {
                group = "reporting"
                description = "Generates a JSON report of all dependency restrictions for this module."
                projectPath.set(project.path)
                specProperty.set(extension.getSpec())
                reportFile.set(
                    project.layout.buildDirectory.file(jsonReportFilePath)
                )
            }
            moduleReportTask.configure {
                dependencyFile.set(dependencyAggregateTask.flatMap { it.output })
            }
            // Add the output of the per-module task to the aggregate task's input
            jsonTask.configure {
                reportFiles.from(moduleReportTask.flatMap { it.reportFile })
            }

            val dependencyDumpTask = project.tasks.register(
                "dependencyGuardDependencyDump",
                DependencyGuardDependencyDumpTask::class.java
            ) {
                group = "reporting"
                description = "Generates a JSON containing the dependencies of this module."
                projectPath.set(project.path)
                dependencies.set(graphBuilder.buildFrom(project))
                dependenciesFile.set(
                    project.layout.buildDirectory.file(dependenciesFilePath)
                )
            }
            // Link the aggregation task to the individual contributions
            dependencyAggregateTask.configure {
                dependencyFiles.from(dependencyDumpTask.flatMap { it.dependenciesFile })
            }
        }
    }
}
