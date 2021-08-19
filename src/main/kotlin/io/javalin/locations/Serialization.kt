@file:Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")

package io.javalin.locations

import com.fasterxml.jackson.databind.SerializationFeature
import io.javalin.http.Context
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsContext
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

private val OBJECT_MAPPER = JavalinJackson.getObjectMapper()
    .copy().apply {
        setConfig(serializationConfig.with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
    }

fun <T : Any> Any.hydrate(location: KClass<T>): T {
    if (this !is Context && this !is WsContext) {
        throw IllegalStateException("Must call hydrate on Context or WsContext instance.")
    }

    val objectInst = location.objectInstance
    if (objectInst != null) {
        return objectInst
    }

    val locationAnnotation = location.findAnnotation<Location>()
        ?: throw IllegalArgumentException("Parameter 'location' must be a class annotated with the Location annotation.")

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

    val hydrates = HashMap<Any, Any?>()
    val locationProperties = location.declaredMemberProperties
    locationProperties.forEach { property ->
        val name = property.name

        property.findAnnotation<FormParameter>()?.let { annot ->
            val targetFormKey = annot.name.takeUnless { it.isBlank() } ?: name
            formParameters[targetFormKey]?.let {
                hydrates[name] = it
            }
        }

        property.findAnnotation<QueryParameter>()?.let { annot ->
            val targetQueryKey = annot.name.takeUnless { it.isBlank() } ?: name
            queryParameters[targetQueryKey]?.let {
                hydrates[name] = it
            }
        }

        property.findAnnotation<UrlParameter>()?.let { annot ->
            val targetPathKey = annot.name.takeUnless { it.isBlank() } ?: name
            pathParameters[targetPathKey]?.let {
                hydrates[name] = it
            }
        }

        formParameters[name]
            ?.takeIf { locationAnnotation.isHydratingFormParameters && !hydrates.containsKey(name) }
            ?.let {
                hydrates[name] = it
            }

        queryParameters[name]
            ?.takeIf { locationAnnotation.isHydratingQueryParameters && !hydrates.containsKey(name) }
            ?.let {
                hydrates[name] = it
            }

        pathParameters[name]
            ?.takeIf { locationAnnotation.isHydratingUrlParameters && !hydrates.containsKey(name) }
            ?.let {
                hydrates[name] = it
            }

    }

    return when (this) {
        is Context -> this.createInstance(hydrates, location)
        is WsContext -> this.createInstance(hydrates, location)
        else -> throw IllegalArgumentException("Must call hydrate on either Context or WsContext instance. [${this.javaClass.name}]")
    }
}

fun <T : Any> Context.createInstance(hydrates: MutableMap<Any, Any?>, location: KClass<T>): T {
    if (location.hasAnnotation<PostBody>()) {
        val jsonBody = try {
            OBJECT_MAPPER.readValue(body(), Map::class.java)
        } catch (ignore: Exception) {
            null
        }

        jsonBody?.forEach {
            val key = it.key ?: return@forEach
            hydrates[key] = it.value
        }
    }

    val json = OBJECT_MAPPER.writeValueAsString(hydrates)
    val instance = JavalinJson.fromJson(json, location.java)

    return instance.also {
        if (it is ContextAware) {
            it.context(this)
        }
    }
}

fun <T : Any> WsContext.createInstance(hydrates: MutableMap<Any, Any?>, location: KClass<T>): T {
    val json = OBJECT_MAPPER.writeValueAsString(hydrates)
    return JavalinJson.fromJson(json, location.java).also {
        if (it is WsContextAware) {
            it.context(this)
        }
    }
}

internal val Location.isHydratingFormParameters: Boolean get() = allowedHydrationMethods.contains(HydrationMethod.POST_FORM_PARAMETERS)
internal val Location.isHydratingQueryParameters: Boolean get() = allowedHydrationMethods.contains(HydrationMethod.QUERY_PARAMETERS)
internal val Location.isHydratingUrlParameters: Boolean get() = allowedHydrationMethods.contains(HydrationMethod.URL_PARAMETERS)