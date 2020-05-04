package com.mjrcompany.eventplannerservice

import arrow.core.Either
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.authorization.AuthorizationService
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.authorization.getAlgorithmFromJWK
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.authorization.makeJwtVerifier
import com.mjrcompany.eventplannerservice.com.mjrcompany.eventplannerservice.routes.*
import com.mjrcompany.eventplannerservice.core.getParamIdAsUUID
import com.mjrcompany.eventplannerservice.util.LocalDateAdapter
import com.mjrcompany.eventplannerservice.util.LocalDateTimeAdapter
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.utils.buildHeaders
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withTimeout
import org.slf4j.event.Level
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    cause.toString(),
                    cause.message
                )
            )
            throw cause
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Post)
        method(HttpMethod.Get)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        header("X-Id-Token")
        allowCredentials = false
        anyHost() // @TODO: change this
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }

    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(
                LocalDate::class.java,
                LocalDateAdapter
            )

            registerTypeAdapter(
                LocalDateTime::class.java,
                LocalDateTimeAdapter
            )
        }
    }

    if (!testing) {
        DatabaseSetup.initHikariDatasource()
        DatabaseSetup.initDevDb()
    }

    install(Authentication) {

        val config = environment.config
        val issuer = config.property("cognito.jwt-validation.issuer").getString()
        val kidAccessToken = config.property("cognito.jwt-validation.kidAccessToken").getString()
        val algorithm = getAlgorithmFromJWK(kidAccessToken)

        jwt {
            realm = "jwt realm"
            verifier(
                makeJwtVerifier(
                    issuer,
                    algorithm
                )
            )
            validate { credential -> JWTPrincipal(credential.payload) }
        }

    }

    install(Routing) {
        events()
        subjects()
        users()
        auth()
        signedUrl()
    }

}


@KtorExperimentalAPI
fun Route.withIdToken(callback: Route.() -> Unit): Route {

    val routeWithIdToken = this.createChild(object : RouteSelector(1.0) {
        override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })

    routeWithIdToken.intercept(ApplicationCallPipeline.Features) {
        val headers = call.request.headers
        val idToken = headers["X-Id-Token"] ?: " "

        when (val result = AuthorizationService(application).getIdTokenPayload(idToken)) {
            is Either.Left -> call.respond(HttpStatusCode.Unauthorized, result.a.errorResponse)
            is Either.Right -> call.attributes.put(UserEmailAttributeKey, result.b.email)
        }

    }
    callback(routeWithIdToken)

    return routeWithIdToken
}


@KtorExperimentalAPI
fun Route.withHostRequestPermission(callback: Route.() -> Unit): Route {

    val routeHostRequestPermission = this.createChild(object : RouteSelector(1.0) {
        override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })

    routeHostRequestPermission.intercept(ApplicationCallPipeline.Features) {
        val headers = call.request.headers
        val idToken = headers["X-Id-Token"] ?: " "
        val id = call.getParamIdAsUUID()

        val permission = AuthorizationService(application).checkHostPermission(id, idToken)
        if (permission is Either.Left) {
            call.respond(HttpStatusCode.Unauthorized, permission.a.errorResponse)
            return@intercept finish()
        }

    }
    callback(routeHostRequestPermission)

    return routeHostRequestPermission
}

@KtorExperimentalAPI
fun Route.withFriendInEventRequestPermission(callback: Route.() -> Unit): Route {

    val routeFriendInEventRequestPermission = this.createChild(object : RouteSelector(1.0) {
        override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })

    routeFriendInEventRequestPermission.intercept(ApplicationCallPipeline.Features) {
        val headers = call.request.headers
        val idToken = headers["X-Id-Token"] ?: " "
        val id = call.getParamIdAsUUID()

        val permission = AuthorizationService(application).checkFriendPermissionToAccessMeeting(id, idToken)
        if (permission is Either.Left) {
            call.respond(HttpStatusCode.Unauthorized, permission.a.errorResponse)
            return@intercept finish()
        }

    }
    callback(routeFriendInEventRequestPermission)

    return routeFriendInEventRequestPermission
}

val UserEmailAttributeKey = AttributeKey<String>("UserEmail")