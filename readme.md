# javalin-locations

### *Annotation library for routing in Javalin, loosely based on the Locations feature library for Ktor.*

```
fun main() {

    val app = Javalin.create()

    @Location("/ping")
    data class Ping(val token: String? = null)

    app.locations {

        get<Ping> { context ->
            val pong = buildString {
                append("Pong")

                when {
                    token.isNullOrBlank() -> return@buildString
                    else -> append(": $token")
                }
            }

            context.result(pong)
        }

        configureAuthenticationRouting()
        configureUserRouting()

    }

    app.start(8080)

}

/*
 * Split routing into files or functions.
 */

fun LocationBuilder.configureAuthenticationRouting(): LocationBuilder {

    @PostBody // Hydrates data class by parsing the request body as JSON object.
    @Location("/authenticate")
    data class Authenticate(val username: String? = null, val password: String? = null)

    post<Authenticate> { context ->
        when { //access to Authenticate class properties and methods
            username.isNullOrBlank() -> context.status(400).result("Invalid username.")
            password.isNullOrBlank() -> context.status(400).result("Invalid password.")
            else -> {
                // TODO check authentication

                context.result("Authentication successful.")
            }
        }
    }

    return this
}

fun LocationBuilder.configureUserRouting(): LocationBuilder {

    @Location("/user/:userId")
    data class UserProfile(val userId: Int = -1)
    data class UserProfileResponse(val userId: Int, val username: String)

    get<UserProfile> { context ->
        when (userId) {
            -1 -> context.status(400).result("Invalid user.")
            else -> {
                // TODO lookup user information

                context.result(UserProfileResponse(userId, "testuser")) //respond with JSON object, could be ORM class, etc
            }
        }
    }

    return this
}
```