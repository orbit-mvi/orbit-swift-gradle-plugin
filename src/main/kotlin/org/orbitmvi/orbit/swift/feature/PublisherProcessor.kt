/*
 * Copyright 2021 Mikołaj Leszczyński & Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbitmvi.orbit.swift.feature

import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import java.io.File

class PublisherProcessor : Processor {

    override fun visit(processorContext: ProcessorContext) {
        val outputData = template.execute(
            mapOf(
                "frameworkName" to processorContext.framework.baseName
            )
        )

        File(processorContext.outputDir, "Publisher.swift").apply {
            createNewFile()
            writeText(outputData)
        }
    }

    companion object {
        private val template: Template = Mustache.compiler()
            .compile(StateObjectProcessor::class.java.classLoader.getResource("Publisher.swift.mustache")!!.readText())
    }
}
