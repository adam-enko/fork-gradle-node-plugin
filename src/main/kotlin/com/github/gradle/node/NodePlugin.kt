package com.github.gradle.node

import com.github.gradle.node.NodeExtension.Companion.DEFAULT_NODE_VERSION
import com.github.gradle.node.bun.task.*
import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmSetupTask
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import com.github.gradle.node.pnpm.task.PnpmInstallTask
import com.github.gradle.node.pnpm.task.PnpmSetupTask
import com.github.gradle.node.pnpm.task.PnpmTask
import com.github.gradle.node.task.NodeSetupTask
import com.github.gradle.node.task.NodeTask
import com.github.gradle.node.util.OsType
import com.github.gradle.node.util.parseOsType
import com.github.gradle.node.util.parsePlatform
import com.github.gradle.node.util.zip
import com.github.gradle.node.variant.computeNodeArchiveDependency
import com.github.gradle.node.variant.computeNodeDir
import com.github.gradle.node.yarn.task.YarnInstallTask
import com.github.gradle.node.yarn.task.YarnSetupTask
import com.github.gradle.node.yarn.task.YarnTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.gradle.util.GradleVersion
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class NodePlugin @Inject internal constructor(
    private val layout: ProjectLayout,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val execOps: ExecOperations,
) : Plugin<Project> {
//    private lateinit var project: Project

    override fun apply(project: Project) {
//        if (GradleVersion.current() < MINIMAL_SUPPORTED_GRADLE_VERSION) {
//            project.logger.error("This version of the plugin requires $MINIMAL_SUPPORTED_GRADLE_VERSION or newer.")
//        }
//        this.project = project
        val nodeExtension = createExtension(project)
        configureNodeExtension(nodeExtension)
        project.extensions.create<PackageJsonExtension>(PackageJsonExtension.NAME)
        addGlobalTypes(project)
        addTasks(project)
        addNpmRule(project, nodeExtension.enableTaskRules)
        addPnpmRule(project, nodeExtension.enableTaskRules)
        addYarnRule(project, nodeExtension.enableTaskRules)
        project.afterEvaluate {
            if (nodeExtension.download.get()) {
                nodeExtension.distBaseUrl.orNull?.let {
                    addRepository(
                        project,
                        it,
                        nodeExtension.allowInsecureProtocol.orNull
                    )
                }
                configureNodeSetupTask(project, nodeExtension)
            }
        }

        configureBunTasks(project)
    }

    private fun createExtension(project: Project): NodeExtension {
        return project.extensions.create<NodeExtension>(NodeExtension.NAME).apply {
            val cacheDir = layout.projectDirectory.dir(".gradle")
            workDir.convention(cacheDir.dir("nodejs"))
            npmWorkDir.convention(cacheDir.dir("npm"))
            pnpmWorkDir.convention(cacheDir.dir("pnpm"))
            yarnWorkDir.convention(cacheDir.dir("yarn"))
            bunWorkDir.convention(cacheDir.dir("bun"))
            nodeProjectDir.convention(project.layout.projectDirectory)
            version.convention(DEFAULT_NODE_VERSION)
            npmVersion.convention("")
            pnpmVersion.convention("")
            yarnVersion.convention("")
            bunVersion.convention("")
            distBaseUrl.convention("https://nodejs.org/dist")
//      allowInsecureProtocol.convention()
            npmCommand.convention("npm")
            npxCommand.convention("npx")
            pnpmCommand.convention("pnpm")
            yarnCommand.convention("yarn")
            bunCommand.convention("bun")
            bunxCommand.convention("bunx")
            npmInstallCommand.convention("install")
            download.convention(false)
            nodeProxySettings.convention(ProxySettings.SMART)
            fastNpmInstall.convention(false)
            oldNpm.convention(false)
            enableTaskRules.convention(true)
//      computedNodeDir
//      resolvedNodeDir
//      computedPlatform
//      resolvedPlatform

        }
    }


    private fun configureNodeExtension(extension: NodeExtension) {
        addPlatform(extension)
        extension.resolvedNodeDir
            .convention(
                layout.dir(
                    zip(
                        extension.resolvedPlatform,
                        extension.workDir,
                        extension.version,
                    ).map { (platform, workDir, version) ->
                        computeNodeDir(platform, workDir.asFile, version)
                    }
                )
            )
            .finalizeValueOnRead()

        @Suppress("DEPRECATION")
        extension.computedNodeDir
            .convention(extension.resolvedNodeDir)
            .finalizeValueOnRead()
    }

    private fun addPlatform(extension: NodeExtension) {
        val osType = parseOsType(System.getProperty("os.name"))
        val arch = System.getProperty("os.arch")

        val unameSpec: Action<ExecSpec> = Action {
            if (osType == OsType.WINDOWS) {
                this.executable = "powershell"
                this.args = listOf(
                    "-NoProfile", // Command runs in ~175ms, -NoProfile saves ~300ms
                    "-Command",
                    "(Get-WmiObject Win32_Processor).Architecture",
                )
            } else {
                this.executable = "uname"
                this.args = listOf("-m")
            }
        }

        val uname: String by lazy {
            if (GradleVersion.current() >= GradleVersion.version("7.5")) {
                val cmd = providers.exec(unameSpec)
                cmd.standardOutput.asText.get().trim()
            } else {
                val out = ByteArrayOutputStream()
                //execOps.exec(unameSpec)
                val cmd = execOps.exec {
                    unameSpec.execute(this)
                    this.standardOutput = out
                }

                cmd.assertNormalExitValue()
                out.toString().trim()
            }
        }
        val platform = parsePlatform(osType, arch, { uname })
        extension.resolvedPlatform.set(platform)
        extension.computedPlatform.convention(extension.resolvedPlatform)
    }

    private fun addGlobalTypes(
        project: Project
    ) {
        fun <T : Any> addGlobalType(cls: KClass<in T>) {
            project.extensions.extraProperties[cls.java.simpleName] = cls.java
        }

        addGlobalType(NodeTask::class)
        addGlobalType(NpmTask::class)
        addGlobalType(NpxTask::class)
        addGlobalType(PnpmTask::class)
        addGlobalType(YarnTask::class)
        addGlobalType(BunTask::class)
        addGlobalType(BunxTask::class)
        addGlobalType(ProxySettings::class)
    }


    private fun addTasks(project: Project) {
        project.tasks.register<NpmInstallTask>(NpmInstallTask.NAME)
        project.tasks.register<PnpmInstallTask>(PnpmInstallTask.NAME)
        project.tasks.register<YarnInstallTask>(YarnInstallTask.NAME)
        project.tasks.register<BunInstallTask>(BunInstallTask.NAME)
        project.tasks.register<NodeSetupTask>(NodeSetupTask.NAME)
        project.tasks.register<NpmSetupTask>(NpmSetupTask.NAME)
        project.tasks.register<PnpmSetupTask>(PnpmSetupTask.NAME)
        project.tasks.register<YarnSetupTask>(YarnSetupTask.NAME)
        project.tasks.register<BunSetupTask>(BunSetupTask.NAME)
    }

    private fun addNpmRule(
        project: Project,
        enableTaskRules: Property<Boolean>,
    ) {
        // note this rule also makes it possible to specify e.g. "dependsOn npm_install"
        project.tasks.addRule("Pattern: \"npm_<command>\": Executes an NPM command.") {
            val taskName = this
            if (taskName.startsWith("npm_") && enableTaskRules.get()) {
                project.tasks.create<NpmTask>(taskName) {
                    val tokens = taskName.split("_").drop(1) // all except first
                    npmCommand.set(tokens)
                    if (tokens.first().equals("run", ignoreCase = true)) {
                        dependsOn(NpmInstallTask.NAME)
                    }
                }
            }
        }
    }

    private fun addPnpmRule(
        project: Project,
        enableTaskRules: Property<Boolean>
    ) {
        // note this rule also makes it possible to specify e.g. "dependsOn npm_install"
        project.tasks.addRule("Pattern: \"pnpm_<command>\": Executes an PNPM command.") {
            val taskName = this
            if (taskName.startsWith("pnpm_") && enableTaskRules.get()) {
                project.tasks.register<PnpmTask>(taskName) {
                    val tokens = taskName.split("_").drop(1) // all except first
                    pnpmCommand.set(tokens)
                    if (tokens.first().equals("run", ignoreCase = true)) {
                        dependsOn(PnpmInstallTask.NAME)
                    }
                }
            }
        }
    }

    private fun addYarnRule(
        project: Project,
        enableTaskRules: Property<Boolean>,
    ) {
        // note this rule also makes it possible to specify e.g. "dependsOn yarn_install"
        project.tasks.addRule("Pattern: \"yarn_<command>\": Executes an Yarn command.") {
            val taskName = this
            if (taskName.startsWith("yarn_") && enableTaskRules.get()) {
                project.tasks.create<YarnTask>(taskName) {
                    val tokens = taskName.split("_").drop(1) // all except first
                    yarnCommand.set(tokens)
                    if (tokens.first().equals("run", ignoreCase = true)) {
                        dependsOn(YarnInstallTask.NAME)
                    }
                }
            }
        }
    }

    private fun addRepository(
        project: Project,
        distUrl: String,
        allowInsecureProtocol: Boolean?,
    ) {
        project.repositories.ivy {
            name = "Node.js"
            setUrl(distUrl)
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
            allowInsecureProtocol?.let { isAllowInsecureProtocol = it }
        }
    }

    private fun configureNodeSetupTask(
        project: Project,
        nodeExtension: NodeExtension,
    ) {
        project.tasks.withType<NodeSetupTask>().configureEach {
            nodeDir.set(nodeExtension.resolvedNodeDir)
            val archiveFileProvider = nodeExtension.computeNodeArchiveDependency()
                .map { nodeArchiveDependency ->
                    resolveNodeArchiveFile(project, nodeArchiveDependency)
                }
            nodeArchiveFile.set(project.layout.file(archiveFileProvider))
            download.convention(nodeExtension.download)
            onlyIf("Download is enabled") {
                download.get()
            }
        }
    }

    private fun configureBunTasks(project: Project) {
        project.tasks.withType<BunAbstractTask>().configureEach {
            ignoreExitValue.convention(false)
            dependsOn(project.tasks.withType<BunSetupTask>())
        }
    }

    private fun resolveNodeArchiveFile(
        project: Project,
        name: String,
    ): File {
        val dependency = project.dependencies.create(name)
        val configuration = project.configurations.detachedConfiguration(dependency)
        configuration.isTransitive = false
        return configuration.resolve().single()
    }

    companion object {
        val MINIMAL_SUPPORTED_GRADLE_VERSION: GradleVersion = GradleVersion.version("6.6")
        const val NODE_GROUP = "Node"
        const val NPM_GROUP = "npm"
        const val PNPM_GROUP = "pnpm"
        const val YARN_GROUP = "Yarn"
        const val BUN_GROUP = "Bun"
    }
}
