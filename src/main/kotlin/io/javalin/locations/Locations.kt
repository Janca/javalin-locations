package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.RouteRole
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.plugin.json.JsonMapper
import io.javalin.plugin.json.jsonMapper
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

typealias ILocationInit = ILocationBuilder.() -> Unit
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

open class ContextAwareLocation {

    lateinit var context: Context
        internal set

}

interface ILocationBuilder {
    fun handler(handler: ILocationHandlerFactory)

    fun jsonMapper(mapper: JsonMapper)
    fun jsonMapper(init: () -> JsonMapper)

    fun jsonMapper(): JsonMapper

    fun errorHandler(handler: (Throwable, Context) -> Unit)

    fun Context.payload(o: Any)
    fun Context.stream(o: Any)

    fun path(fragment: String, init: ILocationInit)
}

internal interface IExtendedLocationBuilder {
    val javalin: Javalin
    val jsonMapper: JsonMapper
    val handlerFactory: ILocationHandlerFactory
    val errorHandler: ILocationErrorHandler?
}

@PublishedApi
internal class LocationBuilder(
    override val javalin: Javalin,
    internal val path: String,
    parent: LocationBuilder? = null
) : ILocationBuilder, IExtendedLocationBuilder {
    override var errorHandler: ILocationErrorHandler? = parent?.errorHandler
    override var handlerFactory: ILocationHandlerFactory = parent?.handlerFactory ?: { it }
    override var jsonMapper: JsonMapper = parent?.jsonMapper ?: javalin.jsonMapper()

    override fun handler(handler: ILocationHandlerFactory) {
        this.handlerFactory = handler
    }

    override fun jsonMapper(mapper: JsonMapper) {
        this.jsonMapper = mapper
    }

    override fun jsonMapper(init: () -> JsonMapper) {
        this.jsonMapper = init.invoke()
    }

    override fun jsonMapper(): JsonMapper = jsonMapper

    override fun path(fragment: String, init: ILocationInit) {
        val locationPath = normalize(path, fragment)
        val locationBuilder = LocationBuilder(javalin, locationPath, this)
        init.invoke(locationBuilder)
    }

    override fun errorHandler(handler: (Throwable, Context) -> Unit) {
        this.errorHandler = handler
    }

    override fun Context.payload(o: Any) {
        val json = jsonMapper().toJsonString(o)
        result(json).contentType("application/json")
    }

    override fun Context.stream(o: Any) {
        val stream = jsonMapper().toJsonStream(o)
        result(stream).contentType(ContentType.APPLICATION_JSON)
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

            return buildString {
                var enclosingClass: Class<*>? = location.java

                val parentPathLocations = LinkedList<Location>()
                do {
                    val next = enclosingClass?.enclosingClass ?: break
                    if (enclosingClass == next) {
                        break
                    }

                    enclosingClass = next
                    val enclosingAnnotation = locationAnnotation(next) ?: break
                    parentPathLocations.addFirst(enclosingAnnotation)
                } while (true)

                val parentPaths = parentPathLocations.map { it.path }
                    .toTypedArray()

                append(normalize(*parentPaths))
                append(locationAnnotation.path)
            }.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException(
                    "Location '${location.qualifiedName}' cannot have empty path; specify path on '${location.qualifiedName}' or define non-empty path on parent if nested."
                )
        }

        fun normalize(vararg fragment: String): String {
            return fragment.joinToString("") {
                when {
                    it.isBlank() -> ""
                    it.startsWith("/") -> it
                    else -> "/$it"
                }
            }
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

internal fun <T : Any, R : Any> location(
    builder: ILocationBuilder,
    location: KClass<T>,
    method: HandlerType,
    roles: Array<out RouteRole>,
    handler: ILocationExtendedHandler<T, R>
): ILocationBuilder {
    val extendedBuilder = builder as LocationBuilder

    val builderPath = extendedBuilder.path
    val locationPath = LocationBuilder.normalize(
        builderPath,
        LocationBuilder.locationPath(location)
    )

    val javalin = extendedBuilder.javalin

    val defaultHandler = Handler { ctx ->
        try {
            val locationInst = ctx.hydrate(location, extendedBuilder)
            when (val response: R = handler.invoke(locationInst, ctx)) {
                !is Unit -> {
                    val json = extendedBuilder.jsonMapper().toJsonString(response)
                    ctx.result(json).contentType(ContentType.APPLICATION_JSON)
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

