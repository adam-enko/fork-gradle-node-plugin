package com.github.gradle.node

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property


interface NodeToolInstallSpec {
    val download: Property<Boolean>
    val version: Property<String>
}

interface NodeExecSpec {
    val workDir: DirectoryProperty
}

abstract class NpmToolInstallSpec internal constructor() : NodeToolInstallSpec
abstract class PnpmToolInstallSpec internal constructor() : NodeToolInstallSpec
abstract class YarnToolInstallSpec internal constructor() : NodeToolInstallSpec
abstract class BunToolInstallSpec internal constructor() : NodeToolInstallSpec

abstract class NpmExecSpec internal constructor() : NodeExecSpec
abstract class PnpmExecSpec internal constructor() : NodeExecSpec
abstract class YarnExecSpec internal constructor() : NodeExecSpec
abstract class BunExecSpec internal constructor() : NodeExecSpec
