package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.TaskDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency

class DependencyGuardPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val rootProject = target.rootProject
        val extension = rootProject.extensions.findByType(DependencyGuardExtension::class.java)
            ?: rootProject.extensions.create(
                "dependencyGuard",
                DependencyGuardExtension::class.java
            )

        // Register the check task for the current project
        target.tasks.register("dependencyGuardCheck", DependencyGuardCheckTask::class.java) {
            group = "verification"
            description = "Checks for unauthorized cross-module dependencies."
            projectPath.set(target.path)
            specProperty.set(extension.getSpec())
            dependencies.set(getDependencies(target))
        }

        // Apply the reporting plugin logic only once on the root project
        if (target == rootProject) {
            applyReportPlugin(rootProject, extension)
        }
    }

    private fun applyReportPlugin(
        rootProject: Project,
        extension: DependencyGuardExtension,
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
            val moduleReportTask = project.tasks.register(
                "dependencyGuardModuleReport",
                DependencyGuardModuleReportTask::class.java
            ) {
                group = "reporting"
                description = "Generates a JSON report of all dependency violations."
                projectPath.set(project.path)
                specProperty.set(extension.getSpec())
                dependencies.set(getDependencies(project))
                reportFile.set(
                    project.layout.buildDirectory.file("reports/dependencyGuard/report.json")
                )
            }
            // Add the output of the per-module task to the aggregate task's input
            jsonTask.configure {
                reportFiles.from(moduleReportTask.flatMap { it.reportFile })
            }
        }
    }

    private fun getDependencies(project: Project): List<TaskDependencies> {
        return project.configurations.map { config ->
            TaskDependencies(
                name = config.name,
                projectPaths = config.dependencies
                    .withType(ProjectDependency::class.java)
                    .map { it.path },
                externalLibraries = config.dependencies
                    .withType(ExternalModuleDependency::class.java)
                    .map { "${it.group}:${it.name}" }
            )
        }
    }
}
