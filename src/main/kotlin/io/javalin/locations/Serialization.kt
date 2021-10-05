@file:Suppress("UNCHECKED_CAST")

package io.javalin.locations

import io.javalin.http.Context
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

internal fun <T : Any> Context.hydrate(location: KClass<T>, builder: LocationBuilder): T {
    val objectInst = location.objectInstance
    if (objectInst != null) {
        return objectInst
    }

    val formParameters = formParamMap()
    val queryParameters = queryParamMap()

    val pathParameters = pathParamMap()

    val allParameters = HashMap<String, Any>()
        .apply {
            putAll(formParameters)
            putAll(queryParameters)
            putAll(pathParameters)
        }

    val locationAnnotation = LocationBuilder.locationAnnotation(location)
        ?: throw IllegalStateException("Parameter 'location' must be a class annotated with the Location annotation.")

    val locationInstance = createInstance(location, builder)
    val locationProperties: Collection<KProperty1<Any, Any>> =
        location.declaredMemberProperties as Collection<KProperty1<Any, Any>>

    locationProperties.forEach { property ->
        var hydrated = false
        val propertyName = property.name

        property.findAnnotation<QueryParameter>()?.let { annot ->
            val hydrateKey = annot.name.takeIf { it.isNotBlank() } ?: propertyName
            queryParameters[hydrateKey]?.let {
                setProperty(property, locationInstance, it)
                hydrated = true
            }
        }

        property.findAnnotation<FormParameter>()?.let { annot ->
            val hydrateKey = annot.name.takeIf { it.isNotBlank() } ?: propertyName
            formParameters[hydrateKey]?.let {
                setProperty(property, locationInstance, it)
                hydrated = true
            }
        }

        property.findAnnotation<PathParameter>()?.let { annot ->
            val hydrateKey = annot.name.takeIf { it.isNotBlank() } ?: propertyName
            pathParameters[hydrateKey]?.let {
                setProperty(property, locationInstance, it)
                hydrated = true
            }
        }

        property.findAnnotation<PostParameter>()?.let { annot ->
            try {
                val body = body()
                when {
                    body.isNotBlank() -> {
                        val type = ((property.returnType.classifier!!) as KClass<*>).java
                        val inst = builder.jsonMapper().fromJsonString(body, type)
                        setProperty(property, locationInstance, inst, false)
                        hydrated = true
                    }
                }
            } catch (e: Exception) {
                // NOP
            }
        }

        if (!hydrated && locationAnnotation.eagerHydration) {
            allParameters[propertyName]?.let {
                setProperty(property, locationInstance, it)
            }
        }
    }

    return locationInstance
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

private val BYTE_ARRAY_TYPE = ByteArray::class.createType()
private val SHORT_ARRAY_TYPE = ShortArray::class.createType()
private val INT_ARRAY_TYPE = IntArray::class.createType()
private val DOUBLE_ARRAY_TYPE = DoubleArray::class.createType()
private val FLOAT_ARRAY_TYPE = FloatArray::class.createType()
private val LONG_ARRAY_TYPE = LongArray::class.createType()
private val BOOLEAN_ARRAY_TYPE = BooleanArray::class.createType()

private fun <T : Any> Context.createInstance(location: KClass<T>, builder: LocationBuilder): T {
    val locationAnnotation = LocationBuilder.locationAnnotation(location)
        ?: throw IllegalStateException("Parameter 'location' must be a class annotated with the Location annotation.")

    return when {
        locationAnnotation.eagerHydration -> try {
            builder.jsonMapper().fromJsonString(body(), location.java)
        } catch (e: Exception) {
            location.createInstance()
        }

        else -> location.createInstance()
    }.also {
        if (it is ContextAwareLocation) {
            it.context = this
        }
    }
}

private fun <V : Any> setProperty(property: KProperty1<Any, V>, instance: Any, value: Any, cast: Boolean = true) {
    try {
        val type = property.returnType
        val hydrated: V = when (cast) {
            true -> value.cast(type) ?: return
            else -> value as V
        }

        when (property) {
            is KMutableProperty1<Any, V> -> property.set(instance, hydrated)
            else -> {
                val backingField = property.javaField ?: return
                backingField.isAccessible = true
                backingField.set(instance, hydrated)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun <V : Any> Any.cast(type: KType, debug: Boolean = false): V? {
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
            else -> when (this.toIntOrNull()) {
                null -> this.toBoolean()
                1 -> true
                else -> false
            }
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
    val casts = this.stream()

    return when {
        type.jvmErasure.isSubclassOf(List::class) -> casts.toList() as V
        type.jvmErasure.java.isArray -> casts.castArray(type) as V
        else -> casts.map { it?.cast<V>(firstType) }.limit(1).findFirst().orElse(null)
    }
}

private fun Stream<*>.castArray(type: KType): Any {
    val values = toArray()
    val length = values.size

    fun kotlinArray(): Any {
        return when (type) {
            BYTE_ARRAY_TYPE -> ByteArray(length) { values[it].cast(BYTE_TYPE)!! }
            SHORT_ARRAY_TYPE -> ShortArray(length) { values[it].cast(SHORT_TYPE)!! }
            INT_ARRAY_TYPE -> IntArray(length) { values[it].cast(INT_TYPE)!! }

            DOUBLE_ARRAY_TYPE -> DoubleArray(length) { values[it].cast(DOUBLE_TYPE)!! }
            FLOAT_ARRAY_TYPE -> FloatArray(length) { values[it].cast(FLOAT_TYPE)!! }
            LONG_ARRAY_TYPE -> LongArray(length) { values[it].cast(LONG_TYPE)!! }
            BOOLEAN_ARRAY_TYPE -> BooleanArray(length) { values[it].cast(BOOLEAN_TYPE)!! }
            else -> throw IllegalArgumentException("Unsupported array type. [${type.classifier}]")
        }
    }

    val typeArguments = type.arguments
    return when {
        typeArguments.isNotEmpty() -> values.cast(type)
        else -> kotlinArray()
    }
}

private fun Array<*>.cast(type: KType): Any {
    val arrayType = type.arguments.first().type!!.classifier as KClass<*>
    val typedArray: Array<Any> = java.lang.reflect.Array.newInstance(arrayType.java.kotlin.java, size) as Array<Any>
    for (i in 0..lastIndex) {
        typedArray[i] = this[i]?.cast(arrayType.createType())!!
    }

    return typedArray
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