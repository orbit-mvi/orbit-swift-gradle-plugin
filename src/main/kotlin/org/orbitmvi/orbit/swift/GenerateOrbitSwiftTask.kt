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

package org.orbitmvi.orbit.swift

import kotlinx.metadata.klib.KlibModuleMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.orbitmvi.orbit.swift.feature.Processor
import org.orbitmvi.orbit.swift.feature.ProcessorContext
import org.orbitmvi.orbit.swift.feature.PublisherProcessor
import org.orbitmvi.orbit.swift.feature.StateObjectProcessor
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

internal open class GenerateOrbitSwiftTask @Inject constructor(
    private val processorContext: ProcessorContext
) : DefaultTask() {

    @get:InputFiles
    internal val inputFilesProvider: Provider<Iterable<File>>
        get() = project.provider {
            processorContext.framework.linkTask.exportLibraries + processorContext.framework.linkTask.sources
        }

    @get:OutputDirectory
    internal val outputDirectoryProvider: Provider<File>
        get() = project.provider { processorContext.outputDir }

    private val processors: List<Processor> = listOf(
        StateObjectProcessor(),
        PublisherProcessor()
    )

    @TaskAction
    fun execute() {
        outputDirectoryProvider.get().apply {
            deleteRecursively()
            Files.createDirectories(toPath())
        }

        processors.forEach { processor ->
            processor.visit(processorContext)
        }

        inputFilesProvider.get().forEach { library ->
            processFeatureContext(library, processorContext)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun processFeatureContext(library: File, processorContext: ProcessorContext) {
        val metadata = KotlinMetadataLibraryProvider.readLibraryMetadata(logger, library) ?: return

        processors.forEach { processor ->
            processor.visitLibrary(processorContext, metadata)

            metadata.fragments.forEach {
                it.pkg?.let { pkg ->
                    processor.visitPackage(processorContext, pkg)
                    pkg.functions.forEach { func -> processor.visitPackageFunction(processorContext, func) }
                }

                it.classes.forEach { clazz ->
                    processor.visitClass(processorContext, clazz)
                }
            }
        }
    }

    class KotlinMetadataLibraryProvider(
        private val library: KotlinLibrary
    ) : KlibModuleMetadata.MetadataLibraryProvider {
        override val moduleHeaderData: ByteArray
            get() = library.moduleHeaderData

        override fun packageMetadata(fqName: String, partName: String): ByteArray =
            library.packageMetadata(fqName, partName)

        override fun packageMetadataParts(fqName: String): Set<String> =
            library.packageMetadataParts(fqName)

        companion object {
            fun readLibraryMetadata(logger: Logger, libraryPath: File): KlibModuleMetadata? {
                @Suppress("TooGenericExceptionCaught")
                try {
                    check(libraryPath.exists()) { "Library does not exist: $libraryPath" }

                    val libraryKonanFile = org.jetbrains.kotlin.konan.file.File(libraryPath.absolutePath)
                    val library = resolveSingleFileKlib(libraryKonanFile)

                    return KlibModuleMetadata.read(KotlinMetadataLibraryProvider(library))
                } catch (exc: IllegalStateException) {
                    logger.error("library can't be read", exc)
                } catch (exc: Exception) {
                    logger.error("can't parse metadata", exc)
                }
                return null
            }
        }
    }
}
