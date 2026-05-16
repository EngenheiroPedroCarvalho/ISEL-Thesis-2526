/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package controller

import controller.General.ANSI_RESET
import controller.General.HTTP_LOG_LEVEL
import controller.General.logMessage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

enum class HttpLogLevel { BASIC, DETAILED, NONE }

private const val HTTP_REQUEST_TIMEOUT: Long = 120000  // 2 min timeout

private const val ANSI_BLUE = "\u001B[34m"

// The HTTP client
val httpClient = HttpClient(CIO.create { requestTimeout = HTTP_REQUEST_TIMEOUT }) {
    // Observe HTTP client activity
    install(ResponseObserver) {
        onResponse { response ->
            when (HTTP_LOG_LEVEL) {
                HttpLogLevel.BASIC -> logMessage(
                    "${ANSI_RESET}HttpRequest[${response.request.method.value} $ANSI_BLUE${response.request.url.encodedPathAndQuery}$ANSI_RESET] | HttpResponse[${response.status}]",
                    2
                )
                HttpLogLevel.DETAILED -> logMessage(
                    "${ANSI_RESET}HttpRequest[${response.request.method.value} $ANSI_BLUE${response.request.url}$ANSI_RESET, ContentType=${response.request.content.contentType}] | HttpResponse[${response.status}]",
                    2
                )
                HttpLogLevel.NONE -> {}
            }
        }
    }
    // JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}
