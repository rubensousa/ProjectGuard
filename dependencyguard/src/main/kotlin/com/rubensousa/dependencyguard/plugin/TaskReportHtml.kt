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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardReportBuilder
import com.rubensousa.dependencyguard.plugin.internal.HtmlReportGenerator
import com.rubensousa.dependencyguard.plugin.internal.RestrictionMatch
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "HTML report should always be regenerated")
abstract class TaskReportHtml : DefaultTask() {

    @get:InputFile
    abstract val jsonReport: RegularFileProperty

    @get:OutputDirectory
    abstract val htmlReport: DirectoryProperty

    @TaskAction
    fun dependencyGuardHtmlReport() {
        val htmlGenerator = HtmlReportGenerator()
        val matches = Json.decodeFromString<List<RestrictionMatch>>(jsonReport.get().asFile.readText())
        val report = DependencyGuardReportBuilder().build(matches)
        htmlGenerator.generate(report, htmlReport.get().asFile)
    }

}
