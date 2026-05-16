/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

import controller.httpClient
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import model.authentication.CloudAuth
import model.authentication.CloudKeyStore.loadKeyStore
import model.authentication.SessionData
import model.authentication.authRoutes

private const val API_DOMAIN = "http://localhost"
private const val API_PORT: Int = 8080
private const val API_BASE_PATH = "quickfaas"
const val API_SCHEME_AUTHORITY = "$API_DOMAIN:$API_PORT/$API_BASE_PATH"

/**
 * Starts an HTTP server capable of receiving access tokens for the given [cloudProviders].
 */
fun startHttpServer(cloudProviders: Array<CloudAuth>) {
    embeddedServer(Netty, port = API_PORT) {
        install(Authentication) {
            loadKeyStore()
            cloudProviders.forEach { ca ->
                oauth(ca.configName) {
                    urlProvider = { "$API_DOMAIN:$API_PORT/$API_BASE_PATH/${ca.shortName}/callback" }
                    providerLookup = { ca.getOAuthSettings() }
                    client = httpClient
                }
            }
        }
        install(Sessions) { cookie<SessionData>("user_session") }
        // Register API routes
        cloudProviders.forEach { cc -> this.routing { authRoutes(API_BASE_PATH, cc) } }
    }.start(wait = false)
}
