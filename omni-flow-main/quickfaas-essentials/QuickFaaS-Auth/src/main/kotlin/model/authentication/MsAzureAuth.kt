/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

package model.authentication

import io.ktor.http.*
import io.ktor.server.auth.OAuthServerSettings.*
import model.authentication.CloudKeyStore.getEntry

object MsAzureAuth : CloudAuth {
    override val shortName = "msazure"
    override val configName = "oauth-msazure"
    override var session = SessionData()
    override fun getOAuthSettings() = OAuth2ServerSettings(
        name = "msazure",
        authorizeUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        accessTokenUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        requestMethod = HttpMethod.Post,
        clientId = getEntry("msazure_clientId"),
        clientSecret = getEntry("msAzure_clientSecret"),
        defaultScopes = listOf(
            "https://management.azure.com/user_impersonation"
        )
    )
}
