@file:Suppress("UnstableApiUsage")

package com.github.gradle.node.npm.exec

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.*
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

abstract class NpmExecSource
@Inject
internal constructor(
    private val execOps: ExecOperations
) : ValueSource<NpmExecResult, NpmExecSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val executable: Property<String>
        val args: ListProperty<String>
        val additionalBinPaths: ListProperty<String>
        val environment: MapProperty<String, String?>
        val includeSystemEnv: Property<Boolean>
        val workingDir: DirectoryProperty
//        val ignoreExitValue: Boolean = false,
    }

    override fun obtain(): NpmExecResult {
        val stdOutFile = Files.createTempFile("npm-stdout", ".log")
        val errOutFile = Files.createTempFile("npm-err", ".log")

        val result =
            Files.newOutputStream(stdOutFile).use { stdoutOS ->
                Files.newOutputStream(errOutFile).use { errOS ->
                    execOps.exec {
                        executable = parameters.executable.get()
                        args = parameters.args.orNull.orEmpty()
                        environment = computeEnv()
                        isIgnoreExitValue = true
                        workingDir = parameters.workingDir.orNull?.asFile
                        standardOutput = stdoutOS
                        errorOutput = errOS
                    }
                }
            }

        return NpmExecResult(
            exitValue = result.exitValue,
            stdout = stdOutFile,
            stderr = errOutFile,
            failure = try {
                result.rethrowFailure()
                null
            } catch (e: ExecException) {
                e
            },
        )
    }

    private fun computeEnv(): Map<String, String?> {

        val includeSystemEnv = parameters.includeSystemEnv.getOrElse(true)
        val customEnv = parameters.environment.get().orEmpty()

        val additionalBinPaths = parameters.additionalBinPaths.orNull.orEmpty()

        return mutableMapOf<String, String?>().apply {

            if (includeSystemEnv) {
                putAll(System.getenv())
            }

            putAll(customEnv)

            if (additionalBinPaths.isNotEmpty()) {
                // Take care of Windows environments that may contain "Path" OR "PATH" - both existing
                // possibly (but not in parallel as of now)
                val pathEnvVarName = if ("Path" in keys) "Path" else "PATH"
                val actualPath = get(pathEnvVarName)
                val updatedPath = additionalBinPaths + actualPath.orEmpty()
                put(pathEnvVarName, updatedPath.joinToString(File.pathSeparator))
            }
        }
    }

    companion object {

    }
}


class NpmExecResult(
    val exitValue: Int,
    val stdout: Path,
    val stderr: Path,
    val failure: ExecException?,
)
