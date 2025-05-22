package com.github.gradle.node.npm.task

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.npm.exec.NpmExecRunner
import com.github.gradle.node.task.BaseTask
import com.github.gradle.node.util.DefaultProjectApiHelper
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
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

abstract class NpmTask : BaseTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val providers: ProviderFactory

    @get:Optional
    @get:Input
    abstract val npmCommand: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val args: ListProperty<String>

    @get:Input
    val ignoreExitValue: Property<Boolean> = objects.property<Boolean>().convention(false)

    @get:Internal
    abstract val workingDir: RegularFileProperty

    @get:Input
    abstract val environment: MapProperty<String, String>

    @get:Internal
    abstract val execOverrides: Property<Action<ExecSpec>>

    @get:Internal
    val projectHelper: DefaultProjectApiHelper = objects.newInstance<DefaultProjectApiHelper>()

    @get:Internal
    val nodeExtension: NodeExtension = NodeExtension[project]

    init {
        group = NodePlugin.NPM_GROUP
        dependsOn(NpmSetupTask.NAME)
    }

    // For DSL
    @Suppress("unused")
    fun execOverrides(execOverrides: Action<ExecSpec>) {
        this.execOverrides.set(execOverrides)
    }

    @TaskAction
    fun exec() {
        val command = npmCommand.get().plus(args.get())
        val nodeExecConfiguration =
            NodeExecConfiguration(
                command,
                environment.get(),
                workingDir.asFile.orNull,
                ignoreExitValue.get(),
                execOverrides.orNull,
            )
        val npmExecRunner = objects.newInstance<NpmExecRunner>()
        result = npmExecRunner.executeNpmCommand(projectHelper, nodeExtension, nodeExecConfiguration)
    }
}
