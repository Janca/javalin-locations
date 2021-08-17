# javalin-locations

### *Annotation library for routing in Javalin, loosely based on the Locations feature library for Ktor.*

#### Example usage:

```kotlin
import io.javalin.Javalin
import java.time.Duration

private val SERVER_START_TIME_MS = System.currentTimeMillis()

fun main() {

    Javalin.create().locations {

        path("/api/v1") {
            configureAuthenticationAPI()
            configureServiceAPI()
        }

    }.start(8080)

}

fun LocationBuilder.configureServiceAPI() {
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

fun LocationBuilder.configureAuthenticationAPI() {
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
sealed class ServiceAPI {

    @Location("/status")
    class Status

    @Location("/uptime", [HydrationMethod.QUERY_PARAMETERS])
    class Uptime(@QueryParameter val raw: Boolean = false) {
        class Response(val uptime: Any)
    }

}

@Location("/auth")
sealed class AuthenticationAPI {

    @PostBody
    @Location("/login")
    class Login(val username: String? = null, val password: String? = null) {
        class Response(val message: String)
    }

}
```