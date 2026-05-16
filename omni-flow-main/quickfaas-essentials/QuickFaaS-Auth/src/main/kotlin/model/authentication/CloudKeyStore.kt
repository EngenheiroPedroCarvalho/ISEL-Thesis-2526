/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

package model.authentication

import model.Utils.readResFile
import model.Utils.readResFileAsStream
import java.security.KeyStore

object CloudKeyStore {

    private val keyStoreStream = readResFileAsStream("keystore/key.store")
    private val keyPwdContent = readResFile("keystore/key.password").toCharArray()
    private val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    /**
     * Loads cloud keys.
     */
    fun loadKeyStore() = keyStore.load(keyStoreStream, keyPwdContent)

    /**
     * Returns the entry value associated with the [entryKey].
     */
    fun getEntry(entryKey: String): String {
        val entryValue = keyStore.getEntry(entryKey, KeyStore.PasswordProtection(keyPwdContent))
        return String((entryValue as KeyStore.SecretKeyEntry).secretKey.encoded)
    }

}