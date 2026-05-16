/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes

enum class RuntimeVersion(val runtime: Runtime, val version: String) {
    JAVA11(Runtime.JAVA, "11"), JAVA17(Runtime.JAVA, "17"), NODEJS14(Runtime.NODEJS, "14")
}

