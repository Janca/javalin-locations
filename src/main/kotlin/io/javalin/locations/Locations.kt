package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsBinaryMessageContext
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsErrorContext
import io.javalin.websocket.WsHandler
import io.javalin.websocket.WsMessageContext
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

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

typealias WsConnectHandler<T> = T.(WsConnectContext) -> Unit
typealias WsMessageHandler<T> = T.(WsMessageContext) -> Unit
typealias WsBinaryMessageHandler<T> = T.(WsBinaryMessageContext) -> Unit
typealias WsCloseHandler<T> = T.(WsCloseContext) -> Unit
typealias WsErrorHandler<T> = T.(WsErrorContext) -> Unit

class WsLocationHandler<T : Any>(private val location: KClass<T>, private val handler: WsHandler) {

    private var wsConnectHandler: WsConnectHandler<T>? = null
    private var wsMessageHandler: WsMessageHandler<T>? = null
    private var wsBinaryMessageHandler: WsBinaryMessageHandler<T>? = null
    private var wsCloseHandler: WsCloseHandler<T>? = null
    private var wsErrorHandler: WsErrorHandler<T>? = null

    fun onConnect(wsConnectHandler: WsConnectHandler<T>) {
        this.wsConnectHandler = wsConnectHandler
        handler.onConnect {
            val value = it.hydrate(location)
            wsConnectHandler.invoke(value, it)
        }
    }

    fun onMessage(wsMessageHandler: WsMessageHandler<T>) {
        this.wsMessageHandler = wsMessageHandler
        handler.onMessage {
            val value = it.hydrate(location)
            wsMessageHandler.invoke(value, it)
        }
    }

    fun onBinaryMessage(wsBinaryMessageHandler: WsBinaryMessageHandler<T>) {
        this.wsBinaryMessageHandler = wsBinaryMessageHandler
        handler.onBinaryMessage {
            val value = it.hydrate(location)
            wsBinaryMessageHandler.invoke(value, it)
        }
    }

    fun onClose(wsCloseHandler: WsCloseHandler<T>) {
        this.wsCloseHandler = wsCloseHandler
        handler.onClose {
            val value = it.hydrate(location)
            wsCloseHandler.invoke(value, it)
        }
    }

    fun onError(wsErrorHandler: WsErrorHandler<T>) {
        this.wsErrorHandler = wsErrorHandler
        handler.onError {
            val value = it.hydrate(location)
            wsErrorHandler.invoke(value, it)
        }
    }

}

// PRIMARY ENTRY-POINT
inline fun Javalin.locations(init: LocationGroup.() -> Unit): Javalin {
    init(LocationGroup(this))
    return this
}

fun <T : Any> Context.result(payload: T): Context {
    val json = JavalinJson.toJson(payload)
    return result(json).contentType("application/json")
}

@PublishedApi
internal val EMPTY_ROLE_SET: Set<Role> = emptySet()

inline fun <reified T : Any> Javalin.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any> Javalin.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any> Javalin.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any> Javalin.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any> Javalin.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any> Javalin.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any> Javalin.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any> Javalin.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any> Javalin.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): Javalin = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)
inline fun <reified T : Any> Javalin.ws(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: (WsLocationHandler<T>) -> Unit): Javalin = location(T::class, handler, permittedRoles)

inline fun <reified T : Any> LocationBuilder.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationBuilder = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)
inline fun <reified T : Any> LocationBuilder.ws(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: (WsLocationHandler<T>) -> Unit): LocationBuilder = location(T::class, handler, permittedRoles)

@PublishedApi
internal val HTTP_HANDLER_TYPES = HandlerType.values().filter { it.isHttpMethod() }.toTypedArray()

inline fun <reified T : Any> Javalin.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): Javalin = handle(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)
inline fun <reified T : Any> LocationGroup.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): LocationGroup = handle(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)
inline fun <reified T : Any> PathGroup.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): PathGroup = handle(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)

inline fun <reified T : Any> Javalin.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): Javalin {
    methods.forEach { httpMethod ->
        location(T::class, httpMethod, {
            handler(this, it, httpMethod)
        }, permittedRoles)
    }

    return this
}

inline fun <reified T : Any> LocationGroup.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): LocationGroup {
    methods.forEach { httpMethod ->
        location(T::class, httpMethod, {
            handler(this, it, httpMethod)
        }, permittedRoles)
    }

    return this
}


