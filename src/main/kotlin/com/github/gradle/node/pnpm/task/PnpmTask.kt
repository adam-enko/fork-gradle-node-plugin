package com.github.gradle.node.pnpm.task

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.pnpm.exec.PnpmExecRunner
import com.github.gradle.node.task.BaseTask
import com.github.gradle.node.util.DefaultProjectApiHelper
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecSpec
import javax.inject.Inject

abstract class PnpmTask : BaseTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val providers: ProviderFactory

    @get:Optional
    @get:Input
    abstract val pnpmCommand: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val args: ListProperty<String>

    @get:Input
    val ignoreExitValue: Property<Boolean> = objects.property<Boolean>().convention(false)

    @get:Internal
    abstract val workingDir: DirectoryProperty

    @get:Input
    abstract val environment: MapProperty<String, String>

    @get:Internal
    abstract val execOverrides: Property<Action<ExecSpec>>

    @get:Internal
    val projectHelper = project.objects.newInstance<DefaultProjectApiHelper>()

    @get:Internal
    val nodeExtension: NodeExtension = NodeExtension[project]

    init {
        group = NodePlugin.NODE_GROUP
        dependsOn(PnpmSetupTask.NAME)
    }

    // For DSL
    @Suppress("unused")
    fun execOverrides(execOverrides: Action<ExecSpec>) {
        this.execOverrides.set(execOverrides)
    }

    @TaskAction
    fun exec() {
        val command = pnpmCommand.get().plus(args.get())
        val nodeExecConfiguration =
            NodeExecConfiguration(
                command, environment.get(), workingDir.asFile.orNull,
                ignoreExitValue.get(), execOverrides.orNull
            )
        val pnpmExecRunner = objects.newInstance(PnpmExecRunner::class.java)
        result = pnpmExecRunner.executePnpmCommand(projectHelper, nodeExtension, nodeExecConfiguration)
    }
}
