/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.requests

interface CloudRequests {
    /**
     * Stores recently arrived OAuth 2.0 bearer [token].
     */
    fun setBearerToken(token: String)
}
