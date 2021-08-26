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
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType

class StateObjectProcessor : Processor {

    override fun visitClass(processorContext: ProcessorContext, clazz: KmClass) {

        val containerHostType = clazz.supertypes.firstOrNull { it.isContainerHost }

        if (containerHostType != null) {
            val frameworkName: String = processorContext.framework.baseName
            val classSimpleName = clazz.name.substringAfterLast('/')

            val containerHostTypes = containerHostType.arguments.mapNotNull { (it.type?.classifier as? KmClassifier.Class)?.name }

            val (hasState, hasSideEffect) = containerHostTypes.map { it != "kotlin/Nothing" && it != "kotlin/Unit" }
            val (stateName, sideEffectName) = containerHostTypes.map { it.substringAfterLast('/') }

            val functions = clazz.functions
                .filter { Flag.IS_PUBLIC(it.flags) }
                .filter { it.name != "onCleared" }
                .map { function ->
                    mapOf(
                        "name" to function.name,
                        "parameters" to function.valueParameters.map {
                            val type = (it.type?.classifier as? KmClassifier.Class)?.name?.substringAfterLast('/')!!

                            mapOf(
                                "name" to it.name,
                                "type" to type
                            )
                        }
                    )
                }

            val data = mutableMapOf(
                "frameworkName" to frameworkName,
                "className" to classSimpleName,
                "hasState" to hasState,
                "hasSideEffect" to hasSideEffect,
                "stateType" to stateName,
                "sideEffectType" to sideEffectName,
                "functions" to functions
            )
            val outputData = template.execute(data)

            File(processorContext.outputDir, "${classSimpleName}StateObject.swift").apply {
                createNewFile()
                writeText(outputData)
            }
        }
    }

    private val KmType.isContainerHost: Boolean
        get() = (classifier as? KmClassifier.Class)?.name == "org/orbitmvi/orbit/ContainerHost"

    companion object {
        private val template: Template = Mustache.compiler()
            .compile(StateObjectProcessor::class.java.classLoader.getResource("stateObject.swift.mustache")!!.readText())
    }
}
