package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Location(
    val path: String,
    val allowedHydrationMethods: Array<out HydrationMethod> = [
        HydrationMethod.POST_FORM_PARAMETERS,
        HydrationMethod.QUERY_PARAMETERS,
        HydrationMethod.URL_PARAMETERS
    ]
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FormParameter(val name: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostBody

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class QueryParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class UrlParameter(val name: String = "")

enum class HydrationMethod {
    POST_FORM_PARAMETERS,
    QUERY_PARAMETERS,
    URL_PARAMETERS
}


// PRIMARY ENTRY-POINT
inline fun Javalin.locations(init: LocationGroup.() -> Unit): Javalin {
    init(LocationGroup(this))
    return this
}

@PublishedApi
internal val EMPTY_ROLE_SET: Set<Role> = emptySet()

@PublishedApi
internal val HTTP_HANDLER_TYPES = HandlerType.values().filter { it.isHttpMethod() }.toTypedArray()


interface LocationBuilder {
    fun path(path: String, init: PathGroup.() -> Unit): LocationBuilder
}

class PathGroup internal constructor(internal val routeGroup: LocationGroup, path: String) : LocationBuilder {
    internal val path = normalizePath(path)

    override fun path(path: String, init: PathGroup.() -> Unit): LocationBuilder {
        val routePath = this.path + normalizePath(path)
        init(PathGroup(routeGroup, routePath))
        return routeGroup
    }
}

class LocationGroup @PublishedApi internal constructor(internal val javalin: Javalin) : LocationBuilder {
    override fun path(path: String, init: PathGroup.() -> Unit): LocationBuilder {
        init(PathGroup(this, path))
        return this
    }
}

open class ContextAware {
    private lateinit var _context: Context

    protected val context: Context get() = _context
    internal fun context(context: Context) {
        this._context = context
    }
}

@PublishedApi
internal fun <T : Any, R> LocationBuilder.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> R, permittedRoles: Set<Role>): LocationBuilder {
    when (this) {
        is LocationGroup -> this.location(location, method, handler, permittedRoles)
        is PathGroup -> this.location(location, method, handler, permittedRoles)
        else -> throw IllegalArgumentException()
    }

    return this
}

@PublishedApi
internal fun <T : Any, R> PathGroup.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> R, permittedRoles: Set<Role>): PathGroup {
    val locationPath = locationPath(location)
    val locationHandler = locationHandler(location, handler)
    routeGroup.javalin.addHandler(method, "$path$locationPath", locationHandler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any, R> LocationGroup.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> R, permittedRoles: Set<Role>): LocationGroup {
    javalin.location(location, method, handler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any, R> Javalin.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> R, permittedRoles: Set<Role>): Javalin {
    val locationPath = locationPath(location)
    val locationHandler = locationHandler(location, handler)
    return addHandler(method, locationPath, locationHandler, permittedRoles)
}

@PublishedApi
internal fun <T : Any, R> Javalin.location(pathPrefix: String, location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): Javalin {
    val locationPath = locationPath(location)
    return ws("$pathPrefix$locationPath", {
        val locationHandler = WsLocationHandler(location, it)
        handler.invoke(locationHandler)
    }, permittedRoles)
}


@PublishedApi
internal fun normalizePath(path: String): String {
    val trimmed = path.trim()
    return when {
        trimmed.startsWith('/') -> trimmed
        else -> "/$trimmed"
    }
}

internal fun <T : Any> locationPath(location: KClass<T>): String {
    val path = buildString {
        var enclosingClass: Class<*>? = null

        val parentPathLocations = LinkedList<Location>()
        do {
            val next = location.java.enclosingClass
            if (enclosingClass == next) {
                break
            }

            enclosingClass = next
            val enclosingAnnotation = enclosingClass.findAnnotation<Location>() ?: break
            parentPathLocations.addFirst(enclosingAnnotation)
        } while (true)

        parentPathLocations.forEach {
            val path = normalizePath(it.path)
            append(path)
        }

        val locationAnnotation = location.findAnnotation<Location>()
            ?: throw IllegalArgumentException("Location '${location.qualifiedName}' is missing annotation 'Location'.")

        val locationPath = normalizePath(locationAnnotation.path)
        append(locationPath)
    }

    return path
}

internal fun <T : Any, R> locationHandler(location: KClass<T>, handler: T.(Context) -> R): Handler {
    return Handler { ctx ->
        when (val response: R = handler(ctx.hydrate(location), ctx)) {
            !is Unit -> ctx.json(response as Any)
        }
    }
}

internal inline fun <reified T : Annotation> KAnnotatedElement.hasAnnotation(): Boolean = findAnnotation<T>() != null
internal inline fun <reified T : Annotation> Class<*>.findAnnotation(): T? {
    return getDeclaredAnnotation(T::class.java)
}