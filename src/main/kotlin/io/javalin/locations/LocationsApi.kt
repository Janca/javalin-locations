package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.RouteRole
import io.javalin.http.HandlerType

fun Javalin.locations(init: ILocationInit) = path("", init)

fun Javalin.path(fragment: String, init: ILocationInit): Javalin {
    val builder = LocationBuilder(this, fragment, null)
    builder.init()

    return this
}

fun ILocationBuilder.group(init: ILocationInit) = path("", init)

inline fun <reified T : Any> ILocationBuilder.get(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.GET, role, handler)

inline fun <reified T : Any> ILocationBuilder.post(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.POST, role, handler)

inline fun <reified T : Any> ILocationBuilder.put(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.PUT, role, handler)

inline fun <reified T : Any> ILocationBuilder.patch(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.PATCH, role, handler)

inline fun <reified T : Any> ILocationBuilder.delete(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.DELETE, role, handler)

inline fun <reified T : Any> ILocationBuilder.head(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.HEAD, role, handler)

inline fun <reified T : Any> ILocationBuilder.trace(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.TRACE, role, handler)

inline fun <reified T : Any> ILocationBuilder.connect(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.CONNECT, role, handler)

inline fun <reified T : Any> ILocationBuilder.options(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationHandler<T>
) = handle(T::class, HandlerType.OPTIONS, role, handler)

inline fun <reified T : Any> ILocationBuilder.handle(
    vararg methods: HandlerType,
    roles: Array<out RouteRole> = EMPTY_ROLES,
    noinline handler: ILocationMethodHandler<T>
): ILocationBuilder = location(this, T::class, methods, roles, handler)

@PublishedApi
internal val EMPTY_ROLES = emptyArray<RouteRole>()