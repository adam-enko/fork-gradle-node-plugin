package com.github.gradle.node.task

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.util.Platform
import com.github.gradle.node.variant.VariantComputer
import com.github.gradle.node.variant.computeNodeExec
import com.github.gradle.node.variant.computeNpmScriptFile
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

abstract class NodeSetupTask
@Inject
internal constructor(
//    private val objects: ObjectFactory,
//    private val providers: ProviderFactory,
    private val fs: FileSystemOperations,
    private val archives: ArchiveOperations,
) : BaseTask() {

    @Deprecated("moved to task properties")
    private val nodeExtension: NodeExtension = NodeExtension[project]

    @get:Input
    abstract val download: Property<Boolean>

    @get:InputFile
    abstract val nodeArchiveFile: RegularFileProperty

    @get:OutputDirectory
    abstract val nodeDir: DirectoryProperty

//    @get:Internal
//    val projectHelper: DefaultProjectApiHelper =
//        objects.newInstance(DefaultProjectApiHelper::class.java)

    @get:Input
    abstract val resolvedPlatform: Property<Platform>

    @get:OutputDirectory
    abstract val resolvedNodeDir: DirectoryProperty

    init {
        group = NodePlugin.NODE_GROUP
        description = "Download and install a local node/npm version."
    }

    @TaskAction
    fun exec() {
        deleteExistingNode()
        unpackNodeArchive()
        setExecutableFlag()
    }

    private fun deleteExistingNode() {
        fs.delete {
            delete(nodeDir.get().dir("../").asFileTree.matching {
                include("node-v*/**")
            })
        }
    }

    private fun unpackNodeArchive() {
        val archiveFile = nodeArchiveFile.get().asFile
        val nodeBinDirProvider = VariantComputer.computeNodeBinDir(nodeDir, resolvedPlatform)
        val archivePath = nodeDir.map { it.dir("../") }
        if (archiveFile.name.endsWith("zip")) {
            fs.copy {
                from(archives.zipTree(archiveFile))
                into(archivePath)
            }
        } else {
            fs.copy {
                from(archives.tarTree(archiveFile))
                into(archivePath)
            }
            // Fix broken symlink
            val nodeBinDirPath = nodeBinDirProvider.get().asFile.toPath()
            fixBrokenSymlink("npm", nodeBinDirPath)
            fixBrokenSymlink("npx", nodeBinDirPath)
        }
    }

    private fun fixBrokenSymlink(name: String, nodeBinDirPath: Path) {
        val script = nodeBinDirPath.resolve(name)
        val scriptFile = computeNpmScriptFile(nodeDir, name, resolvedPlatform.get().isWindows())
        if (Files.deleteIfExists(script)) {
            Files.createSymbolicLink(script, nodeBinDirPath.relativize(Paths.get(scriptFile.get())))
        }
    }

    private fun setExecutableFlag() {
        if (!resolvedPlatform.get().isWindows()) {
            val nodeBinDirProvider = VariantComputer.computeNodeBinDir(
                resolvedNodeDir,
                resolvedPlatform
            )
            val nodeExecPath = computeNodeExec(
                downloadEnabled = download.get(),
                resolvedPlatform = resolvedPlatform.get(),
                nodeBinDir = nodeBinDirProvider.get().asFile,
            )
            val nodeExecFile = File(nodeExecPath)
            if (nodeExecFile.exists()) {
                nodeExecFile.setExecutable(true)
            }
        }
    }

    companion object {
        const val NAME = "nodeSetup"
    }
}
