package com.github.gradle.node.bun.task

import com.github.gradle.node.bun.exec.BunExecRunner
import com.github.gradle.node.exec.NodeExecConfiguration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class BunTask @Inject internal constructor(
    private val objects: ObjectFactory,
) : BunAbstractTask() {

    @get:Optional
    @get:Input
    abstract val bunCommand: ListProperty<String>

    @TaskAction
    fun exec() {
        val command = bunCommand.get().plus(args.get())
        val nodeExecConfiguration =
            NodeExecConfiguration(
                command = command,
                environment = environment.get(),
                workingDir = workingDir.asFile.orNull,
                ignoreExitValue = ignoreExitValue.get(),
                execOverrides = execOverrides.orNull
            )
        val bunExecRunner = objects.newInstance(BunExecRunner::class.java)
        result = bunExecRunner.executeBunCommand(workingDir.get().asFile, nodeExecConfiguration)
    }
}
