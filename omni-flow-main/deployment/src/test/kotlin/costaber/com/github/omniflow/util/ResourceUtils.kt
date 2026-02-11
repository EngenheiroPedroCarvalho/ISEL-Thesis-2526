package costaber.com.github.omniflow.util

import java.io.File

fun ClassLoader.getResourceContent(fileName: String): String =
    File(this.getResource(fileName)?.path ?: fileName)
        .readText(Charsets.UTF_8)
        .trim()
