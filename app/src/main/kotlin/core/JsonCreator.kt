package core

import kotlinx.coroutines.*
import structs.LanguageIdentifier
import structs.LanguageTable
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JsonCreator {

    private var folderNamingType: TranslationMgrFlags.FolderNaming? = null;

    suspend fun export2Json(languageTable: LanguageTable?, outputFolder: String, fileName: String, bMergeComponentAndKey: Boolean, bSkipEmptyCells: Boolean, inFolderNamingType: TranslationMgrFlags.FolderNaming?): Boolean {
        folderNamingType = inFolderNamingType;
        // we have only one line off error message, thus we just have to return wether
        // there was an error, the error is already printed and shouldnt be overriden by
        // the success message if succeeding exports were successfull.
        val allSucceeded = coroutineScope {
            val jobs = languageTable?.identifiers?.map { identifier ->
                async {
                    if (bMergeComponentAndKey) {
                        if (!exportSimple(languageTable, outputFolder, fileName, identifier, bSkipEmptyCells)) {
                            return@async false
                        }
                    } else {
                        if (!exportAdvanced(languageTable, outputFolder, fileName, identifier, bSkipEmptyCells)) {
                            return@async false
                        }
                    }
                    return@async true
                }
            }
            jobs?.awaitAll()?.contains(false)?.not() ?: true
        }
        return allSucceeded

    }

    private suspend fun exportAdvanced(languageTable: LanguageTable?, outputFolder: String, fileName: String, identifier: LanguageIdentifier, skipEmptyCells: Boolean): Boolean = withContext(Dispatchers.IO) {
        val data = languageTable?.jTableData ?: return@withContext false
        var langIndex = languageTable.getIdentifierIndex(identifier).takeIf { it != -1 } ?: return@withContext false
        // skip component and key columns
        langIndex += 2
        try {
            val pathToCreate = createOutputFolder(outputFolder, identifier) ?: return@withContext false
            // We need this filewriter to allow Umlauts
            OutputStreamWriter(FileOutputStream("$pathToCreate$fileName.json"), StandardCharsets.UTF_8).use { writer ->
                var lastComponent = ""
                var isFirstComp = true
                writer.write("{\n")
                for (row in data) {
                    val value = row[langIndex]
                    if (skipEmptyCells && (value.isNullOrBlank())) continue
                    var isFirstKeyValue = false
                    if (row[0] != lastComponent) {
                        // New component
                        if (isFirstComp) {
                            writer.write("\t\"" + row[0] + "\": {\n")
                            isFirstKeyValue = true
                            isFirstComp = false
                        } else {
                            writer.write("\n\t},\n\t\"" + row[0] + "\": {\n")
                        }
                        writer.write("\t\t\"" + row[1] + "\": " + "\"" + value + "\"")
                        lastComponent = row[0] ?: ""
                    } else {
                        if (isFirstKeyValue) {
                            writer.write("\t\t\"" + row[1] + "\": " + "\"" + value + "\"")
                        } else {
                            writer.write(",\n\t\t\"" + row[1] + "\": " + "\"" + value + "\"")
                        }
                    }
                }
                writer.write("\n\t}\n}")
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) { App.get().setStatus(e.localizedMessage, App.ERROR_MESSAGE) }
            return@withContext false
        }
        return@withContext true
    }

    private suspend fun exportSimple(languageTable: LanguageTable?, outputFolder: String, fileName: String, identifier: LanguageIdentifier, skipEmptyCells: Boolean): Boolean = withContext(Dispatchers.IO) {
        val data = languageTable?.jTableData ?: return@withContext false
        var langIndex = languageTable.getIdentifierIndex(identifier).takeIf { it != -1 } ?: return@withContext false
        // skip component and key columns
        langIndex += 2
        try {
            val pathToCreate = createOutputFolder(outputFolder, identifier) ?: return@withContext false
            // We need this filewriter to allow Umlauts
            OutputStreamWriter(FileOutputStream("$pathToCreate$fileName.json"), StandardCharsets.UTF_8).use { writer ->
                writer.write("{")
                var isFirstItem = false
                for (row in data) {
                    val value = row[langIndex]
                    if (value.isNullOrBlank()) continue
                    if (!isFirstItem) {
                        writer.write("\n\t\"" + row[0] + "_" + row[1] + "\": " + "\"" + value + "\"")
                        isFirstItem = true
                    } else {
                        writer.write(",\n\t\"" + row[0] + "_" + row[1] + "\": " + "\"" + value + "\"")
                    }
                }
                writer.write("\n}")
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) { App.get().setStatus(e.localizedMessage, App.ERROR_MESSAGE) }
            return@withContext false
        }
        return@withContext true
    }

    private suspend fun createOutputFolder(outputFolder: String, identifier: LanguageIdentifier): String? {
        val fileSep = System.lineSeparator()
        val path = outputFolder + fileSep +
                when (folderNamingType) {
                    TranslationMgrFlags.FolderNaming.BRAND_AND_LOCALE_AS_SUBFOLDER -> identifier.brand + fileSep + identifier.locale + fileSep
                    TranslationMgrFlags.FolderNaming.BRAND_LOCALE -> identifier.brand + "_" + identifier.locale + fileSep
                    TranslationMgrFlags.FolderNaming.LOCALE_BRAND -> identifier.locale + "_" + identifier.brand + fileSep
                    else -> ""
                }
        if (!createFolder(path)) return null

        return path
    }

    private suspend fun createFolder(path: String): Boolean {
        val pathObj = Paths.get(path)
        return createFolder(pathObj)
    }

    private suspend fun createFolder(path: Path): Boolean = withContext(Dispatchers.IO) {
        if (Files.exists(path)) return@withContext true
        try {
            // Create directory doesnt with with sub directories when the parent older is
            // not created yet
            Files.createDirectories(path)
            return@withContext true
        } catch (e: IOException) {
            App.get().setStatus(e.localizedMessage, App.ERROR_MESSAGE)
            return@withContext false
        }
    }

}