inline fun <reified T : Any> PathGroup.handle(vararg methods: HandlerType, permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): PathGroup {
    methods.forEach { httpMethod ->
        location(T::class, httpMethod, {
            handler(this, it, httpMethod)
        }, permittedRoles)
    }

    return this
}

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

private val BYTE_TYPE = Byte::class.createType()
private val SHORT_TYPE = Short::class.createType()
private val INT_TYPE = Int::class.createType()
private val DOUBLE_TYPE = Double::class.createType()
private val FLOAT_TYPE = Float::class.createType()
private val LONG_TYPE = Long::class.createType()
private val BOOLEAN_TYPE = Boolean::class.createType()
private val STRING_TYPE = String::class.createType()

private val NULLABLE_BYTE_TYPE = Byte::class.createType(nullable = true)
private val NULLABLE_SHORT_TYPE = Short::class.createType(nullable = true)
private val NULLABLE_INT_TYPE = Int::class.createType(nullable = true)
private val NULLABLE_DOUBLE_TYPE = Double::class.createType(nullable = true)
private val NULLABLE_FLOAT_TYPE = Float::class.createType(nullable = true)
private val NULLABLE_LONG_TYPE = Long::class.createType(nullable = true)
private val NULLABLE_BOOLEAN_TYPE = Boolean::class.createType(nullable = true)
private val NULLABLE_STRING_TYPE = String::class.createType(nullable = true)

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
internal fun <T : Any, R> LocationBuilder.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): LocationBuilder {
    when (this) {
        is LocationGroup -> this.location(location, handler, permittedRoles)
        is PathGroup -> this.location(location, handler, permittedRoles)
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
internal fun <T : Any, R> PathGroup.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): PathGroup {
    routeGroup.javalin.location(path, location, handler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any, R> LocationGroup.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> R, permittedRoles: Set<Role>): LocationGroup {
    javalin.location(location, method, handler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any, R> LocationGroup.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): LocationGroup {
    javalin.location(location, handler, permittedRoles)
    return this
}


@PublishedApi
internal fun <T : Any, R> Javalin.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> R, permittedRoles: Set<Role>): Javalin {
    val locationPath = locationPath(location)
    val locationHandler = locationHandler(location, handler)
    return addHandler(method, locationPath, locationHandler, permittedRoles)
}

@PublishedApi
internal fun <T : Any, R> Javalin.location(location: KClass<T>, handler: (WsLocationHandler<T>) -> R, permittedRoles: Set<Role>): Javalin {
    val locationPath = locationPath(location)
    return ws(locationPath, {
        val locationHandler = WsLocationHandler(location, it)
        handler.invoke(locationHandler)
    }, permittedRoles)
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

private fun <T : Any> locationPath(location: KClass<T>): String {
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

private fun <T : Any, R> locationHandler(location: KClass<T>, handler: T.(Context) -> R): Handler {
    return Handler { ctx ->
        when (val response: R = handler(ctx.hydrate(location), ctx)) {
            !is Unit -> ctx.result(response as Any)
        }
    }
}

private fun <T : Any> Any.hydrate(type: KClass<T>): T {
    val locationAnnotation = type.findAnnotation<Location>()

    val instance: T = when {
        this is Context && type.hasAnnotation<PostBody>() -> try {
            JavalinJson.fromJson(body(), type.java)
        } catch (e: Exception) {
            type.createInstance()
        }

        else -> type.createInstance()
    }

    if (this is Context && instance is ContextAware) {
        instance.context(this)
    }

    fun <V : Any> real(type: KType, value: Any): V? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is String -> {
                try {
                    when (type) {
                        BYTE_TYPE -> value.toByte()
                        SHORT_TYPE -> value.toShort()
                        INT_TYPE -> value.toInt()
                        DOUBLE_TYPE -> value.toDouble()
                        FLOAT_TYPE -> value.toFloat()
                        LONG_TYPE -> value.toLong()
                        BOOLEAN_TYPE -> when {
                            value.isEmpty() -> true
                            else -> value.toBoolean()
                        }

                        NULLABLE_BYTE_TYPE -> value.toByteOrNull()
                        NULLABLE_SHORT_TYPE -> value.toShortOrNull()
                        NULLABLE_INT_TYPE -> value.toIntOrNull()
                        NULLABLE_DOUBLE_TYPE -> value.toDoubleOrNull()
                        NULLABLE_FLOAT_TYPE -> value.toFloatOrNull()
                        NULLABLE_LONG_TYPE -> value.toLongOrNull()
                        NULLABLE_BOOLEAN_TYPE -> when {
                            value.isEmpty() -> true
                            else -> value.toBoolean()
                        }

                        STRING_TYPE, NULLABLE_STRING_TYPE -> value
                        else -> throw IllegalArgumentException("Unsupported type. [${type.classifier}]")
                    } as V
                } catch (nfe: NumberFormatException) {
                    return null
                }
            }

            is List<*> -> {
                when {
                    type.isSubtypeOf(List::class.starProjectedType) -> {
                        val firstType = type.arguments.first().type ?: throw IllegalStateException()
                        val realList: List<Any?> = value.map { real<V>(firstType, it as Any) }
                        realList.filterNotNull() as V
                    }

                    else -> real<V>(type, value.filterNotNull().first()) as V
                }
            }

            else -> value as V
        }
    }

    fun <V> getParameterValue(propertyName: String, parameterKey: String, map: Map<String, V>): V? {
        val targetParameter = parameterKey.takeIf { it.isNotBlank() } ?: propertyName
        return map[targetParameter]
    }

    fun <V : Any> setProperty(instance: T, property: KProperty1<T, V?>, value: Any) {
        val propertyType = property.returnType
        var real: V? = real(propertyType, value) ?: return

        @Suppress("UNCHECKED_CAST")
        if (propertyType.jvmErasure.java.isArray) {
            real = if (real is List<*>) {
                val array = java.lang.reflect.Array.newInstance((propertyType.arguments.first().type?.classifier as KClass<*>).java, real.size)
                real.forEachIndexed { idx, it -> java.lang.reflect.Array.set(array, idx, it) }
                array as V
            } else {
                val array = java.lang.reflect.Array.newInstance((propertyType.arguments.first().type?.classifier as KClass<*>).java, 1)
                java.lang.reflect.Array.set(array, 0, real)
                array as V
            }
        }

        when (property) {
            is KMutableProperty1 -> property.set(instance, real)
            else -> {
                val backingField = property.javaField ?: return
                backingField.isAccessible = true

                backingField.set(instance, real)
            }
        }
    }

    val pathParameters = when (this) {
        is Context -> pathParamMap()
        is WsContext -> pathParamMap()
        else -> emptyMap()
    }

    val queryParameters = when (this) {
        is Context -> queryParamMap()
        is WsContext -> queryParamMap()
        else -> emptyMap()
    }

    val formParameters = when (this) {
        is Context -> formParamMap()
        else -> emptyMap()
    }

    val allParameters = HashMap<String, Any>()
    val hydrationMethods = locationAnnotation?.allowedHydrationMethods
    hydrationMethods?.forEach {
        when (it) {
            HydrationMethod.POST_FORM_PARAMETERS -> allParameters.putAll(formParameters)
            HydrationMethod.QUERY_PARAMETERS -> allParameters.putAll(queryParameters)
            HydrationMethod.URL_PARAMETERS -> allParameters.putAll(pathParameters)
        }
    }

    type.declaredMemberProperties.forEach { property ->
        val propertyName = property.name
        val propertyType = property.returnType.classifier as KClass<*>

        if (propertyType.hasAnnotation<Location>()) {
            setProperty(instance, property, hydrate(propertyType))
            return@forEach
        }

        property.findAnnotation<UrlParameter>()
            ?.let { urlParameter ->
                val value = getParameterValue(propertyName, urlParameter.name, pathParameters)
                value?.let {
                    setProperty(instance, property, it)
                    return@forEach
                }
            }

        property.findAnnotation<QueryParameter>()
            ?.let { queryParameter ->
                val value = getParameterValue(propertyName, queryParameter.name, queryParameters)?.firstOrNull()
                value?.let {
                    setProperty(instance, property, it)
                    return@forEach
                }
            }

        property.findAnnotation<FormParameter>()
            ?.let { formParameter ->
                val value = getParameterValue(propertyName, formParameter.name, formParameters)?.firstOrNull()
                value?.let {
                    setProperty(instance, property, it)
                    return@forEach
                }
            }

        val value = allParameters[propertyName]
        value?.let {
            setProperty(instance, property, it)
            return@forEach
        }
    }

    return instance
}

internal inline fun <reified T : Annotation> KAnnotatedElement.hasAnnotation(): Boolean = findAnnotation<T>() != null
internal inline fun <reified T : Annotation> Class<*>.findAnnotation(): T? {
    return getDeclaredAnnotation(T::class.java)
}