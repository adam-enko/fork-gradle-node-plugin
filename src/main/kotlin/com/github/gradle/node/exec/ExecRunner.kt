package com.github.gradle.node.exec

import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.File

/**
 * Helper function that will calculate the environment variables that should be used.
 *
 * This is operating system aware and will check
 * `Path` and `PATH` in [ExecConfiguration.additionalBinPaths] on Windows.
 *
 * @param execConfiguration configuration to get environment variables from
 */
internal fun computeEnvironment(execConfiguration: ExecConfiguration): Map<String, String> {
    val execEnvironment = mutableMapOf<String, String>()
    execEnvironment += System.getenv()
    execEnvironment += execConfiguration.environment
    if (execConfiguration.additionalBinPaths.isNotEmpty()) {
        // Take care of Windows environments that may contain "Path" OR "PATH" - both existing
        // possibly (but not in parallel as of now)
        val pathEnvironmentVariableName = if (execEnvironment["Path"] != null) "Path" else "PATH"
        val actualPath = execEnvironment[pathEnvironmentVariableName]
        val additionalPathsSerialized = execConfiguration.additionalBinPaths.joinToString(File.pathSeparator)
        execEnvironment[pathEnvironmentVariableName] =
            "${additionalPathsSerialized}${File.pathSeparator}${actualPath}"
    }
    return execEnvironment
}

internal fun computeWorkingDir(nodeProjectDir: File, execConfiguration: ExecConfiguration): File {
    val workingDir = execConfiguration.workingDir ?: nodeProjectDir
    workingDir.mkdirs()
    return workingDir
}

/**
 * Basic execution runner that runs a given ExecConfiguration.
 *
 * Specific implementations likely use the same configuration but may assign
 * different meaning to its values.
 */
class ExecRunner {
    fun execute(
        execOps: ExecOperations,
        nodeProjectDir: File,
        execConfiguration: ExecConfiguration
    ): ExecResult {
        return execOps.exec {
            executable = execConfiguration.executable
            args = execConfiguration.args
            environment = computeEnvironment(execConfiguration)
            isIgnoreExitValue = execConfiguration.ignoreExitValue
            workingDir = computeWorkingDir(nodeProjectDir, execConfiguration)
            execConfiguration.execOverrides?.execute(this)
        }
    }
}
