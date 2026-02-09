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
