package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

typealias ILocationInit = ILocationBuilder.() -> Unit
typealias ILocationAfterHandler<T> = T.(Context) -> Unit
typealias ILocationExtendedAfterHandler<T, R> = T.(Context, R?) -> Unit
typealias ILocationHandler<T> = ILocationExtendedHandler<T, Unit>
typealias ILocationMethodHandler<T> = ILocationExtendedMethodHandler<T, Unit>
typealias ILocationExtendedHandler<T, R> = T.(ctx: Context) -> R
typealias ILocationExtendedMethodHandler<T, R> = T.(method: HandlerType, ctx: Context) -> R
typealias ILocationErrorHandler = (exception: Throwable, ctx: Context) -> Unit
typealias ILocationHandlerFactory = (parent: Handler) -> Handler

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Location(val path: String, val eagerHydration: Boolean = true)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostBody

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FormParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PathParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class QueryParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IgnoreParameterType(vararg val types: KClass<*>)

open class ContextAwareLocation {
    internal lateinit var backingContext: Context
    protected val context: Context get() = backingContext
}

interface ILocationBuilder {
    fun handlerFactory(handler: ILocationHandlerFactory)

    fun errorHandler(handler: (Throwable, Context) -> Unit)

    fun path(fragment: String, init: ILocationInit)
}

internal interface IExtendedLocationBuilder {
    val javalin: Javalin
    val handlerFactory: ILocationHandlerFactory
    val errorHandler: ILocationErrorHandler?
}

@PublishedApi
internal class LocationBuilder(
    override val javalin: Javalin,
    val path: String,
    parent: LocationBuilder? = null
) : ILocationBuilder, IExtendedLocationBuilder {
    override var errorHandler: ILocationErrorHandler? = parent?.errorHandler
    override var handlerFactory: ILocationHandlerFactory = parent?.handlerFactory ?: { it }

    override fun handlerFactory(handler: ILocationHandlerFactory) {
        this.handlerFactory = handler
    }

    override fun path(fragment: String, init: ILocationInit) {
        val locationPath = normalize(path, fragment)
        val locationBuilder = LocationBuilder(javalin, locationPath, this)
        init.invoke(locationBuilder)
    }

    override fun errorHandler(handler: (Throwable, Context) -> Unit) {
        this.errorHandler = handler
    }

    companion object {

        fun <T : Any> locationAnnotation(location: KClass<T>): Location? {
            return location.findAnnotation()
        }

        fun <T : Any> locationAnnotation(location: Class<T>): Location? {
            return location.getDeclaredAnnotation(Location::class.java)
        }

        fun <T : Any> locationPath(location: KClass<T>): String {
            val locationAnnotation = locationAnnotation(location)
                ?: throw IllegalArgumentException("Location '${location.qualifiedName}' is missing required annotation 'Location'.")


            var enclosingClass: Class<*>? = location.java
            var workingPath = ""

            do {
                val next = enclosingClass?.enclosingClass ?: break
                if (enclosingClass == next) {
                    break
                }

                enclosingClass = next
                val enclosingAnnotation = locationAnnotation(next) ?: break

                val enclosingPath = enclosingAnnotation.path
                if (enclosingPath.isBlank()) {
                    break
                }

                workingPath = normalize(enclosingPath, workingPath)
            } while (true)

            workingPath = normalize(workingPath, locationAnnotation.path)
            if (workingPath.isBlank()) {
                throw IllegalArgumentException("Location '${location.qualifiedName}' cannot have empty path; specify path on '${location.qualifiedName}' or define non-empty path on parent if nested.")
            }

            return workingPath
        }

        fun normalize(path: String, vararg fragment: String): String {
            var workingPath = when {
                path.isBlank() -> ""
                path.startsWith("/") -> path
                else -> "/$path"
            }

            fragment.forEach {
                workingPath += when {
                    it.isBlank() -> ""
                    it.startsWith("/") -> it
                    else -> "/$it"
                }
            }

            return workingPath
        }
    }
}

@PublishedApi
internal fun <T : Any, R : Any> ILocationBuilder.handle(
    location: KClass<T>,
    method: HandlerType,
    roles: Array<out RouteRole>,
    handler: ILocationExtendedHandler<T, R>
): ILocationBuilder {
    return location(this, location, method, roles, handler)
}

@PublishedApi
internal fun <T : Any, R : Any> location(
    builder: ILocationBuilder,
    location: KClass<T>,
    methods: Array<out HandlerType>,
    roles: Array<out RouteRole>,
    handler: ILocationExtendedMethodHandler<T, R>
): ILocationBuilder {
    methods.forEach { method ->
        location(builder, location, method, roles) { ctx ->
            handler.invoke(this, method, ctx)
        }
    }

    return builder
}

@PublishedApi
internal fun <T : Any> before(
    builder: ILocationBuilder,
    location: KClass<T>,
    handler: ILocationHandler<T>,
): ILocationBuilder {
    val extendedBuilder = builder as LocationBuilder
    val javalin = extendedBuilder.javalin

    val builderPath = extendedBuilder.path
    val locationPath = LocationBuilder.normalize(builderPath, LocationBuilder.locationPath(location))
    javalin.before(locationPath) {
        val hydrated = it.hydrate(location)
        handler.invoke(hydrated, it)
    }

    return builder
}

@PublishedApi
internal fun <T : Any, R : Any> after(
    builder: ILocationBuilder,
    location: KClass<T>,
    handler: ILocationExtendedAfterHandler<T, R>,
): ILocationBuilder {
    val extendedBuilder = builder as LocationBuilder
    val javalin = extendedBuilder.javalin

    val builderPath = extendedBuilder.path
    val locationPath = LocationBuilder.normalize(builderPath, LocationBuilder.locationPath(location))
    javalin.after(locationPath) {
        val localHandlerResult = it.attribute<R>("local-handler-result")
        val hydrated = it.hydrate(location)
        handler.invoke(hydrated, it, localHandlerResult)
    }

    return builder
}

internal fun <T : Any, R : Any> location(
    builder: ILocationBuilder,
    location: KClass<T>,
    method: HandlerType,
    roles: Array<out RouteRole>,
    handler: ILocationExtendedHandler<T, R>
): ILocationBuilder {
    val extendedBuilder = builder as LocationBuilder
    val javalin = extendedBuilder.javalin

    val builderPath = extendedBuilder.path
    val locationPath = LocationBuilder.normalize(builderPath, LocationBuilder.locationPath(location))

    val defaultHandler = Handler { ctx ->
        try {
            val locationInst = ctx.hydrate(location)
            when (val response: R = handler.invoke(locationInst, ctx)) {
                !is Unit -> {
                    ctx.json(response)
                    ctx.attribute("local-handler-result", response)
                }
            }
        } catch (e: Throwable) {
            val errorHandler = extendedBuilder.errorHandler ?: throw e
            errorHandler.invoke(e, ctx)
        }
    }

    val locationHandler = builder.handlerFactory.invoke(defaultHandler)
    javalin.addHandler(method, locationPath, locationHandler, *roles)

    return builder
}

