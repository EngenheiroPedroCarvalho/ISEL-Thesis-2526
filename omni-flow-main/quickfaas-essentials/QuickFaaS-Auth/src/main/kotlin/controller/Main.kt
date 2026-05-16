/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

package controller

import model.authentication.GcpAuth
import model.authentication.MsAzureAuth
import model.authentication.openAuthWebPage
import startHttpServer
import java.util.*

private val cloudProviders = arrayOf(MsAzureAuth, GcpAuth)

fun main() {
    startHttpServer(cloudProviders)
    val scan = Scanner(System.`in`)
    val option = getSelectedProvider(scan)
    if (option != 99) {
        openAuthWebPage(cloudProviders[option].shortName)
        scan.next()
    }
}

/**
 * Obtains selected cloud provider option
 */
fun getSelectedProvider(scan: Scanner): Int {
    println("In which cloud provider would you like to authenticate?")
    var option: Int
    do {
        println(" 0: Microsoft Azure")
        println(" 1: Google Cloud Platform")
        println("99: Exit")
        print("Choose an option:")
        option = scan.nextInt()
    } while (!((option in 0..1) || option == 99));
    return option
}
