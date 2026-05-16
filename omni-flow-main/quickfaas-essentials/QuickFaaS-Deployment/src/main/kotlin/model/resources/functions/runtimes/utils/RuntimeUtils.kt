/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes.utils

import model.Utils
import model.resources.functions.runtimes.Runtime

interface RuntimeUtils {
    val runtime: Runtime

    /**
     * Read a template file named [fileName] from [templatesDir].
     */
    fun readTemplateFile(fileName: String, templatesDir: String) =
        Utils.readResFile(filePath = "${runtime.templatesDirRoot}/$templatesDir/$fileName")

    /**
     * Create a file named [fileName] with a certain [content] in the
     * recently created temporary directory [tmpDir].
     */
    fun createFileInTmp(fileName: String, content: String, tmpDir: String) =
        Utils.createFileWithDirs(directories = "${runtime.tmpDirsRoot}/$tmpDir", fileName, content)

    /**
     * Copies the specified template file [fileName] from [templatesDir] to the
     * temporary directory [tmpDir].
     */
    fun copyTemplateFileToTmp(fileName: String, templatesDir: String, tmpDir: String) =
        createFileInTmp(fileName, readTemplateFile(fileName, templatesDir), tmpDir)

}