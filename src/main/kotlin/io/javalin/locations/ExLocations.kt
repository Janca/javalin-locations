package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.HandlerType

inline fun <reified T : Any, R> Javalin.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): Javalin = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)
inline fun <reified T : Any, R> Javalin.ws(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: (WsLocationHandler<T>) -> R): Javalin = location(T::class, handler, permittedRoles)

inline fun <reified T : Any, R> LocationBuilder.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> R): LocationBuilder = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)
inline fun <reified T : Any, R> LocationBuilder.ws(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: (WsLocationHandler<T>) -> R): LocationBuilder = location(T::class, handler, permittedRoles)