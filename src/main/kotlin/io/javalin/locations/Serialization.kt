@file:Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")

package io.javalin.locations

import io.javalin.http.Context
import io.javalin.websocket.WsContext
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName
import kotlin.streams.toList

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

private val BYTE_ARRAY_TYPE = ByteArray::class.createType()
private val SHORT_ARRAY_TYPE = ShortArray::class.createType()
private val INT_ARRAY_TYPE = IntArray::class.createType()
private val DOUBLE_ARRAY_TYPE = DoubleArray::class.createType()
private val FLOAT_ARRAY_TYPE = FloatArray::class.createType()
private val LONG_ARRAY_TYPE = LongArray::class.createType()
private val BOOLEAN_ARRAY_TYPE = BooleanArray::class.createType()

private data class LocationSerializationCache(
    val allProperties: Map<out String, KProperty1<Any, Any>>,
    val formProperties: Map<out String, KProperty1<Any, Any>>,
    val queryProperties: Map<out String, KProperty1<Any, Any>>,
    val urlProperties: Map<out String, KProperty1<Any, Any>>
)

private val LOCATION_SERIALIZATION_CACHE = HashMap<KClass<*>, LocationSerializationCache>()

fun <T : Any> Any.hydrate(location: KClass<T>): T {
    val locationAnnotation = location.findAnnotation<Location>()
        ?: throw IllegalArgumentException("Parameter 'location' must be a class annotated with the Location annotation.")

    val locationInstance: T = when (this) {
        is Context -> this.createInstance(location)
        is WsContext -> this.createInstance(location)
        else -> throw IllegalArgumentException("Must call hydrate on either Context or WsContext instance. [${this::class.jvmName}]")
    }

    val (locationProperties, locationFormProperties, locationQueryProperties, locationUrlProperties) = LOCATION_SERIALIZATION_CACHE.computeIfAbsent(location) { locationKey ->
        val memberProperties = locationKey.declaredMemberProperties

        val allProperties = HashMap<String, KProperty1<Any, Any>>()
        val formProperties = HashMap<String, KProperty1<Any, Any>>()
        val queryProperties = HashMap<String, KProperty1<Any, Any>>()
        val urlProperties = HashMap<String, KProperty1<Any, Any>>()

        memberProperties.forEach { prop ->
            allProperties[prop.name] = prop as KProperty1<Any, Any>

            val formParameterAnnotation = prop.findAnnotation<FormParameter>()
            formParameterAnnotation?.let {
                formProperties[it.name.takeIf { annotName -> annotName.isNotBlank() } ?: prop.name] = prop
                return@forEach
            }

            val queryParameterAnnotation = prop.findAnnotation<QueryParameter>()
            queryParameterAnnotation?.let {
                queryProperties[it.name.takeIf { annotName -> annotName.isNotBlank() } ?: prop.name] = prop
                return@forEach
            }

            val urlParameterAnnotation = prop.findAnnotation<UrlParameter>()
            urlParameterAnnotation?.let {
                urlProperties[it.name.takeIf { annotName -> annotName.isNotBlank() } ?: prop.name] = prop
                return@forEach
            }
        }

        LocationSerializationCache(allProperties, formProperties, queryProperties, urlProperties)
    }


    val formParameters = when (this) {
        is Context -> formParamMap()
        else -> emptyMap()
    }

    val queryParameters = when (this) {
        is Context -> queryParamMap()
        is WsContext -> queryParamMap()
        else -> emptyMap()
    }

    val pathParameters = when (this) {
        is Context -> pathParamMap()
        is WsContext -> pathParamMap()
        else -> emptyMap()
    }

    val allParameters = HashMap<String, Any>().apply {
        val hydrationMethods = locationAnnotation.allowedHydrationMethods
        hydrationMethods.forEach {
            when (it) {
                HydrationMethod.POST_FORM_PARAMETERS -> putAll(formParameters)
                HydrationMethod.QUERY_PARAMETERS -> putAll(queryParameters)
                HydrationMethod.URL_PARAMETERS -> putAll(pathParameters)
            }
        }
    }

    formParameters.forEach { (key, value) ->
        val locationFormProperty = locationFormProperties[key] ?: return@forEach
        setProperty(locationFormProperty, locationInstance, value)
    }

    queryParameters.forEach { (key, value) ->
        val queryProperty = locationQueryProperties[key] ?: return@forEach
        setProperty(queryProperty, locationInstance, value)
    }

    pathParameters.forEach { (key, value) ->
        val pathProperty = locationUrlProperties[key] ?: return@forEach
        setProperty(pathProperty, locationInstance, value)
    }

    allParameters.forEach { (key, value) ->
        val property = locationProperties[key] ?: return@forEach
        setProperty(property, locationInstance, value)
    }

    return locationInstance
}

private fun <T : Any> Context.createInstance(location: KClass<T>): T {
    return when {
        location.hasAnnotation<PostBody>() -> try {
            bodyAsClass(location.java)
        } catch (e: Exception) {
            location.createInstance()
        }

        else -> location.createInstance()
    }.also {
        if (it is ContextAware) {
            it.context(this)
        }
    }
}

