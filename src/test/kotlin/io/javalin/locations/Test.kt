package io.javalin.locations

import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJackson
import java.time.Duration

private val SERVER_START_TIME_MS = System.currentTimeMillis()

fun main() {

    Javalin.create().locations {
        path("/api/v1") {
            configureAuthenticationAPI()
            configureServiceAPI()
        }

        get<ArraySerialization.TestA> {
            it.payload(ids)
        }
    }.start(8080)

}

fun ILocationBuilder.configureServiceAPI() {
    head<ServiceAPI.Status> { ctx -> ctx.status(200) }
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
    jsonMapper {
        // You can define json mappers per path group,
        // or globally on the Javalin#location(ILocationBuilder.()->Unit) entry point
        // if none are set, defaults to Javalin.jsonMapper()

        JavalinJackson()
    }

    post<AuthenticationAPI.Login> { ctx ->
        when {
            username.isNullOrBlank() -> ctx.json(AuthenticationAPI.Login.Response("Invalid username."))
            password.isNullOrBlank() -> ctx.json(AuthenticationAPI.Login.Response("Invalid password."))
            else -> {
                // TODO authentication

                // use Context#payload(Any) to use the JsonMapper assigned to the location
                ctx.payload(AuthenticationAPI.Login.Response("Authentication successful."))
            }
        }
    }
}

@Location("/service")
object ServiceAPI {

    @Location("/status")
    object Status

    @Location("/uptime")
    class Uptime(@QueryParameter val raw: Boolean = false) {
        class Response(val uptime: Any)
    }

}

@Location("/arrays")
object ArraySerialization {

    @Location("/testa")
    class TestA(val ids: IntArray = intArrayOf())

}

@Location("/auth")
object AuthenticationAPI {

    @PostBody
    @Location("/login")
    class Login(val username: String? = null, val password: String? = null) {
        class Response(val message: String)
    }

}