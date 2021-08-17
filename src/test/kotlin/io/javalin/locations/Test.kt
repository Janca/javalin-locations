package io.javalin.locations

import io.javalin.Javalin

@Location("/test")
data class Test(val token: String? = null) {

    @Location("/:name")
    data class Edit(val name: String? = null)

}

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

        post<Test.Edit> { ctx ->
            ctx.status(200)
        }

        configureAuthenticationRouting()
        configureUserRouting()

    }

    app.start(8080)

}

fun LocationBuilder.configureAuthenticationRouting(): LocationBuilder {

    //@PostBody // Hydrates data class by parsing the request body as JSON object.
    @Location("/authenticate")
    class Authenticate(val username: String? = null, val password: String? = null)

    post<Authenticate> { context ->
        println(context.formParamMap().entries.joinToString { "${it.key} : ${it.value.joinToString()}" })

        when { //access to Authenticate class properties and methods
            username.isNullOrEmpty() -> context.status(400).result("Invalid username.")
            password.isNullOrEmpty() -> context.status(400).result("Invalid password.")
            else -> {
                // TODO check authentication

                context.result("Authentication successful. ${username}:${password}")
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