private fun <T : Any> WsContext.createInstance(location: KClass<T>): T {
    return location.createInstance()
        .also {
            if (it is WsContextAware) {
                it.context(this)
            }
        }
}

fun <V : Any> setProperty(property: KProperty1<Any, V>, instance: Any, value: Any) {
    val type = property.returnType
    val hydrated: V = value.cast(type) ?: return

    when (property) {
        is KMutableProperty1<Any, V> -> property.set(instance, hydrated)
        else -> {
            val backingField = property.javaField ?: return
            backingField.isAccessible = true
            backingField.set(instance, hydrated)
        }
    }
}

private fun <V : Any> Any.cast(type: KType): V? {
    return when (this) {
        is String -> this.cast(type) as V?
        is List<*> -> this.cast(type) as V?
        else -> throw IllegalArgumentException()
    }
}

private fun <V : Any> String.cast(type: KType): V? {
    return when (type) {
        STRING_TYPE, NULLABLE_STRING_TYPE -> this

        BYTE_TYPE -> this.toByte()
        SHORT_TYPE -> this.toShort()
        INT_TYPE -> this.toInt()
        DOUBLE_TYPE -> this.toDouble()
        FLOAT_TYPE -> this.toFloat()
        LONG_TYPE -> this.toLong()

        BOOLEAN_TYPE, NULLABLE_BOOLEAN_TYPE -> when {
            this.isEmpty() -> true
            else -> this.toBoolean()
        }

        NULLABLE_BYTE_TYPE -> this.toByteOrNull()
        NULLABLE_SHORT_TYPE -> this.toShortOrNull()
        NULLABLE_INT_TYPE -> this.toIntOrNull()
        NULLABLE_DOUBLE_TYPE -> this.toDoubleOrNull()
        NULLABLE_FLOAT_TYPE -> this.toFloatOrNull()
        NULLABLE_LONG_TYPE -> this.toLongOrNull()

        else -> when {
            type.jvmErasure.java.isArray -> this.castArray(type)
            else -> throw IllegalArgumentException("Unsupported type. [${type.classifier}]")
        }
    } as V?
}

private fun <V : Any> List<*>.cast(type: KType): V? {
    val firstType = type.arguments.firstOrNull()?.type ?: type ?: throw IllegalStateException()
    val casts = this.stream().map { it?.cast<V>(firstType) }

    return when {
        type.jvmErasure.isSubclassOf(List::class) -> casts.toList() as V
        type.jvmErasure.java.isArray -> casts.castArray(type) as V
        else -> casts.limit(1).findFirst().orElse(null)
    }
}

private fun Stream<*>.castArray(type: KType): Any {
    val values = toArray()
    val length = values.size

    fun kotlinArray(): Any {
        return when (type) {
            BYTE_ARRAY_TYPE -> ByteArray(length) { values[it] as Byte }
            SHORT_ARRAY_TYPE -> ShortArray(length) { values[it] as Short }
            INT_ARRAY_TYPE -> IntArray(length) { values[it] as Int }
            DOUBLE_ARRAY_TYPE -> DoubleArray(length) { values[it] as Double }
            FLOAT_ARRAY_TYPE -> FloatArray(length) { values[it] as Float }
            LONG_ARRAY_TYPE -> LongArray(length) { values[it] as Long }
            BOOLEAN_ARRAY_TYPE -> BooleanArray(length) { values[it] as Boolean }
            else -> throw IllegalArgumentException("Unsupported array type. [${type.classifier}]")
        }
    }

    val typeArguments = type.arguments
    return when {
        typeArguments.isNotEmpty() -> {
            val arrayType = type.arguments.first().type!!.classifier as KClass<*>
            val typedArray = java.lang.reflect.Array.newInstance(arrayType.java, length)
            java.lang.reflect.Array.set(typedArray, 0, values)
            typedArray
        }

        else -> kotlinArray()
    }
}

private fun String.castArray(type: KType): Any {
    fun String.kotlinArray(): Any {
        val arrayType = type.jvmErasure.java
        val castType = arrayType.componentType.kotlin.createType()

        return when (type) {
            BYTE_ARRAY_TYPE -> ByteArray(1) { this.cast(castType)!! }
            SHORT_ARRAY_TYPE -> ShortArray(1) { this.cast(castType)!! }
            INT_ARRAY_TYPE -> IntArray(1) { this.cast(castType)!! }
            DOUBLE_ARRAY_TYPE -> DoubleArray(1) { this.cast(castType)!! }
            FLOAT_ARRAY_TYPE -> FloatArray(1) { this.cast(castType)!! }
            LONG_ARRAY_TYPE -> LongArray(1) { this.cast(castType)!! }
            BOOLEAN_ARRAY_TYPE -> BooleanArray(1) { this.cast(castType)!! }
            else -> throw IllegalArgumentException("Unsupported array type. [${type.classifier}]")
        }
    }

    val typeArguments = type.arguments
    return when {
        typeArguments.isNotEmpty() -> {
            val arrayType = type.arguments.first().type!!.classifier as KClass<*>
            val typedArray = java.lang.reflect.Array.newInstance(arrayType.java, 1)
            java.lang.reflect.Array.set(typedArray, 0, this.cast(arrayType.java.kotlin.createType()))
            typedArray
        }

        else -> this.kotlinArray()
    }
}