package com.github.gradle.node.npm.exec

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.exec.ExecConfiguration
import com.github.gradle.node.exec.ExecRunner
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.npm.proxy.NpmProxy
import com.github.gradle.node.util.ProjectApiHelper
import com.github.gradle.node.util.zip
import com.github.gradle.node.variant.VariantComputer
import com.github.gradle.node.variant.computeNodeExec
import com.github.gradle.node.variant.computeNpmScriptFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ExecResult
import java.io.File
import javax.inject.Inject

abstract class NpmExecRunner {
    @get:Inject
    abstract val providers: ProviderFactory

    fun executeNpmCommand(
        project: ProjectApiHelper,
        extension: NodeExtension,
        nodeExecConfiguration: NodeExecConfiguration,
    ): ExecResult {
        val npmExecConfiguration = NpmExecConfiguration(
            "npm"
        ) { nodeExtension, npmBinDir ->
            VariantComputer.computeNpmExec(nodeExtension, npmBinDir)
        }
        return executeCommand(
            project,
            extension,
            NpmProxy.addProxyEnvironmentVariables(extension.nodeProxySettings.get(), nodeExecConfiguration),
            npmExecConfiguration,
        )
    }

    fun executeNpxCommand(
        project: ProjectApiHelper,
        extension: NodeExtension,
        nodeExecConfiguration: NodeExecConfiguration,
    ): ExecResult {
        val npxExecConfiguration = NpmExecConfiguration("npx") { nodeExtension, npmBinDir ->
            VariantComputer.computeNpxExec(nodeExtension, npmBinDir)
        }

        return executeCommand(project, extension, nodeExecConfiguration, npxExecConfiguration)
    }

    private fun executeCommand(
        project: ProjectApiHelper,
        extension: NodeExtension,
        nodeExecConfiguration: NodeExecConfiguration,
        npmExecConfiguration: NpmExecConfiguration,
    ): ExecResult {
        val execConfiguration =
            computeExecConfiguration(extension, npmExecConfiguration, nodeExecConfiguration).get()
        val execRunner = ExecRunner()
        return execRunner.execute(project, extension, execConfiguration)
    }

    private fun computeExecConfiguration(
        extension: NodeExtension, npmExecConfiguration: NpmExecConfiguration,
        nodeExecConfiguration: NodeExecConfiguration,
    ): Provider<ExecConfiguration> {
        val additionalBinPathProvider = computeAdditionalBinPath(extension)
        val executableAndScriptProvider = computeExecutable(extension, npmExecConfiguration)
        return zip(additionalBinPathProvider, executableAndScriptProvider)
            .map { (additionalBinPath, executableAndScript) ->
                val argsPrefix =
                    if (executableAndScript.script != null) listOf(executableAndScript.script) else listOf()
                val args = argsPrefix.plus(nodeExecConfiguration.command)
                ExecConfiguration(
                    executable = executableAndScript.executable,
                    args = args,
                    additionalBinPaths = additionalBinPath,
                    environment = nodeExecConfiguration.environment,
                    workingDir = nodeExecConfiguration.workingDir,
                    ignoreExitValue = nodeExecConfiguration.ignoreExitValue,
                    execOverrides = nodeExecConfiguration.execOverrides
                )
            }
    }

    private fun computeExecutable(
        nodeExtension: NodeExtension,
        npmExecConfiguration: NpmExecConfiguration,
    ): Provider<ExecutableAndScript> {
        val nodeDirProvider = nodeExtension.resolvedNodeDir
        val npmDirProvider = VariantComputer.computeNpmDir(nodeExtension, nodeDirProvider)
        val nodeBinDirProvider = VariantComputer.computeNodeBinDir(nodeDirProvider, nodeExtension.resolvedPlatform)
        val npmBinDirProvider = VariantComputer.computeNpmBinDir(npmDirProvider, nodeExtension.resolvedPlatform)
        val nodeExecProvider = computeNodeExec(nodeExtension, nodeBinDirProvider)
        val executableProvider =
            npmExecConfiguration.commandExecComputer(nodeExtension, npmBinDirProvider)
        val isWindows = nodeExtension.resolvedPlatform.get().isWindows()
        val npmScriptFileProvider =
            computeNpmScriptFile(nodeDirProvider, npmExecConfiguration.command, isWindows)
        return zip(
            nodeExtension.download,
            nodeExtension.nodeProjectDir,
            executableProvider,
            nodeExecProvider,
            npmScriptFileProvider,
        ).map {
            val (download, nodeProjectDir, executable, nodeExec,
                npmScriptFile) = it
            if (download) {
                val localCommandScript = nodeProjectDir.dir("node_modules/npm/bin")
                    .file("${npmExecConfiguration.command}-cli.js").asFile
                if (localCommandScript.exists()) {
                    return@map ExecutableAndScript(nodeExec, localCommandScript.absolutePath)
                } else if (!File(executable).exists()) {
                    return@map ExecutableAndScript(nodeExec, npmScriptFile)
                }
            }
            return@map ExecutableAndScript(executable)
        }
    }

    private data class ExecutableAndScript(
        val executable: String,
        val script: String? = null
    )

    private fun computeAdditionalBinPath(
        nodeExtension: NodeExtension,
    ): Provider<List<String>> {
        return nodeExtension.download.flatMap { download ->
            if (!download) {
                providers.provider { listOf<String>() }
            }
            val nodeDirProvider = nodeExtension.resolvedNodeDir
            val nodeBinDirProvider = VariantComputer.computeNodeBinDir(nodeDirProvider, nodeExtension.resolvedPlatform)
            val npmDirProvider = VariantComputer.computeNpmDir(nodeExtension, nodeDirProvider)
            val npmBinDirProvider = VariantComputer.computeNpmBinDir(npmDirProvider, nodeExtension.resolvedPlatform)
            zip(npmBinDirProvider, nodeBinDirProvider).map { (npmBinDir, nodeBinDir) ->
                listOf(npmBinDir, nodeBinDir).map { file -> file.asFile.absolutePath }
            }
        }
    }
}
