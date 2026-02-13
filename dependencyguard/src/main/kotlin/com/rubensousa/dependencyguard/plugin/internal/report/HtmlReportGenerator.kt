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

package com.rubensousa.dependencyguard.plugin.internal.report

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

internal class HtmlReportGenerator {

    fun generate(report: VerificationReport, outputDirectory: File) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val jsonReport = Json.encodeToString(report)
        val htmlTemplate = readResource("report/index.html")

        // Inject the JSON data into a script tag
        val dataScript = "<script>window.REPORT_DATA = ${jsonReport};</script>"
        val finalHtml = htmlTemplate.replace("</body>", "$dataScript</body>")

        File(outputDirectory, "index.html").writeText(finalHtml)
        copyResource("report/style.css", outputDirectory)
        copyResource("report/script.js", outputDirectory)
    }

    private fun readResource(resourcePath: String): String {
        return this.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Could not find resource: $resourcePath")
    }

    private fun copyResource(resourcePath: String, outputDirectory: File) {
        val inputStream = this.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Could not find resource: $resourcePath")
        val outputFile = File(outputDirectory, resourcePath.substringAfterLast('/'))
        inputStream.use {
            outputFile.outputStream().use {
                inputStream.copyTo(it)
            }
        }
    }

}