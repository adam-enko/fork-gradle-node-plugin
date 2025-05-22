package com.github.gradle.node

//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Provides a parsed view of package.json
 */
abstract class PackageJsonExtension
@Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) {

//    /**
//     * Raw JsonNode returned by Jackson, this may be removed in a future release
//     */
//    abstract val node: Property<JsonNode>

    val packageJsonContent: Property<String> = objects.property<String>()
        .convention(
            providers.provider {
                layout.projectDirectory.file("package.json")
                    .asFile
                    .takeIf { it.exists() }
                    ?.readText()
            }
        )

//    init {
//        node.finalizeValueOnRead()
//        node.set(providers.provider {
//            layout.projectDirectory.file("package.json")
//                .asFile
//                .let(ObjectMapper()::readTree)
//        })
//
//        packageJsonContent.convention(
//            providers.provider {
//                layout.projectDirectory.file("package.json")
//                    .asFile
//                    .takeIf { it.exists() }
//                    ?.readText()
//            }
//        )
//
//    }

    private val packageJsonData: Provider<Map<*, *>> = packageJsonContent.map { text ->
        JsonSlurper().parseText(text) as? Map<*, *>
            ?: error("Could not parse package.json")
    }

    val name: Provider<String> =
        packageJsonData.map2 { it["name"]?.toString() }
//        providers.provider { node.get().get("name")?.asText() }

    val version: Provider<String> =
        packageJsonData.map2 { it["version"]?.toString() }
//        providers.provider { node.get().get("version")?.asText() }

    val description: Provider<String> =
        packageJsonData.map2 { it["description"]?.toString() }

    val homepage: Provider<String> =
        packageJsonData.map2 { it["homepage"]?.toString() }

    val license: Provider<String> =
        packageJsonData.map2 { it["license"]?.toString() }

    val private: Provider<Boolean> =
        packageJsonData.map2 { it["private"] as? Boolean }

    /**
     * Get the text value of a given field
     */
    fun get(name: String): String? {
        return packageJsonData.orNull?.get(name)?.toString()
    }

    /**
     * Get the boolean value of a given field
     */
    fun getBoolean(name: String): Boolean? {
        return packageJsonData.orNull?.get(name) as? Boolean
    }

    /**
     * Get the text value of a field containing nested objects
     *
     * e.g. `{ "outer": { "inner": "nested } }`
     */
    fun get(vararg names: String): String? {
        require(names.isNotEmpty()) { "names must not be empty" }
        val data = packageJsonData.orNull ?: return null
        val path = names.dropLast(1)
        val last = path.fold(data) { acc, next ->
            acc[next] as? Map<*, *> ?: return null
        }
        return last.get(names.last())?.toString()
    }

    private fun <T : Any, R : Any> Provider<T>.map2(mapper: (T) -> R?): Provider<R> =
        flatMap { providers.provider { mapper(it) } }

    companion object {
        /**
         * Extension name in Gradle
         */
        const val NAME = "packageJson"
    }
}
