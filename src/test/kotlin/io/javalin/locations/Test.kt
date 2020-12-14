package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.util.RouteOverviewUtil.metaInfo
import io.javalin.http.HandlerType

@PostBody
@Location("/shorten")
class ShortenLink(val links: Array<String> = emptyArray())

fun main() {

    val app = Javalin.create()

    app.locations {
        handle<ShortenLink> { ctx, httpMethod ->

        }
    }

    app.start(8080)

}