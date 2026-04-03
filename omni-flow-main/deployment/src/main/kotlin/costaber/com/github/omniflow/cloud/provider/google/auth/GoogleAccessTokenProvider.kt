package costaber.com.github.omniflow.cloud.provider.google.auth

import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException

class GoogleAccessTokenProvider(
    private val credentials: GoogleCredentials =
        GoogleCredentials.getApplicationDefault().createScoped(
            listOf("https://www.googleapis.com/auth/cloud-platform")
        )
) {
    fun getTokenValue(): String{
        try {
            credentials.refreshIfExpired()
            if(credentials.accessToken == null){
                credentials.refresh()
            }
            val token = credentials.accessToken?.tokenValue
            require(!token.isNullOrBlank()) {"ADC token is null/blank after refresh()" }
            return token
        } catch (e: IOException) {
            throw IllegalStateException(
                "Failed to obtain Application Default Credentials access token. " +
                "If running locally, run: gcloud auth application-default login",
                e
            )
        }
    }
}