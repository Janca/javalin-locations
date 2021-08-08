package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.plugin.json.JavalinJson
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Location(val path: String, val promiscuousHydrator: Boolean = true)

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

// PRIMARY ENTRY-POINT
inline fun Javalin.locations(init: LocationGroup.() -> Unit): Javalin {
    init(LocationGroup(this))
    return this
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

inline fun <reified T : Any> LocationGroup.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any> LocationGroup.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): LocationGroup = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)

inline fun <reified T : Any> PathGroup.get(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.GET, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.post(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.POST, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.put(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.PUT, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.patch(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.PATCH, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.delete(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.DELETE, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.head(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.HEAD, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.trace(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.TRACE, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.connect(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.CONNECT, handler, permittedRoles)
inline fun <reified T : Any> PathGroup.options(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(Context) -> Unit): PathGroup = location(T::class, HandlerType.OPTIONS, handler, permittedRoles)

@PublishedApi
internal val HTTP_HANDLER_TYPES = HandlerType.values().filter { it.isHttpMethod() }.toTypedArray()

inline fun <reified T : Any> Javalin.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): Javalin = handle<T>(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)
inline fun <reified T : Any> LocationGroup.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): LocationGroup = handle<T>(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)
inline fun <reified T : Any> PathGroup.handle(permittedRoles: Set<Role> = EMPTY_ROLE_SET, noinline handler: T.(ctx: Context, httpMethod: HandlerType) -> Unit): PathGroup = handle<T>(permittedRoles = permittedRoles, handler = handler, methods = *HTTP_HANDLER_TYPES)

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

interface LocationApiBuilder {
    fun path(path: String, init: PathGroup.() -> Unit): LocationApiBuilder
}

class PathGroup internal constructor(internal val routeGroup: LocationGroup, path: String) : LocationApiBuilder {
    internal val path = normalizePath(path)

    override fun path(path: String, init: PathGroup.() -> Unit): LocationApiBuilder {
        val routePath = this.path + normalizePath(path)
        init(PathGroup(routeGroup, routePath))
        return routeGroup
    }
}

class LocationGroup @PublishedApi internal constructor(internal val javalin: Javalin) : LocationApiBuilder {

    override fun path(path: String, init: PathGroup.() -> Unit): LocationApiBuilder {
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
internal fun <T : Any> PathGroup.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> Unit, permittedRoles: Set<Role>): PathGroup {
    val locationPath = locationPath(location)
    val locationHandler = locationHandler(location, handler)
    routeGroup.javalin.addHandler(method, "$path$locationPath", locationHandler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any> LocationGroup.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> Unit, permittedRoles: Set<Role>): LocationGroup {
    javalin.location(location, method, handler, permittedRoles)
    return this
}

@PublishedApi
internal fun <T : Any> Javalin.location(location: KClass<T>, method: HandlerType, handler: T.(Context) -> Unit, permittedRoles: Set<Role>): Javalin {
    val locationPath = locationPath(location)
    val locationHandler = locationHandler(location, handler)

    return addHandler(method, locationPath, locationHandler, permittedRoles)
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
    val locationClassMembers = location.declaredMemberProperties
    val nestedLocationMembers = locationClassMembers.filter { (it.returnType.classifier as? KClass<*>)?.hasAnnotation<Location>() == true }

    val parentPath: String = nestedLocationMembers
        .joinToString("/") {
            (it.returnType.classifier as? KClass<*>)?.findAnnotation<Location>()?.path ?: throw IllegalStateException()
        }

    val locationAnnotation = location.findAnnotation<Location>()
    val locationPath = locationAnnotation?.path
        ?: throw IllegalArgumentException("Location '${location.qualifiedName}' is missing annotation 'Location'.")

    val path = parentPath + locationPath
    return normalizePath(path)
}

private fun <T : Any> locationHandler(location: KClass<T>, handler: T.(Context) -> Unit): Handler {
    fun <U : Any> Context.hydrate(type: KClass<U>): U {
        val locationAnnotation = type.findAnnotation<Location>()

        val instance: U = when {
            type.hasAnnotation<PostBody>() -> try {
                JavalinJson.fromJson(body(), type.java)
            } catch (e: Exception) {
                type.createInstance()
            }

            else -> type.createInstance()
        }

        if (instance is ContextAware) {
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
                            BOOLEAN_TYPE -> value.toBoolean()

                            NULLABLE_BYTE_TYPE -> value.toByteOrNull()
                            NULLABLE_SHORT_TYPE -> value.toShortOrNull()
                            NULLABLE_INT_TYPE -> value.toIntOrNull()
                            NULLABLE_DOUBLE_TYPE -> value.toDoubleOrNull()
                            NULLABLE_FLOAT_TYPE -> value.toFloatOrNull()
                            NULLABLE_LONG_TYPE -> value.toLongOrNull()
                            NULLABLE_BOOLEAN_TYPE -> value.toBoolean()

                            STRING_TYPE, NULLABLE_STRING_TYPE -> value
                            else -> throw IllegalArgumentException("Unsupported type. [${type.classifier}]")
                        } as V
                    } catch (nfe: NumberFormatException) {
                        return null
                    }
                }

                is List<*> -> {
                    val firstType = type.arguments.first().type ?: throw IllegalStateException()
                    val realList: List<Any?> = value.map { real<V>(firstType, it as Any) }
                    realList.filterNotNull() as V
                }

                else -> value as V
            }
        }

        fun <V> getParameterValue(propertyName: String, parameterKey: String, map: Map<String, V>): V? {
            val targetParameter = parameterKey.takeIf { it.isNotBlank() } ?: propertyName
            return map[targetParameter]
        }

        fun <V : Any> setProperty(instance: U, property: KProperty1<U, V?>, value: Any) {
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

        val pathParameters = pathParamMap()
        val queryParameters = queryParamMap()
        val formParameters = formParamMap()

        val allParameters: Map<String, Any> = when (locationAnnotation?.promiscuousHydrator) {
            true -> HashMap<String, Any>()
                .apply {
                    putAll(pathParameters)
                    putAll(queryParameters)
                    putAll(formParameters)
                }

            else -> emptyMap()
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

            locationAnnotation?.promiscuousHydrator
                ?.let { hydrateAll ->
                    if (hydrateAll) {
                        val value = allParameters[propertyName]
                        value?.let {
                            setProperty(instance, property, it)
                            return@forEach
                        }
                    }
                }
        }

        return instance
    }

    return Handler { ctx -> handler(ctx.hydrate(location), ctx) }
}

internal inline fun <reified T : Annotation> KAnnotatedElement.hasAnnotation(): Boolean = findAnnotation<T>() != null