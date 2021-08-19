package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.HandlerType

inline fun <reified T : Any> Javalin.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any> Javalin.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any> Javalin.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any> Javalin.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any> Javalin.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any> Javalin.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any> Javalin.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any> Javalin.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any> Javalin.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)

inline fun <reified T : Any> LocationBuilder.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)

inline fun <reified T : Any> Javalin.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): Javalin = handle(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)
inline fun <reified T : Any> LocationBuilder.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): LocationBuilder = handle(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)

inline fun <reified T : Any> Javalin.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): Javalin {
    methods.forEach { httpMethod ->
        location(T::class, httpMethod, {
            handler(this, it, httpMethod)
        }, permittedRoles)
    }

    return this
}

inline fun <reified T : Any> LocationBuilder.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): LocationBuilder {
    return when (this) {
        is LocationGroup -> this.handle(permittedRoles = permittedRoles, handler = handler, methods = *methods)
        is PathGroup -> this.handle(permittedRoles = permittedRoles, handler = handler, methods = *methods)
        else -> throw IllegalArgumentException("")
    }
}

@PublishedApi
internal inline fun <reified T : Any> LocationGroup.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): LocationGroup {
    methods.forEach { httpMethod ->
        location(T::class, httpMethod, {
            handler(this, it, httpMethod)
        }, permittedRoles)
    }

    return this
}

@PublishedApi
internal inline fun <reified T : Any> PathGroup.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): PathGroup {
    methods.forEach { httpMethod ->
        location(T::class, httpMethod, {
            handler(this, it, httpMethod)
        }, permittedRoles)
    }

    return this
}