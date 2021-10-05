# javalin-locations

### *Annotation library for routing in Javalin, loosely based on the Locations feature library for Ktor.*

### Targeting Javalin Version 4.x

```xml

<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>4.1.0</version>
</dependency>
```

*Currently the Javalin Locations library is not on Maven Central, please build and install locally.*

### Basics

#### Entry-Points

The main entry-point for the `javalin-locations` library is the `Javalin.locations(ILocationInit)`
or `Javalin.path(String, ILocationInit)` extension methods. From within here you will be able to use the common HTTP
methods as route handlers, such as `get` and `post` and others.

```kotlin
javalin.locations {
    // Routing here
}
```

#### HTTP method handlers

When using `javalin-locations` route handlers, you must define a class type that will be used for generating the
route's path and will be hydrated by the incoming request and passed to the handler as `this`. The defined class must
also be annotated with `@Location`.

```kotlin
@Location("/some/test/route/{param1}")
class TestRoute(val param1: String? = null)

javalin.locations {
    get<TestRoute> { ctx ->
        when {
            param1.isNullOrBlank() -> { // we have access to all the declared properties of TestRoute, currently scoped as `this`
                ctx.status(400).result("Invalid parameter")
            }
            
            else -> {
                // TODO something
            }
        }
    }
}
```

### Extended usage:

#### Sample Application

```kotlin
import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJackson
import java.time.Duration

private val SERVER_START_TIME_MS = System.currentTimeMillis()

fun main() {

    Javalin.create()
        .path("/api/v1") {
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

/*
 * Nested routes automatically prefix their enclosing classes, for example
 * the routes below would become:
 *    
 *    /service
 *    /service/status
 *    /sevice/uptime
 * 
 */

@Location("/service")
object ServiceAPI {

    @Location("/status")
    object Status

    @Location("/uptime")
    class Uptime(@QueryParameter val raw: Boolean = false) {
        class Response(val uptime: Any)
    }

}

@Location("/auth")
object AuthenticationAPI {

    @PostBody
    @Location("/login")
    class Login(val username: String? = null, val password: String? = null) {
        class Response(val message: String)
    }

}
```

#### Handle GET or POST in same handler

```kotlin
@Location("/image/{id}.png")
data class Image(val id: String? = null)

Javalin.create().locations {
    handle<Image>(HandlerType.GET, HandlerType.POST) { method, ctx ->
        ctx.result("You've reached here via the '$method' method.")
    }
}.start()
```

#### Wrap handlers in JetBrains Exposed DB transactions

```kotlin
@Location("/status")
object Status

@Location("/users")
object Users {

    @Location("/{id}")
    data class Single(val id: Int = -1)

}

Javalin.create()
    .locations {
        head<Status> { ctx ->
            ctx.status(200)
        }

        pathGroup {
            // all handlers in path group will use this when invoked
            handler { parent -> // be sure to call `handle(Context)` on parent
                Handler { ctx ->
                    transaction { // wrap whole handler in Exposed transaction
                        parent.handle(ctx)
                    }
                }
            }

            get<Users.Single> {
                when (id) {
                    -1 -> ctx.status(400).result("Invalid user identifier.")
                    else -> when (val targetUser =
                        UsersTable.findById(id)) { // this is safe to do because handler is wrapped in transaction now
                        null -> ctx.status(400).result("Invalid user identifier.")
                        else -> ctx.payload(targetUser) // serializing is also done inside transaction
                    }
                }
            }
        }
    }.start()
```

#### Handler's return value used as payload

```kotlin
class SampleRequest {
    val requestStartTime = System.currentTimeMillis()

    class SampleResponse(requestStartTime: Long) {
        val requestTimeMs = System.currentTimeMillis() - requestStartTime
    }
}

Javalin.create()
    .locations {
        get<SampleRequest, SampleRequest.SampleResponse> {
            Thread.sleep(1000)
            SampleResponse(requestStartTime)
        }
    }.start()
```

#### Accessing context from `Location` class
Sometimes you might want to write additional properties inside your `Location` classes that use information
from the current `Context`. You can gain access by extending the class `ContextAwareLocation`.

```kotlin
class SpecialRoute : ContextAwareLocation() {
    // context is available here as a protected property
    val authorizationToken =  context.header("Authorization")
}
```
