package com.github.gradle.node.variant

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.util.Platform
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File

/**
 * Get the expected node binary name, `node.exe` on Windows and `node` everywhere else.
 */
internal fun computeNodeExec(
    downloadEnabled: Boolean,
    resolvedPlatform: Platform,
    nodeBinDir: File,
): String {

    val baseDir = if (downloadEnabled) {
        nodeBinDir.absolutePath + File.pathSeparator
    } else {
        ""
    }

    val nodeCommand = if (resolvedPlatform.isWindows()) {
        "node.exe"
    } else {
        "node"
    }

    return baseDir + nodeCommand
}

internal fun computeNpmScriptFile(
    nodeDirProvider: Provider<Directory>,
    command: String,
    isWindows: Boolean
): Provider<String> {
    return nodeDirProvider.map { nodeDir ->
        if (isWindows) nodeDir.dir("node_modules/npm/bin/$command-cli.js").asFile.path
        else nodeDir.dir("lib/node_modules/npm/bin/$command-cli.js").asFile.path
    }
}

internal fun computeNodeDir(
    platform: Platform,
    workDir: File,
    version: String,
): File {
//    val osName = nodeExtension.resolvedPlatform.get().name
//    val osArch = nodeExtension.resolvedPlatform.get().arch
    return computeNodeDir(
        workDir = workDir,
        version = version,
        osName = platform.name,
        osArch = platform.arch,
    )
}

internal fun computeNodeDir(
//    nodeExtension: NodeExtension,
    workDir: File,
    version: String,
    osName: String,
    osArch: String,
): File {
    val dirName = "node-v$version-$osName-$osArch"
    return workDir.resolve(dirName)
}

/**
 * Compute the path for a given command, from a given binary directory, taking Windows into account
 */
internal fun computeExec(
    nodeExtension: NodeExtension,
    downloadEnabled: Boolean,
    binDir: Provider<Directory>,
    configurationCommand: String,
    unixCommand: String,
    windowsCommand: String,
): Provider<String> {
    val command = nodeExtension.resolvedPlatform.map { platform ->
        if (platform.isWindows()) {
            if (configurationCommand == unixCommand) {
                windowsCommand
            } else {
                configurationCommand
            }
        } else {
            configurationCommand
        }
    }
//    val command = if (nodeExtension.resolvedPlatform.get().isWindows()) {
////        if (configurationCommand == unixCommand) configurationCommand else windowsCommand
//        configurationCommand.mapIf({ it == unixCommand }) { windowsCommand }
//    } else {
//        configurationCommand
//    }
    return if (downloadEnabled) {
        binDir.zip(command) { dir, cmd ->
            dir.file(cmd).asFile.invariantSeparatorsPath
        }
//        binDir.resolve(command).absolutePath
    } else {
        command
    }
}

/**
 * Compute the path for a given package, taken versions and user-configured working directories into account
 */
internal fun computePackageDir(
    packageName: String,
    packageVersion: String,
    packageWorkDir: File,
): File {
    val dirnameSuffix = if (packageVersion.isNotBlank()) {
        "-v${packageVersion}"
    } else {
        "-latest"
    }
    val dirname = "$packageName$dirnameSuffix"
    return packageWorkDir.resolve(dirname)
}

/**
 * Get the node archive name in Gradle dependency format, using zip for Windows and tar.gz everywhere else.
 *
 * Essentially: `org.nodejs:node:$version:$osName-$osArch@tar.gz`
 */
internal fun computeNodeArchiveDependency(
    platform: Platform,
    version: String,
): String {
    val osName = platform.name
    val osArch = platform.arch
    val type = if (platform.isWindows()) "zip" else "tar.gz"
    return "org.nodejs:node:$version:$osName-$osArch@$type"
}

internal fun NodeExtension.computeNodeArchiveDependency(): Provider<String> {
    return resolvedPlatform.zip(version) { platform, version ->
        val osName = platform.name
        val osArch = platform.arch
        val type = if (platform.isWindows()) "zip" else "tar.gz"
        "org.nodejs:node:$version:$osName-$osArch@$type"
    }
}
