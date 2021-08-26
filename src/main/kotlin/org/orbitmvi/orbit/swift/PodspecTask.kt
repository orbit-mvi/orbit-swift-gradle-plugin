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

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.orbitmvi.orbit.swift

import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.wrapper.Wrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.PodspecPlatformSettings
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin

/**
 * The task generates a podspec file which allows a user to
 * integrate a Kotlin/Native framework into a CocoaPods project.
 */
open class PodspecTask : DefaultTask() {

    @get:Input
    internal var specName: String = project.asValidFrameworkName() + "OrbitSwift"

    @get:OutputFile
    internal val outputFileProvider: Provider<File>
        get() = project.provider { project.file("$specName.podspec") }

    @get:Nested
    val pods = project.objects.listProperty(CocoapodsDependency::class.java)

    @get:Input
    internal lateinit var version: Provider<String>

    @get:Input
    @get:Optional
    internal val homepage = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val license = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val authors = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val summary = project.objects.property(String::class.java)

    @get:Nested
    internal lateinit var ios: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var osx: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var tvos: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var watchos: Provider<PodspecPlatformSettings>

    @TaskAction
    fun generate() {

        val gradleWrapper = (project.rootProject.tasks.getByName("wrapper") as? Wrapper)?.scriptFile
        require(gradleWrapper != null && gradleWrapper.exists()) {
            """
            The Gradle wrapper is required to run the build from Xcode.

            Run the `:wrapper` task to generate the wrapper manually.

            See details about the wrapper at https://docs.gradle.org/current/userguide/gradle_wrapper.html
            """.trimIndent()
        }

        val data = mapOf(
            "specName" to specName,
            "version" to version.get(),
            "homepage" to homepage.getOrEmpty(),
            "authors" to authors.getOrEmpty(),
            "license" to license.getOrEmpty(),
            "summary" to summary.getOrEmpty(),
            "frameworkDir" to project.buildDir.resolve("cocoapods/orbit").relativeTo(outputFileProvider.get().parentFile).path,
            "iosTarget" to KotlinCocoapodsPlugin.KOTLIN_TARGET_FOR_IOS_DEVICE,
            "watchosTarget" to KotlinCocoapodsPlugin.KOTLIN_TARGET_FOR_WATCHOS_DEVICE,
            "deploymentTargets" to listOf(ios, osx, tvos, watchos).map { it.get() }.filter { it.deploymentTarget != null }.map {
                mapOf(
                    "name" to it.name,
                    "deploymentTarget" to it.deploymentTarget
                )
            },
            "dependencies" to pods.get().map { pod ->
                mapOf(
                    "name" to pod.name,
                    "versionSuffix" to if (pod.version != null) ", '${pod.version}'" else ""
                )
            },
            "gradleCommand" to "\$REPO_ROOT/${gradleWrapper.toRelativeString(project.projectDir)}",
            "syncTask" to "${project.path}:syncOrbitSwift",
            "propertyTarget" to KotlinCocoapodsPlugin.TARGET_PROPERTY,
            "propertyConfig" to KotlinCocoapodsPlugin.CONFIGURATION_PROPERTY,
            "propertyCflags" to KotlinCocoapodsPlugin.CFLAGS_PROPERTY,
            "propertyHeaderPaths" to KotlinCocoapodsPlugin.HEADER_PATHS_PROPERTY,
            "propertyFrameworkPaths" to KotlinCocoapodsPlugin.FRAMEWORK_PATHS_PROPERTY,
        )

        with(outputFileProvider.get()) {
            writeText(template.execute(data))

            if (hasPodfileOwnOrParent(project)) {
                logger.quiet(
                    """
                    Generated a podspec file at: $absolutePath.
                    To include it in your Xcode project, check that the following dependency snippet exists in your Podfile:

                    pod '$specName', :path => '${parentFile.absolutePath}'

            """.trimIndent()
                )
            }
        }
    }

    private fun Provider<String>.getOrEmpty() = getOrElse("")

    companion object {
        private val KotlinMultiplatformExtension?.cocoapodsExtensionOrNull: CocoapodsExtension?
            get() = (this as? ExtensionAware)?.extensions?.findByType(CocoapodsExtension::class.java)

        private fun hasPodfileOwnOrParent(project: Project): Boolean =
            if (project.rootProject == project) project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podfile != null
            else project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podfile != null ||
                    (project.parent?.let { hasPodfileOwnOrParent(it) } ?: false)

        private val Project.multiplatformExtensionOrNull: KotlinMultiplatformExtension?
            get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

        private val template: Template = Mustache.compiler()
            .compile(PodspecTask::class.java.classLoader.getResource("podspec.mustache")!!.readText())
    }
}
