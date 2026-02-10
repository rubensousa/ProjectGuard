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

import com.rubensousa.dependencyguard.plugin.internal.DependencyGuardReport
import com.rubensousa.dependencyguard.plugin.internal.HtmlReportGenerator
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "HTML report should always be regenerated")
abstract class DependencyGuardHtmlReportTask : DefaultTask() {

    @get:InputFile
    abstract val jsonReport: RegularFileProperty

    @get:OutputFile
    abstract val htmlReport: RegularFileProperty

    @TaskAction
    fun generateReport() {
        val htmlGenerator = HtmlReportGenerator()
        if (!jsonReport.get().asFile.exists()) {
            htmlReport.get().asFile.writeText(htmlGenerator.generate(DependencyGuardReport(emptyList())))
            return
        }
        val report = Json.decodeFromString<DependencyGuardReport>(jsonReport.get().asFile.readText())
        val html = htmlGenerator.generate(report)
        htmlReport.get().asFile.writeText(html)
    }

}
