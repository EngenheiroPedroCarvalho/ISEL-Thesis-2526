/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model

import io.ktor.client.statement.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.milliseconds

object Utils {

    const val RESOURCES = "src/main/resources"
    const val FUNC_DEPLOYMENT = "function-deployment"
    const val FUNC_TEMPLATES = "function-templates"
    const val FUNC_DEFINITION = "function-definition"
    const val LANG_CONFIGS = "language-configs"
    const val LANG_SIGNATURES = "language-signatures-read-only"
    const val PROVIDER_CONFIGS = "provider-configs"
    const val TEMPORARY = "temporary"
    const val CONFIGS_FILE = "function-configs.json"
    const val ZIP_FILE = "function-source.zip"

    /**
     * Reads the file specified by [filePath].
     * [lfFormat] true to convert '\n\r' (windows new line char) to '\n'.
     */
    fun readFile(filePath: String, lfFormat: Boolean = false): String {
        val text: String = File(filePath).readText()
        return if (lfFormat) text.replace("\r", "") else text
    }

    /**
     * Reads the file specified by [filePath] from resources.
     * [lfFormat] true to convert '\n\r' (windows new line char) to '\n'.
     */
    fun readResFile(filePath: String, lfFormat: Boolean = false): String {
        val text: String = Utils.javaClass.classLoader.getResourceAsStream(filePath)?.readTextAndClose() ?: ""
        return if (lfFormat) text.replace("\r", "") else text
    }

    // Extension function
    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
        return this.bufferedReader(charset).use { it.readText() }
    }

    /**
     * Creates a file in [filePath] with the given [content].
     */
    fun createFile(filePath: String, content: String) = File(filePath).writeText(content)

    /**
     * Creates a file in [directories] named [fileName] with the given [content].
     */
    fun createFileWithDirs(directories: String, fileName: String, content: String) {
        Files.createDirectories(Path.of(directories))
        createFile("$directories/$fileName", content)
    }

    /**
     * Creates a temporary directory in [baseDir].
     */
    fun createTempDir(baseDir: String): Path {
        val path = Path.of(baseDir)
        Files.createDirectories(path)
        return createTempDirectory(path, prefix = "quickfaas-tmp-")
    }

    /**
     * Zips [sourcesDir] to [targetZip].
     */
    fun zipDirectory(sourcesDir: String, targetZip: String) {
        val fos = FileOutputStream(targetZip)
        val zipOut = ZipOutputStream(fos)
        val fileToZip = File(sourcesDir)
        zipFile(fileToZip, fileToZip.name, zipOut, zipContentOnly = true)
        zipOut.close()
        fos.close()
    }

    // Recursive
    private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream, zipContentOnly: Boolean) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles()
            if (zipContentOnly) {
                children!!.forEach { zipFile(it, it.name, zipOut, false) }
            } else {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(ZipEntry(fileName))
                    zipOut.closeEntry()
                } else {
                    zipOut.putNextEntry(ZipEntry("$fileName/"))
                    zipOut.closeEntry()
                }
                children!!.forEach { zipFile(it, "$fileName/${it.name}", zipOut, false) }
            }
            return
        }
        val fileInStream = FileInputStream(fileToZip)
        zipOut.putNextEntry(ZipEntry(fileName))
        var length = 0
        val bytes = ByteArray(1024)
        while (length >= 0) {
            zipOut.write(bytes, 0, length)
            length = fileInStream.read(bytes)
        }
        fileInStream.close()
    }

    /**
     * Calculates the duration between sending an HTTP request and receiving the [response].
     */
    fun calculateHttpDuration(response: HttpResponse) =
        (response.responseTime.timestamp - response.requestTime.timestamp).milliseconds

}
