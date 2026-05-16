/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

package controller

import io.ktor.client.*
import io.ktor.client.engine.cio.*

private const val HTTP_REQUEST_TIMEOUT: Long = 120000  // 2 min timeout

val httpClient = HttpClient(CIO.create { requestTimeout = HTTP_REQUEST_TIMEOUT })
