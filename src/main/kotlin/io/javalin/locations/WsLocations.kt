package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.Role
import io.javalin.websocket.WsBinaryMessageContext
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsErrorContext
import io.javalin.websocket.WsHandler
import io.javalin.websocket.WsMessageContext
import kotlin.reflect.KClass

typealias WsConnectHandler<T> = T.(WsConnectContext) -> Unit
typealias WsMessageHandler<T> = T.(WsMessageContext) -> Unit
typealias WsBinaryMessageHandler<T> = T.(WsBinaryMessageContext) -> Unit
typealias WsCloseHandler<T> = T.(WsCloseContext) -> Unit
typealias WsErrorHandler<T> = T.(WsErrorContext) -> Unit

inline fun <reified T : Any> Javalin.ws(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: (WsLocationHandler<T>) -> Unit): Javalin = location(T::class, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.ws(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: (WsLocationHandler<T>) -> Unit): LocationBuilder = location(T::class, handler, permittedRoles)

open class WsContextAware {
    private lateinit var _context: WsContext

    protected val context: WsContext get() = _context
    internal fun context(context: WsContext) {
        this._context = context
    }
}

class WsLocationHandler<T : Any> internal constructor(private val location: KClass<T>, private val handler: WsHandler) {

    fun onConnect(wsConnectHandler: WsConnectHandler<T>) {
        handler.onConnect {
            val value = it.hydrate(location)
            wsConnectHandler.invoke(value, it)
        }
    }

    fun onMessage(wsMessageHandler: WsMessageHandler<T>) {
        handler.onMessage {
            val value = it.hydrate(location)
            wsMessageHandler.invoke(value, it)
        }
    }

    fun onBinaryMessage(wsBinaryMessageHandler: WsBinaryMessageHandler<T>) {
        handler.onBinaryMessage {
            val value = it.hydrate(location)
            wsBinaryMessageHandler.invoke(value, it)
        }
    }

    fun onClose(wsCloseHandler: WsCloseHandler<T>) {
        handler.onClose {
            val value = it.hydrate(location)
            wsCloseHandler.invoke(value, it)
        }
    }

    fun onError(wsErrorHandler: WsErrorHandler<T>) {
        handler.onError {
            val value = it.hydrate(location)
            wsErrorHandler.invoke(value, it)
        }
    }

}

@PublishedApi
internal fun <T : Any, R> LocationBuilder.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): LocationBuilder {
    when (this) {
        is LocationGroup -> this.location(location, handler, permittedRoles)
        is PathGroup -> this.location(location, handler, permittedRoles)
        else -> throw IllegalArgumentException()
    }

    return this
}


@PublishedApi
internal fun <T : Any, R> PathGroup.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): PathGroup {
    routeGroup.javalin.location(path, location, handler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any, R> LocationGroup.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): LocationGroup {
    javalin.location(location, handler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any, R> Javalin.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): Javalin {
    val locationPath = locationPath(location)
    return ws(locationPath, {
        val locationHandler = WsLocationHandler(location, it)
        handler.invoke(locationHandler)
    }, permittedRoles)
}
