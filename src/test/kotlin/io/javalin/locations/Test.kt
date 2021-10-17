package io.javalin.locations

import io.javalin.Javalin
import io.javalin.core.util.RouteOverviewPlugin
import java.time.Duration

private val SERVER_START_TIME_MS = System.currentTimeMillis()

fun main() {

    Javalin.create {
        it.registerPlugin(RouteOverviewPlugin("/routes"))
    }.path("/api/v1") {
        errorHandler { throwable, ctx ->
            when (throwable) {
                is IllegalArgumentException -> ctx.result("Invalid argument.").status(400)
                else -> ctx.result("Server Exception").status(500)
            }
        }

        configureAuthenticationAPI()
        configureServiceAPI()
    }.start(8080)

}

fun ILocationBuilder.configureServiceAPI() {
    head<ServiceAPI.Status> { ctx -> ctx.status(200) }
    head<ServiceAPI.Status.API> { it.status(200) }
    get<ServiceAPI.Uptime, ServiceAPI.Uptime.Response> {
        val uptimeMillis = System.currentTimeMillis() - SERVER_START_TIME_MS

        when {
            raw -> ServiceAPI.Uptime.Response(uptimeMillis)
            else -> {
                val duration = Duration.ofMillis(uptimeMillis)
                val uptime = "%02d:%02d:%02d.%03d".format(
                    duration.toHours().rem(24),
                    duration.toMinutes().rem(60),
                    duration.seconds.rem(60),
                    duration.nano.div(1000_000)
                )

                ServiceAPI.Uptime.Response(uptime)
            }
        }
    }
}

fun ILocationBuilder.configureAuthenticationAPI() {

    post<AuthenticationAPI.Login> { ctx ->
        when {
            username.isNullOrBlank() -> ctx.json(AuthenticationAPI.Login.Response("Invalid username."))
            password.isNullOrBlank() -> ctx.json(AuthenticationAPI.Login.Response("Invalid password."))
            else -> {
                // TODO authentication

                ctx.json(AuthenticationAPI.Login.Response("Authentication successful."))
            }
        }
    }
}

@Location("/service")
object ServiceAPI {

    @Location("/status")
    object Status {

        @Location("/api")
        object API

    }

    @Location("/uptime")
    class Uptime(@QueryParameter val raw: Boolean = false) {
        class Response(val uptime: Any)
    }
}

@Location("/auth")
object AuthenticationAPI {

    @PostBody
    @Location("/login")
    class Login(
        val username: String? = null,

        @IgnoreParameterType(QueryParameter::class)
        val password: String? = null
    ) {
        class Response(val message: String)
    }

}