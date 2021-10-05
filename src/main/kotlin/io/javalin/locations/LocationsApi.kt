package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.HandlerType

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

fun Javalin.locations(init: ILocationInit) = path("", init)

fun Javalin.path(fragment: String, init: ILocationInit): Javalin {
    val builder = LocationBuilder(this, fragment, null)
    builder.init()

    return this
}

inline fun <reified T : Any> ILocationBuilder.get(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.GET, role, handler)

inline fun <reified T : Any> ILocationBuilder.post(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.POST, role, handler)

inline fun <reified T : Any> ILocationBuilder.put(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.PUT, role, handler)

inline fun <reified T : Any> ILocationBuilder.patch(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.PATCH, role, handler)

inline fun <reified T : Any> ILocationBuilder.delete(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.DELETE, role, handler)

inline fun <reified T : Any> ILocationBuilder.head(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.HEAD, role, handler)

inline fun <reified T : Any> ILocationBuilder.trace(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.TRACE, role, handler)

inline fun <reified T : Any> ILocationBuilder.connect(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.CONNECT, role, handler)

inline fun <reified T : Any> ILocationBuilder.options(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T, Unit>
) = handle(T::class, HandlerType.OPTIONS, role, handler)

@PublishedApi
internal val EMPTY_ROLES = emptyArray<RouteRole>()