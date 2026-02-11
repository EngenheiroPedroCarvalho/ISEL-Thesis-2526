package costaber.com.github.omniflow.builder

import costaber.com.github.omniflow.model.Authentication

class AuthenticationBuilder : Builder<Authentication> {

    private lateinit var type: String
    private var scope: String? = null
    private var scopes: String? = null
    private var audience: String? = null

    fun type(value: String) = apply { this.type = value }

    fun scope(value: String) = apply { this.scope = value }

    fun scopes(value: String) = apply { this.scopes = value }

    fun audience(value: String) = apply { this.audience = value }

    override fun build() = Authentication(
        type = type,
        scope = scope,
        scopes = scopes,
        audience = audience
    )
}