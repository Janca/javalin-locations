package io.javalin.locations

import io.javalin.core.security.RouteRole
import io.javalin.http.HandlerType

inline fun <reified T : Any, R : Any> ILocationBuilder.get(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.GET, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.post(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.POST, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.put(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.PUT, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.patch(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.PATCH, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.delete(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.DELETE, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.head(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.HEAD, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.trace(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.TRACE, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.connect(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.CONNECT, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.options(
    vararg role: RouteRole = EMPTY_ROLES,
    noinline handler: ILocationExtendedHandler<T, R>
) = handle(T::class, HandlerType.OPTIONS, role, handler)

inline fun <reified T : Any, R : Any> ILocationBuilder.handle(
    vararg methods: HandlerType,
    roles: Array<out RouteRole> = EMPTY_ROLES,
    noinline handler: ILocationExtendedMethodHandler<T, R>
): ILocationBuilder = location(this, T::class, methods, roles, handler)