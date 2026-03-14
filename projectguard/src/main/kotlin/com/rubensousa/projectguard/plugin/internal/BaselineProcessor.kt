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

package com.rubensousa.projectguard.plugin.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

internal class BaselineProcessor {

    private val objectMapper = ObjectMapper(YAMLFactory())
        .apply {
            registerModule(
                KotlinModule.Builder()
                    .enable(KotlinFeature.NullToEmptyMap)
                    .build()
            )
        }

    fun parse(file: File): BaselineConfiguration {
        return objectMapper.readValue(file, BaselineConfiguration::class.java)
    }

    fun write(file: File, content: BaselineConfiguration) {
        return objectMapper.writeValue(file, content)
    }

}
