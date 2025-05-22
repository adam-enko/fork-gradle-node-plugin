package com.github.gradle.node.bun.exec

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.exec.ExecConfiguration
import com.github.gradle.node.exec.ExecRunner
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.npm.exec.NpmExecConfiguration
import com.github.gradle.node.npm.proxy.NpmProxy
import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.util.Platform
import com.github.gradle.node.util.zip
import com.github.gradle.node.variant.VariantComputer
import com.github.gradle.node.variant.computeNodeExec
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.File
import javax.inject.Inject

abstract class BunExecRunner @Inject internal constructor(
    private val providers: ProviderFactory,
    private val execOps: ExecOperations,
) {

    fun executeBunCommand(
        nodeProjectDir: File,
        nodeProxySettings: ProxySettings,
        nodeExecConfiguration: NodeExecConfiguration,
    ): ExecResult {
        val bunExecConfiguration = NpmExecConfiguration(
            "bun"
        ) { nodeExtension, binDir -> VariantComputer.computeBunExec(nodeExtension, binDir) }

        val enhancedNodeExecConfiguration =
            NpmProxy.addProxyEnvironmentVariables(nodeProxySettings, nodeExecConfiguration)
        val execConfiguration =
            computeExecConfiguration(extension, bunExecConfiguration, enhancedNodeExecConfiguration).get()
        return ExecRunner().execute(execOps, nodeProjectDir, execConfiguration)
    }

    fun executeBunxCommand(
        extension: NodeExtension,
        nodeProjectDir: File,
        nodeProxySettings: ProxySettings,
        nodeExecConfiguration: NodeExecConfiguration,
    ): ExecResult {
        val bunExecConfiguration = NpmExecConfiguration(
            "bunx"
        ) { nodeExtension, bunBinDir ->
            VariantComputer.computeBunxExec(nodeExtension, bunBinDir)
        }

        val enhancedNodeExecConfiguration =
            NpmProxy.addProxyEnvironmentVariables(nodeProxySettings, nodeExecConfiguration)
        val execConfiguration =
            computeExecConfiguration(extension, bunExecConfiguration, enhancedNodeExecConfiguration).get()
        return ExecRunner().execute(execOps, nodeProjectDir, execConfiguration)
    }

    private fun computeExecConfiguration(
        extension: NodeExtension,
        bunExecConfiguration: NpmExecConfiguration,
        nodeExecConfiguration: NodeExecConfiguration,
    ): Provider<ExecConfiguration> {
        val additionalBinPathProvider = computeAdditionalBinPath(
            extension
        )
        val executableAndScriptProvider = computeExecutable(extension, bunExecConfiguration)
        return zip(additionalBinPathProvider, executableAndScriptProvider)
            .map { (additionalBinPath, executableAndScript) ->
                val argsPrefix =
                    if (executableAndScript.script != null) listOf(executableAndScript.script) else listOf()
                val args = argsPrefix.plus(nodeExecConfiguration.command)
                ExecConfiguration(
                    executableAndScript.executable, args, additionalBinPath,
                    nodeExecConfiguration.environment, nodeExecConfiguration.workingDir,
                    nodeExecConfiguration.ignoreExitValue, nodeExecConfiguration.execOverrides
                )
            }
    }

    private fun computeExecutable(
        nodeExtension: NodeExtension,
        bunExecConfiguration: NpmExecConfiguration,
        platform: Platform
    ): Provider<ExecutableAndScript> {
        val nodeDirProvider = nodeExtension.resolvedNodeDir
        val bunDirProvider = VariantComputer.computeBunDir(nodeExtension)
        val nodeBinDirProvider = VariantComputer.computeNodeBinDir(nodeDirProvider, platform)
        val bunBinDirProvider = VariantComputer.computeBunBinDir(bunDirProvider, platform)
        val nodeExec = computeNodeExec(
            downloadEnabled = nodeExtension.download.get(),
            resolvedPlatform = nodeExtension.resolvedPlatform.get(),
            nodeBinDirProvider.get().asFile
        )
        val executableProvider =
            bunExecConfiguration.commandExecComputer(nodeExtension, bunBinDirProvider)

        return zip(
            nodeExtension.download,
            nodeExtension.nodeProjectDir,
            executableProvider
        ).map { (download, nodeProjectDir, executable) ->
            if (download) {
                val localCommandScript = nodeProjectDir.dir("node_modules/bun/bin")
                    .file("${bunExecConfiguration.command}.js").asFile
                if (localCommandScript.exists()) {
                    return@map ExecutableAndScript(nodeExec, localCommandScript.absolutePath)
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
         downloadEnabled: Boolean,
        resolvedPlatform: Platform,
    ): List<String> {
        return if (!downloadEnabled) {
            emptyList()
        } else {
            val bunDirProvider = VariantComputer.computeBunDir(nodeExtension)
            val bunBinDirProvider = VariantComputer.computeBunBinDir(bunDirProvider, resolvedPlatform)
            listOf(bunBinDirProvider.get().asFile.absolutePath)
        }
    }
}
