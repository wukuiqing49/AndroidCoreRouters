package com.wkq.router.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class RouterExtension {
    abstract val moduleName: Property<String>
    abstract val version: Property<String>
    abstract val group: Property<String>
    abstract val addApiDependency: Property<Boolean>
    abstract val addProcessorDependency: Property<Boolean>
}

class RouterGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("wkqRouter", RouterExtension::class.java)
        extension.moduleName.convention(defaultModuleName(project))
        extension.version.convention(
            project.providers.gradleProperty("androidCoreRoutersVersion")
                .orElse(project.providers.gradleProperty("routerVersion"))
                .orElse("v1.0.5")
        )
        extension.group.convention(
            project.providers.gradleProperty("androidCoreRoutersGroup")
                .orElse("com.github.wukuiqing49.AndroidCoreRouters")
        )
        extension.addApiDependency.convention(true)
        extension.addProcessorDependency.convention(true)

        project.pluginManager.apply("com.google.devtools.ksp")

        project.plugins.withId("com.android.application") {
            configureRouter(project, extension)
        }
        project.plugins.withId("com.android.library") {
            configureRouter(project, extension)
        }
    }

    private fun configureRouter(project: Project, extension: RouterExtension) {
        project.afterEvaluate {
            val version = extension.version.get()
            val group = extension.group.get()

            if (extension.addApiDependency.get()) {
                project.dependencies.add("implementation", "$group:core_router_api:$version")
            }
            if (extension.addProcessorDependency.get()) {
                project.dependencies.add("ksp", "$group:core_router_processor:$version")
            }

            configureKspModuleName(project, extension.moduleName.get())
        }
    }

    private fun configureKspModuleName(project: Project, moduleName: String) {
        val kspExtension = project.extensions.findByName("ksp")
        if (kspExtension != null) {
            val argMethod = kspExtension.javaClass.methods.firstOrNull { method ->
                method.name == "arg" &&
                    method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == String::class.java &&
                    method.parameterTypes[1] == String::class.java
            }
            if (argMethod != null) {
                argMethod.invoke(kspExtension, "moduleName", moduleName)
                return
            }
        }

        project.logger.warn(
            "AndroidCoreRouters: KSP extension was not found. " +
                "Make sure com.google.devtools.ksp is available in pluginManagement."
        )
    }

    private fun defaultModuleName(project: Project): String {
        return project.path
            .trim(':')
            .replace(':', '_')
            .let { value ->
                val normalized = value.ifBlank { project.name }
                    .replace(Regex("[^A-Za-z0-9_]"), "_")
                if (normalized.firstOrNull()?.isLetter() == true) normalized else "m_$normalized"
            }
    }
}
