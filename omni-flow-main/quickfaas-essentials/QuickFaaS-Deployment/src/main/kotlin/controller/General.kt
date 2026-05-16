/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package controller

import kotlin.system.exitProcess

object General {
    val HTTP_LOG_LEVEL = HttpLogLevel.BASIC     // Establish level of detail for logging
    const val AUTO_DELETE_FUNC_ZIP = true       // Delete function ZIP file after deployment when true

    // Console colors
    const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_GREEN = "\u001B[32m"
    private const val ANSI_RED = "\u001B[31m"
    private const val ANSI_YELLOW = "\u001B[33m"

    /**
     * Logs to the console a given [message], colored using the following [code]:
     *
     * 0 -> Success
     * 1 -> Error
     * 2 -> Info
     */
    fun logMessage(message: String, code: Int) {
        when (code) {
            0 -> println("$ANSI_GREEN$message$ANSI_RESET")
            1 -> {
                println("$ANSI_RED$message$ANSI_RESET")
                exitProcess(1)  // Terminates the currently running process.
            }
            2 -> println("$ANSI_YELLOW$message$ANSI_RESET")
        }
    }
}
