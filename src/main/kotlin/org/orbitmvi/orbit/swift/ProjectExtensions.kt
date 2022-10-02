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

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.ARCHS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.PLATFORM_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.TARGET_PROPERTY
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*

internal fun Project.asValidFrameworkName() = name.replace('-', '_')

// Based on https://github.com/JetBrains/kotlin/commit/8c021af64611193075dcd5185c5adbe74360894d
internal fun Project.getNativeTargets(): List<KonanTarget> {
    class UnknownArchitectureException(platform: String, arch: String) :
        IllegalArgumentException("Architecture $arch is not supported for platform $platform")

    val platforms = project.findProperty(PLATFORM_PROPERTY)?.toString()?.split(",", " ")?.filter { it.isNotBlank() }
    val archs = project.findProperty(ARCHS_PROPERTY)?.toString()?.split(",", " ")?.filter { it.isNotBlank() }

    if (platforms == null || archs == null) {
        check(project.findProperty(TARGET_PROPERTY) == null) {
            """
                $TARGET_PROPERTY property was dropped in favor of $PLATFORM_PROPERTY and $ARCHS_PROPERTY. 
                Podspec file might be outdated. Sync project with Gradle files or run the 'podspec' task manually to regenerate it.
                """.trimIndent()
        }
        return emptyList()
    }

    check(platforms.size == 1) {
        "$PLATFORM_PROPERTY has to contain a single value only. If building for multiple platforms is required, consider using XCFrameworks"
    }

    val platform = platforms.first()

    val targets: MutableSet<KonanTarget> = mutableSetOf()

    when (platform) {

        "iphoneos" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> IOS_ARM64
                    "armv7", "armv7s" -> IOS_ARM32
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }
        "iphonesimulator" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> IOS_SIMULATOR_ARM64
                    "x86_64" -> IOS_X64
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }
        "watchos" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "armv7k" -> WATCHOS_ARM32
                    "arm64_32" -> WATCHOS_ARM64
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }
        "watchsimulator" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> WATCHOS_SIMULATOR_ARM64
                    "i386" -> WATCHOS_X86
                    "x86_64" -> WATCHOS_X64
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }
        "appletvos" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> TVOS_ARM64
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }
        "appletvsimulator" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64", "arm64e" -> TVOS_SIMULATOR_ARM64
                    "x86_64" -> TVOS_X64
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }
        "macosx" -> {
            targets.addAll(archs.map { arch ->
                when (arch) {
                    "arm64" -> MACOS_ARM64
                    "x86_64" -> MACOS_X64
                    else -> throw UnknownArchitectureException(platform, arch)
                }
            })
        }

        else -> throw IllegalArgumentException("Platform $platform is not supported")
    }

    check(targets.isNotEmpty()) { "Could not identify native targets for platform: '$platform' and architectures: '$archs'" }

    return targets.toList()
}
