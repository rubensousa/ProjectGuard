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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGraph
import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardSpec
import org.junit.rules.TemporaryFolder
import java.io.File

internal class DependencyPluginSimulator(
    private val temporaryFolder: TemporaryFolder,
) {

    fun dumpDependencies(
        moduleId: String,
        action: DependencyGraph.() -> Unit = {},
    ): File {
        val graph = DependencyGraph(configurationId = "compileClasspath")
        graph.action()
        val outputFile = getDependencyFile(moduleId)
        val executor = DependencyDumpExecutor(
            moduleId = moduleId,
            outputFile = outputFile,
            dependencyGraphs = listOf(graph),
        )
        executor.execute()
        return outputFile
    }

    fun dumpAggregateDependencies(): File {
        val outputFile = getAggregateDependenciesFile()
        val dependencyFiles = temporaryFolder.root.listFiles().filter {
            it.name.startsWith("dependencies-")
        }
        if (dependencyFiles.isEmpty()) {
            throw IllegalStateException("Dependency dump is required")
        }
        val executor = AggregateDependencyDumpExecutor(
            inputFiles = dependencyFiles.toSet(),
            outputFile = outputFile,
        )
        executor.execute()
        return outputFile
    }

    fun dumpRestrictions(
        moduleId: String,
        spec: DependencyGuardSpec,
    ): File {
        val outputFile = getRestrictionsFile(moduleId)
        val dependenciesFile = getAggregateDependenciesFile()
        if (!dependenciesFile.exists()) {
            throw IllegalStateException("Aggregate dependency dump is required")
        }
        val executor = RestrictionDumpExecutor(
            moduleId = moduleId,
            outputFile = outputFile,
            dependenciesFile = dependenciesFile,
            spec = spec
        )
        executor.execute()
        return outputFile
    }

    fun dumpAggregateRestrictions(): File {
        val outputFile = getAggregateRestrictionsFile()
        val restrictionFiles = temporaryFolder.root.listFiles().filter {
            it.name.startsWith("restrictions-")
        }
        if (restrictionFiles.isEmpty()) {
            throw IllegalStateException("Restriction dump is required")
        }
        val linkedSet = linkedSetOf<File>()
        linkedSet.addAll(restrictionFiles)
        val executor = AggregateRestrictionDumpExecutor(
            inputFiles = linkedSet,
            outputFile = outputFile,
        )
        executor.execute()
        return outputFile
    }

    fun generateBaseline(): File {
        val outputFile = getBaselineFile()
        val inputFile = getAggregateRestrictionsFile()
        if (!inputFile.exists()) {
            throw IllegalStateException("Aggregate restriction dump is required")
        }
        val executor = BaselineExecutor(
            inputFile = inputFile,
            outputFile = outputFile
        )
        executor.execute()
        return outputFile
    }

    fun check(): Result<Unit> {
        val executor = CheckExecutor(
            baselineFile = getBaselineFile(),
            restrictionDumpFile = getAggregateRestrictionsFile()
        )
        return executor.execute()
    }

    fun check(moduleId: String): Result<Unit> {
        val executor = CheckExecutor(
            baselineFile = getBaselineFile(),
            restrictionDumpFile = getRestrictionsFile(moduleId),
        )
        return executor.execute()
    }

    fun htmlReport(moduleId: String): File {
        val outputDir = File(temporaryFolder.root, "html-report-${getValidFilePath(moduleId)}")
        outputDir.mkdirs()
        val executor = HtmlReportExecutor(
            restrictionDumpFile = getRestrictionsFile(moduleId),
            baselineFile = getBaselineFile(),
            outputFile = outputDir,
        )
        executor.execute()
        return outputDir
    }

    fun getBaselineFile(): File {
        return getFile("baseline.json")
    }

    private fun getAggregateDependenciesFile(): File {
        return getFile("dependencies.json")
    }

    private fun getDependencyFile(moduleId: String): File {
        return getFile("dependencies-$moduleId.json")
    }

    private fun getRestrictionsFile(moduleId: String): File {
        return getFile("restrictions-$moduleId.json")
    }

    private fun getAggregateRestrictionsFile(): File {
        return getFile("restrictions.json")
    }

    private fun getFile(path: String): File {
        return File(temporaryFolder.root, getValidFilePath(path))
    }

    private fun getValidFilePath(path: String): String {
        return path.replace(":", "-")
    }

}
