/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

package model.authentication

import API_SCHEME_AUTHORITY
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import model.Utils
import kotlin.system.exitProcess

data class SessionData(var token: String = "")

/**
 * Opens provider's authentication web page
 */
fun openAuthWebPage(cpShortName: String) = Utils.openWebPage("$API_SCHEME_AUTHORITY/$cpShortName/login")

/**
 * Registers callback API routes
 */
fun Routing.authRoutes(apiBasePath: String, ca: CloudAuth) {
    val cpShortName = ca.shortName
    route("/$apiBasePath/$cpShortName") {
        authenticate(ca.configName) {
            get("/login") { /** Redirects to 'authorizeUrl' automatically **/ }
            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                call.sessions.set(SessionData(principal?.accessToken.toString()))
                call.respondRedirect("/$apiBasePath/$cpShortName/auth-status")
            }
        }
        get("/auth-status") {
            val session: SessionData? = call.sessions.get()
            if (session != null) {
                ca.session = session
                println("--------------------")
                println("Your authentication token:\n${session.token}")
                call.respondText("Successfully authenticated in cloud provider", status = HttpStatusCode.OK)
            } else {
                call.respondText(
                    "Something went wrong. Please try again later.",
                    status = HttpStatusCode.InternalServerError
                )
            }
            exitProcess(0)
        }
    }
}
