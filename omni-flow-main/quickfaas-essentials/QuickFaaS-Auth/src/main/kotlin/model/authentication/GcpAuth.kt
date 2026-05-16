/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

package model.authentication

import io.ktor.http.*
import io.ktor.server.auth.OAuthServerSettings.*
import model.authentication.CloudKeyStore.getEntry

object GcpAuth : CloudAuth {
    override val shortName = "gcp"
    override val configName = "oauth-gcp"
    override var session = SessionData()
    override fun getOAuthSettings() = OAuth2ServerSettings(
        name = "google",
        authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
        accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
        requestMethod = HttpMethod.Post,
        clientId = getEntry("gcp_clientId"),
        clientSecret = getEntry("gcp_clientSecret"),
        defaultScopes = listOf(
            "https://www.googleapis.com/auth/cloud-platform"
        )
    )
}
