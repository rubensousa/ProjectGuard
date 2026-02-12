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

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * This task is just here to that all other tasks can create the reference file if it does no longer exist.
 */
@DisableCachingByDefault
abstract class TaskCreateBaselineFile : DefaultTask() {

    @get:OutputFile
    internal abstract val baselineFileReference: RegularFileProperty

    @TaskAction
    fun dependencyGuardReferenceBaselineFile() {
        baselineFileReference.asFile.get().createNewFile()
    }

}
