package com.github.gradle.node

import com.github.gradle.node.npm.proxy.ProxySettings
import com.github.gradle.node.util.Platform
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

abstract class NodeExtension internal constructor() {

    /**
     * The directory where Node.js is unpacked (when [download] is `true`).
     */
    abstract val workDir: DirectoryProperty

    /**
     * The directory where npm is installed (when a specific version is defined)
     */
    abstract val npmWorkDir: DirectoryProperty

    /**
     * The directory where pnpm is installed (when a pnpm task is used)
     */
    abstract val pnpmWorkDir: DirectoryProperty

    /**
     * The directory where yarn is installed (when a Yarn task is used)
     */
    abstract val yarnWorkDir: DirectoryProperty

    /**
     * The directory where Bun is installed (when a Bun task is used)
     */
    abstract val bunWorkDir: DirectoryProperty

    /**
     * The Node.js project directory location
     * This is where the package.json file and node_modules directory are located
     * By default it is at the root of the current project
     */
    abstract val nodeProjectDir: DirectoryProperty

    /**
     * Version of node to download and install (only used if [download] is `true`)
     * It will be unpacked in the workDir
     */
    abstract val version: Property<String>

    /**
     * Version of npm to use.
     * If specified, installs it in the [npmWorkDir].
     * If empty, the plugin will use the npm command bundled with Node.js.
     */
    abstract val npmVersion: Property<String>

    /**
     * Version of pnpm to use.
     * Any pnpm task first installs pnpm in the [pnpmWorkDir].
     * It uses the specified version if defined and the latest version otherwise (by default).
     */
    abstract val pnpmVersion: Property<String>

    /**
     * Version of Yarn to use
     * Any Yarn task first installs Yarn in the yarnWorkDir
     * It uses the specified version if defined and the latest version otherwise (by default)
     */
    abstract val yarnVersion: Property<String>

    /**
     * Version of Bun to use
     * Any Bun task first installs Bun in the bunWorkDir
     * It uses the specified version if defined and the latest version otherwise (by default)
     */
    abstract val bunVersion: Property<String>

    /**
     * Base URL for fetching node distributions
     * Only used if download is true
     * Change it if you want to use a mirror
     * Or set to null if you want to add the repository on your own.
     */
    abstract val distBaseUrl: Property<String>

    /**
     * Specifies whether it is acceptable to communicate with the Node.js repository over an insecure HTTP connection.
     * Only used if download is true
     * Change it to true if you use a mirror that uses HTTP rather than HTTPS
     * Or set to null if you want to use Gradle's default behaviour.
     */
    abstract val allowInsecureProtocol: Property<Boolean>

    abstract val npmCommand: Property<String>
    abstract val npxCommand: Property<String>
    abstract val pnpmCommand: Property<String>
    abstract val yarnCommand: Property<String>
    abstract val bunCommand: Property<String>
    abstract val bunxCommand: Property<String>

    /**
     * The npm command executed by the npmInstall task
     * By default it is install but it can be changed to ci
     */
    abstract val npmInstallCommand: Property<String>

    /**
     * Whether to download and install a specific Node.js version or not
     * If false, it will use the globally installed Node.js
     * If true, it will download node using above parameters
     * Note that npm is bundled with Node.js
     */
    abstract val download: Property<Boolean>

    /**
     * Whether the plugin automatically should add the proxy configuration to npm and yarn commands
     * according the proxy configuration defined for Gradle
     *
     * Disable this option if you want to configure the proxy for npm or yarn on your own
     * (in the .npmrc file for instance)
     *
     */
    abstract val nodeProxySettings: Property<ProxySettings>

    /**
     * Use fast NpmInstall logic, excluding `node_modules` for output tracking resulting in a significantly faster
     * npm install/ci configuration at the cost of slightly decreased correctness in certain circumstances.
     *
     * In practice this means that if you change `node_modules` through other means than npm install/ci
     * [com.github.gradle.node.npm.task.NpmInstallTask]
     * tasks will continue being up-to-date, but if you're modifying `node_modules` through
     * other tools you may have other correctness problems and surfacing them here may be preferred.
     *
     * https://docs.npmjs.com/cli/v8/configuring-npm/package-lock-json#hidden-lockfiles
     *
     * Requires npm 7 or later.
     * This will become the default in 4.x.
     */
    abstract val fastNpmInstall: Property<Boolean>

    /**
     * Disable functionality that requires newer versions of npm
     *
     * If you're not downloading Node.js and using old version of Node or npm
     * set this to true to disable functionality that makes use of newer functionality.
     *
     * This will be removed in 4.x
     */
    abstract val oldNpm: Property<Boolean>

    /**
     * Create rules for automatic task creation
     *
     * Disabling this will prevent the npm_ npx_ yarn_ pnpm_ tasks from being
     * automatically created.
     * It's recommended to turn this off after you've gotten comfortable
     * with the plugin and register your own tasks instead of relying on the rule.
     */
    abstract val enableTaskRules: Property<Boolean>


    /**
     * Computed path to nodejs directory
     */
    @Deprecated(message = "replaced with resolvedNodeDir", replaceWith = ReplaceWith("resolvedNodeDir"))
    abstract val computedNodeDir: DirectoryProperty

    /**
     * Computed path to nodejs directory
     */
    abstract val resolvedNodeDir: DirectoryProperty

    /**
     * Operating system and architecture
     */
    @Deprecated(message = "replaced with resolvedPlatform", replaceWith = ReplaceWith("resolvedPlatform"))
    abstract val computedPlatform: Property<Platform>

    /**
     * Operating system and architecture
     */
    abstract val resolvedPlatform: Property<Platform>

    @Deprecated(
        "useGradleProxySettings has been replaced with nodeProxySettings",
        replaceWith = ReplaceWith("nodeProxySettings.set(i)")
    )
    fun setUseGradleProxySettings(value: Boolean) {
        nodeProxySettings.set(if (value) ProxySettings.SMART else ProxySettings.OFF)
    }

    companion object {
        /**
         * Extension name in Gradle
         */
        const val NAME = "node"

        /**
         * Default version of Node to download if none is set
         */
        const val DEFAULT_NODE_VERSION = "18.17.1"

        /**
         * Default version of npm to download if none is set
         */
        const val DEFAULT_NPM_VERSION = "9.6.7"

        @JvmStatic
        @Deprecated("internal util")
        operator fun get(project: Project): NodeExtension {
            return project.extensions.getByType()
        }

        @Deprecated("internal util")
        @JvmStatic
        fun create(project: Project): NodeExtension {
            return project.extensions.create<NodeExtension>(NAME, project)
        }
    }
}
