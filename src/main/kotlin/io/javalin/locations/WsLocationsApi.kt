package io.javalin.locations

import io.javalin.Javalin
import io.javalin.websocket.*
import kotlin.reflect.KClass

typealias WsLocationHandler<C, T> = T.(C) -> Unit

class WsLocationConfig<T : Any> @PublishedApi internal constructor(
    private val location: KClass<T>,
    private val wsConfig: WsConfig
) {
    fun onConnect(handler: WsLocationHandler<WsConnectContext, T>) {
        wsConfig.onConnect {
            val hydrated = it.hydrate(location)
            handler.invoke(hydrated, it)
        }
    }

    fun onMessage(handler: WsLocationHandler<WsMessageContext, T>) {
        wsConfig.onMessage {
            val hydrated = it.hydrate(location)
            handler.invoke(hydrated, it)
        }
    }

    fun onBinaryMessage(handler: WsLocationHandler<WsBinaryMessageContext, T>) {
        wsConfig.onBinaryMessage {
            val hydrated = it.hydrate(location)
            handler.invoke(hydrated, it)
        }
    }

    fun onClose(handler: WsLocationHandler<WsCloseContext, T>) {
        wsConfig.onClose {
            val hydrated = it.hydrate(location)
            handler.invoke(hydrated, it)
        }
    }

    fun onError(handler: WsLocationHandler<WsErrorContext, T>) {
        wsConfig.onError {
            val hydrated = it.hydrate(location)
            handler.invoke(hydrated, it)
        }
    }
}

inline fun <reified T : Any> Javalin.ws(noinline config: (WsLocationConfig<T>) -> Unit) {
    val locationPath = LocationBuilder.locationPath(T::class)
    ws(locationPath) {
        config.invoke(WsLocationConfig(T::class, it))
    }
}

inline fun <reified T : Any> ILocationBuilder.ws(noinline config: (WsLocationConfig<T>) -> Unit) {
    val extendedBuilder = this as LocationBuilder
    val locationPath = LocationBuilder.normalize(extendedBuilder.path, LocationBuilder.locationPath(T::class))

    extendedBuilder.javalin.ws(locationPath) {
        config.invoke(WsLocationConfig(T::class, it))
    }
}