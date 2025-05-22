package com.github.gradle.node.variant

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.util.Platform
import com.github.gradle.node.util.mapIf
import com.github.gradle.node.util.zip
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File

object VariantComputer {
    /**
     * Get the expected node binary directory, taking Windows specifics into account.
     */
    fun computeNodeBinDir(nodeDir: File, platform: Platform): Provider<Directory> =
        computeProductBinDir(nodeDir, platform)

    /**
     * Get the expected node binary name, node.exe on Windows and node everywhere else.
     */
    @Deprecated(
        message = "replaced by package-level function",
        replaceWith =
            ReplaceWith("com.github.gradle.node.variant.computeNodeExec(nodeExtension, nodeBinDirProvider)")
    )
    fun computeNodeExec(nodeExtension: NodeExtension, nodeBinDir: File): Provider<String> {
        return com.github.gradle.node.variant.computeNodeExec(nodeExtension, nodeBinDir)
    }

    /**
     * Get the expected directory for a given npm version.
     */
    fun computeNpmDir(nodeExtension: NodeExtension, nodeDir: File): Provider<Directory> {
        return zip(nodeExtension.npmVersion, nodeExtension.npmWorkDir, nodeDirProvider).map {
            val (npmVersion, npmWorkDir, nodeDir) = it
            if (npmVersion.isNotBlank()) {
                val directoryName = "npm-v${npmVersion}"
                npmWorkDir.dir(directoryName)
            } else nodeDir
        }
    }

    /**
     * Get the expected npm binary directory, taking Windows specifics into account.
     */
    fun computeNpmBinDir(npmDir: File, platform: Platform) =
        computeProductBinDir(npmDir, platform)

    /**
     * Get the expected node binary name, npm.cmd on Windows and npm everywhere else.
     *
     * Can be overridden by setting npmCommand.
     */
    fun computeNpmExec(nodeExtension: NodeExtension, npmBinDirProvider: Provider<Directory>): Provider<String> {
        return computeExec(
            nodeExtension,
            npmBinDirProvider,
            nodeExtension.npmCommand,
            "npm",
            "npm.cmd",
        )
    }

    /**
     * Get the expected node binary name, npx.cmd on Windows and npx everywhere else.
     *
     * Can be overridden by setting npxCommand.
     */
    fun computeNpxExec(nodeExtension: NodeExtension, npmBinDirProvider: Provider<Directory>): Provider<String> {
        return computeExec(
            nodeExtension, npmBinDirProvider,
            nodeExtension.npxCommand, "npx", "npx.cmd"
        )
    }

    fun computePnpmDir(nodeExtension: NodeExtension): Provider<Directory> {
        return computePackageDir("pnpm", nodeExtension.pnpmVersion, nodeExtension.pnpmWorkDir)
    }

    fun computePnpmBinDir(pnpmDirProvider: Provider<Directory>, platform: Property<Platform>) =
        computeProductBinDir(pnpmDirProvider, platform)

    fun computePnpmExec(nodeExtension: NodeExtension, pnpmBinDirProvider: Provider<Directory>): Provider<String> {
        return computeExec(
            nodeExtension, pnpmBinDirProvider,
            nodeExtension.pnpmCommand, "pnpm", "pnpm.cmd"
        )
    }

    fun computeYarnDir(nodeExtension: NodeExtension): Provider<Directory> {
        return computePackageDir("yarn", nodeExtension.yarnVersion, nodeExtension.yarnWorkDir)
    }

    fun computeYarnBinDir(yarnDirProvider: Provider<Directory>, platform: Property<Platform>) =
        computeProductBinDir(yarnDirProvider, platform)

    fun computeYarnExec(nodeExtension: NodeExtension, yarnBinDirProvider: Provider<Directory>): Provider<String> {
        return zip(nodeExtension.yarnCommand, yarnBinDirProvider).map { (yarnCommand, yarnBinDir) ->
            val command = if (nodeExtension.resolvedPlatform.get().isWindows()) {
                yarnCommand.mapIf({ it == "yarn" }) { "yarn.cmd" }
            } else {
                yarnCommand
            }
            // This is conceptually pretty simple as we per documentation always download yarn
            yarnBinDir.dir(command).asFile.absolutePath
        }
    }

    fun computeBunDir(
        version: String,
        workDir: File,
    ): File {
        return computePackageDir("bun", version, workDir)
    }

    fun computeBunBinDir(bunDir: File, platform: Platform) =
        computeProductBinDir(bunDir, platform)

    fun computeBunExec(nodeExtension: NodeExtension, bunBinDirProvider: Provider<Directory>): Provider<String> {

        return computeExec(
            nodeExtension,
            bunBinDirProvider,
            nodeExtension.bunCommand,
            "bun",
            "bun.cmd"
        )
    }

    /**
     * Get the expected bunx binary name, bunx.cmd on Windows and bunx everywhere else.
     *
     * Can be overridden by setting bunxCommand.
     */
    fun computeBunxExec(nodeExtension: NodeExtension, bunBinDirProvider: File):  String  {
        return computeExec(
            nodeExtension, bunBinDirProvider,
            nodeExtension.bunxCommand, "bunx", "bunx.cmd"
        )
    }

    private fun computeProductBinDir(productDir: File, platform: Platform): File =
        if (platform.isWindows()) productDir else productDir.resolve("bin")

    /**
     * Get the node archive name in Gradle dependency format, using zip for Windows and tar.gz everywhere else.
     *
     * Essentially: `org.nodejs:node:$version:$osName-$osArch@tar.gz`
     */
    @Deprecated(
        message = "replaced by package-level function",
    )
    fun computeNodeArchiveDependency(nodeExtension: NodeExtension): Provider<String> {
        return nodeExtension.computeNodeArchiveDependency()
//        return com.github.gradle.node.variant.computeNodeArchiveDependency(nodeExtension)
    }
}
