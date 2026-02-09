package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraphBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project

class DependencyGuardPlugin : Plugin<Project> {

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
            description = "Generates an aggregate JSON report of all dependency violations."
            reportLocation.set(
                rootProject.layout.buildDirectory.file("reports/dependencyGuard/report.json")
            )
        }

        val dependencyAggregateTask = rootProject.tasks.register(
            "dependencyGuardDependencyReport",
            DependencyGuardDependencyReportTask::class.java
        ) {
            group = "reporting"
            description = "Generates an aggregate JSON report of all dependencies of this project."
            output.set(
                rootProject.layout.buildDirectory.file("reports/dependencyGuard/dependencies.json")
            )
        }

        val htmlTask = rootProject.tasks.register(
            "dependencyGuardHtmlReport",
            DependencyGuardHtmlReportTask::class.java
        ) {
            group = "reporting"
            description = "Generates an HTML report of all dependency violations."
            jsonReport.set(jsonTask.flatMap { it.reportLocation })
            htmlReport.set(
                rootProject.layout.buildDirectory.file("reports/dependencyGuard/report.html")
            )
        }

        // Create a lifecycle task to run both reports
        rootProject.tasks.register("dependencyGuardReport") {
            group = "reporting"
            description = "Generates JSON and HTML reports of all dependency violations."
            dependsOn(jsonTask, htmlTask)
        }


        // For all projects, create a worker report task that feeds into the aggregate JSON task
        rootProject.subprojects.forEach { project ->
            val checkTask = project.tasks.register(
                "dependencyGuardCheck",
                DependencyGuardCheckTask::class.java
            ) {
                group = "verification"
                description = "Checks for unauthorized cross-module dependencies."
                projectPath.set(project.path)
                specProperty.set(extension.getSpec())
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
                description = "Generates a JSON report of all dependency violations."
                projectPath.set(project.path)
                specProperty.set(extension.getSpec())
                reportFile.set(
                    project.layout.buildDirectory.file("reports/dependencyGuard/report.json")
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
                    project.layout.buildDirectory.file("reports/dependencyGuard/dependencies.json")
                )
            }
            // Link the aggregation task to the individual contributions
            dependencyAggregateTask.configure {
                dependencyFiles.from(dependencyDumpTask.flatMap { it.dependenciesFile })
            }
        }
    }
}
