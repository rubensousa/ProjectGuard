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

package com.rubensousa.dependencyguard.plugin.internal

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

internal class JsonFileWriter {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    // See: https://github.com/gradle/gradle/issues/25412
    fun writeToFile(content: DependencyGuardReport, file: File) {
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(content))
    }

    fun writeToFile(content: DependencyGraphAggregateReport, file: File) {
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(content))
    }

    fun writeToFile(content: DependencyGraphReport, file: File) {
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(content))
    }

}
