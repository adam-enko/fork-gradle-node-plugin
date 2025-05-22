package com.github.gradle.node.bun.task

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.task.BaseTask
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
import org.gradle.process.ExecSpec
import javax.inject.Inject

abstract class BunAbstractTask internal constructor() : BaseTask() {

//    @get:Inject
//    abstract val objects: ObjectFactory
//
//    @get:Inject
//    abstract val providers: ProviderFactory

    @get:Optional
    @get:Input
    abstract val args: ListProperty<String>

    @get:Input
    abstract val ignoreExitValue: Property<Boolean>

    @get:Input
    abstract val environment: MapProperty<String, String>

    @get:Internal
    abstract val workingDir: DirectoryProperty

    @get:Internal
    abstract val execOverrides: Property<Action<ExecSpec>>

//    @get:Internal
//    val projectHelper = project.objects.newInstance<DefaultProjectApiHelper>()

//    @get:Internal
//    val nodeExtension = NodeExtension[project]

    init {
        group = NodePlugin.BUN_GROUP
//        dependsOn(BunSetupTask.NAME)
    }

    // For DSL
    @Suppress("unused")
    fun execOverrides(execOverrides: Action<ExecSpec>) {
        this.execOverrides.set(execOverrides)
    }
}
