package com.rubensousa.dependencyguard.plugin

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import com.rubensousa.dependencyguard.plugin.internal.RestrictionChecker
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import com.rubensousa.dependencyguard.plugin.internal.TaskDependencies
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class DependencyGuardCheckTask : DefaultTask() {

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val specProperty: Property<DependencyGuardSpec>

    @get:Input
    internal abstract val dependencies: ListProperty<TaskDependencies>

    @TaskAction
    fun dependencyGuardCheck() {
        val spec = specProperty.get()
        if (spec.isEmpty()) {
            return
        }
        val currentModulePath = projectPath.get()
        val violations = mutableListOf<RestrictionMatch>()
        val restrictionChecker = RestrictionChecker()
        dependencies.get().forEach { config ->
            config.projectPaths.forEach { dependencyPath ->
                violations.addAll(
                    restrictionChecker.findMatches(
                        modulePath = currentModulePath,
                        dependencyPath = dependencyPath,
                        spec = spec,
                    )
                )
            }
            config.externalLibraries.forEach { library ->
                violations.addAll(
                    restrictionChecker.findMatches(
                        modulePath = currentModulePath,
                        dependencyPath = library,
                        spec = spec,
                    )
                )
            }
        }
        val suppressedViolations = violations.filter { it.isSuppressed }
        val fatalViolations = violations.filter { !it.isSuppressed }
        if (suppressedViolations.isNotEmpty()) {
            logger.warn("Found ${suppressedViolations.size} suppressed violation(s)")
        }
        if (fatalViolations.isNotEmpty()) {
            logger.error("Found ${fatalViolations.size} fatal violation(s)")
            throw GradleException(fatalViolations.joinToString("\n\n") { it.asText() })
        }
    }

}
