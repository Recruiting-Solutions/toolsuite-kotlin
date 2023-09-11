package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType.BOOLEAN
import org.apache.poi.ss.usermodel.CellType.NUMERIC
import org.apache.poi.ss.usermodel.CellType.STRING
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import structs.Language
import structs.LanguageIdentifier
import structs.LanguageRowMap
import structs.LanguageTable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// Grid size in which this tool searches for all necessary data to extract the
// rest of a single sheet.
private const val MAX_SEARCH_COLUMN = 60
private const val MAX_SEARCH_ROW = 20

private val ISO_CODES = setOf(
        "af_za", "am_et", "ar_ae", "ar_bh", "ar_dz", "ar_eg", "ar_iq", "ar_jo", "ar_kw", "ar_lb", "ar_ly", "ar_ma",
        "arn_cl", "ar_om", "ar_qa", "ar_sa", "ar_sd", "ar_sy", "ar_tn", "ar_ye", "as_in", "az_az", "az_cyrl_az", "az_latn_az", "ba_ru", "be_by", "bg_bg", "bn_bd", "bn_in", "bo_cn", "br_fr",
        "bs_cyrl_ba", "bs_latn_ba", "ca_es", "co_fr", "cs_cz", "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "de_li", "de_lu", "dsb_de", "dv_mv", "el_cy", "el_gr", "en_029", "en_au", "en_bz", "en_ca",
        "en_cb", "en_gb", "en_es", "en_ie", "en_in", "en_it", "en_jm", "en_mt", "en_my", "en_nz", "en_ph", "en_pt", "en_tr", "en_sg", "en_tt", "en_us", "en_za", "en_zw", "es_ar", "es_bo", "es_cl",
        "es_co", "es_cr", "es_do", "es_ec", "es_es", "es_gt", "es_hn", "es_mx", "es_ni", "es_pa", "es_pe", "es_pr", "es_py", "es_sv", "es_us", "es_uy", "es_ve", "et_ee", "eu_es", "fa_ir", "fi_fi",
        "fil_ph", "fo_fo", "fr_be", "fr_ca", "fr_ch", "fr_fr", "fr_lu", "fr_mc", "fy_nl", "ga_ie", "gd_gb", "gd_ie", "gl_es", "gsw_fr", "gu_in", "ha_latn_ng", "he_il", "hi_in", "hr_ba", "hr_hr",
        "hsb_de", "hu_hu", "hy_am", "id_id", "ig_ng", "ii_cn", "in_id", "is_is", "it_ch", "it_it", "iu_cans_ca", "iu_latn_ca", "iw_il", "ja_jp", "ka_ge", "kk_kz", "kl_gl", "km_kh", "kn_in", "kok_in",
        "ko_kr", "ky_kg", "lb_lu", "lo_la", "lt_lt", "lv_lv", "mi_nz", "mk_mk", "ml_in", "mn_mn", "mn_mong_cn", "moh_ca", "mr_in", "ms_bn", "ms_my", "mt_mt", "nb_no", "ne_np", "nl_be", "nl_nl",
        "nn_no", "no_no", "nso_za", "oc_fr", "or_in", "pa_in", "pl_pl", "prs_af", "ps_af", "pt_br", "pt_pt", "qut_gt", "quz_bo", "quz_ec", "quz_pe", "rm_ch", "ro_mo", "ro_ro", "ru_mo", "ru_ru",
        "rw_rw", "sah_ru", "sa_in", "se_fi", "se_no", "se_se", "si_lk", "sk_sk", "sl_si", "sma_no", "sma_se", "smj_no", "smj_se", "smn_fi", "sms_fi", "sq_al", "sr_ba", "sr_cs", "sr_cyrl_ba",
        "sr_cyrl_cs", "sr_cyrl_me", "sr_cyrl_rs", "sr_latn_ba", "sr_latn_cs", "sr_latn_me", "sr_latn_rs", "sr_me", "sr_rs", "sr_sp", "sv_fi", "sv_se", "sw_ke", "syr_sy", "ta_in", "te_in",
        "tg_cyrl_tj", "th_th", "tk_tm", "tlh_qs", "tn_za", "tr_tr", "tt_ru", "tzm_latn_dz", "ug_cn", "uk_ua", "ur_pk", "uz_cyrl_uz", "uz_latn_uz", "uz_uz", "vi_vn", "wo_sn", "xh_za", "yo_ng", "zh_cn",
        "zh_hk", "zh_mo", "zh_sg", "zh_tw", "zu_za"
)

private const val COMPONENT = "component"
private const val KEY = "key"

class TranslationMgr {
    private var languageTable: LanguageTable? = null

    // These are variables for stat tracking.
    var statNumEmptyCells = 0
    var statCalculationTime = 0L
    var files: Array<File>? = null
    var appendBrandToLocale = false
    private var importFlags = 0
    private var exportFlags = 0
    var folderNamingType: TranslationMgrFlags.FolderNaming? = null

    private fun getFlag(flag: TranslationMgrFlags.Import): Boolean {
        return ((importFlags shr flag.ordinal) and 1) == 1
    }

    private fun getFlag(flag: TranslationMgrFlags.Export): Boolean {
        return ((exportFlags shr flag.ordinal) and 1) == 1
    }

    fun setFlag(flag: TranslationMgrFlags.Import, bEnable: Boolean) {
        importFlags = if (bEnable) {
            importFlags or 1 shl flag.ordinal
        } else {
            importFlags and (1 shl flag.ordinal).inv()
        }
    }

    fun setFlag(flag: TranslationMgrFlags.Export, bEnable: Boolean) {
        exportFlags = if (bEnable) {
            exportFlags or 1 shl flag.ordinal
        } else {
            exportFlags and (1 shl flag.ordinal).inv()
        }
    }

    fun getNumSelectedFiles(): Int {
        return files?.size ?: 0
    }

    companion object {
        private suspend fun getSheetsFromExcel(sheets: MutableSet<Sheet>, file: File?) = withContext(Dispatchers.IO) {
            if (file == null) return@withContext
            try {
                val fis = FileInputStream(file)
                XSSFWorkbook(fis).use { workbook ->
                    val numSheets = workbook.numberOfSheets
                    for (i in 0..<numSheets) {
                        sheets.add(workbook.getSheetAt(i))
                    }
                }
            } catch (e: IOException) {
                App.get().setStatus(e.localizedMessage, App.ERROR_MESSAGE)
            }
        }
    }

    private var emptyCellValue = ""

    private fun getCellValue(row: Row?, col: Int): String? {
        if (row == null) return null
        val cell = row.getCell(col, MissingCellPolicy.CREATE_NULL_AS_BLANK) ?: return null
        if (getFlag(TranslationMgrFlags.Import.USE_HYPERLINK_IF_AVAILABLE)) {
            cell.hyperlink?.let { return it.address }
        }
        return when (cell.cellType) {
            BOOLEAN -> fixString(cell.booleanCellValue.toString())
            STRING -> fixString(cell.stringCellValue)
            NUMERIC -> cell.numericCellValue.toInt().toString()
            else -> emptyCellValue
        }
    }

    private fun fixString(str: String): String {
        return str
                .replace("\n", " ")
                .replace("\r", " ").replace(System.getProperty("line.separator"), " ")
                // Replace double spacebar
                .replace("\"", "\\\"")
                .replace(Regex("( )+"), " ")
                .trim()
    }

    fun getFileName(path: String): String {
        val dot = path.lastIndexOf(".")
        val slash = path.lastIndexOf("\\").takeIf { it != -1 } ?: path.lastIndexOf("/")
        if (dot > slash) return path.substring(if (slash > 0) slash + 1 else 0, dot)
        return path.substring(if (slash > 0) slash + 1 else 0)
    }

    suspend fun export2Json(outputFolder: String, fileName: String): Boolean {
        // we have only one line off error message, thus we just have to return wether
        // there was an error, the error is already printed and shouldnt be overriden by
        // the success message if succeeding exports were successfull.
        val bMergeComponentAndKey = getFlag(TranslationMgrFlags.Export.CONCAT_COMPONENT_AND_KEY)
        val bSkipEmptyCells = getFlag(TranslationMgrFlags.Export.DONT_EXPORT_EMPTY_VALUES)
        val allSucceeded = coroutineScope {
            val jobs = languageTable?.identifiers?.map { identifier ->
                async {
                    if (bMergeComponentAndKey) {
                        if (!exportSimple(outputFolder, fileName, identifier, bSkipEmptyCells)) {
                            return@async false
                        }
                    } else {
                        if (!exportAdvanced(outputFolder, fileName, identifier, bSkipEmptyCells)) {
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

    private suspend fun exportAdvanced(outputFolder: String, fileName: String, identifier: LanguageIdentifier, skipEmptyCells: Boolean): Boolean = withContext(Dispatchers.IO) {
        val data = languageTable?.jTableData ?: return@withContext false
        var langIndex = languageTable?.getIdentifierIndex(identifier)?.takeIf { it != -1 } ?: return@withContext false
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

    private suspend fun exportSimple(outputFolder: String, fileName: String, identifier: LanguageIdentifier, skipEmptyCells: Boolean): Boolean = withContext(Dispatchers.IO) {
        val data = languageTable?.jTableData ?: return@withContext false
        var langIndex = languageTable?.getIdentifierIndex(identifier)?.takeIf { it != -1 } ?: return@withContext false
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
        val fileSep = System.getProperty("file.separator")
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

    private fun isLocale(value: String): Boolean {
        return ISO_CODES.contains(value.lowercase())
    }

    private fun findBrand(sheet: Sheet?): String? {
        if (sheet == null) return ""
        val row = sheet.getRow(0) ?: return ""
        return getCellValue(row, 0)
    }

    /**
     * @return List of all locales found in the sheet including their columnID.
     **/
    private fun findLocales(sheet: Sheet?): List<Pair<LanguageIdentifier, Int>>? {
        if (sheet == null) return null
        val brand = findBrand(sheet)?.takeIf { it.isNotBlank() } ?: "NO_BRAND"
        val locales = mutableListOf<Pair<LanguageIdentifier, Int>>()
        val lastRow = MAX_SEARCH_ROW.coerceAtMost(sheet.lastRowNum) // 0 based
        for (r in 0..lastRow) {
            val row = sheet.getRow(r) ?: continue
            val lastCol = MAX_SEARCH_COLUMN.coerceAtMost(row.lastCellNum.toInt())
            for (c in 0..<lastCol) {
                val value = getCellValue(row, c)
                if (value != null && isLocale(value)) {
                    locales.add(LanguageIdentifier(brand, value) to c)
                }
            }
        }
        return locales
    }

    private fun extractLanguage(sheet: Sheet?, languageIdentifier: LanguageIdentifier, startRow: Int, componentCol: Int, keyCol: Int, valueCol: Int): Language? {
        if (sheet == null) return null

        val language = Language(languageIdentifier)

        for (r in startRow..sheet.lastRowNum) { // 0 based
            val row = sheet.getRow(r) ?: continue
            val component = getCellValue(row, componentCol)
            if (component.isNullOrBlank()) continue

            val key = getCellValue(row, keyCol)
            if (key.isNullOrBlank()) continue
            val value = getCellValue(row, valueCol) ?: ""
            language.addUnique(component, key, value)
        }
        return language
    }

    private fun findFirstValueRow(sheet: Sheet?, componentCol: Int, keyCol: Int, valueCol: Int): Int {
        if (sheet == null) return -1

        var foundTableHeader = false
        val lastRow = MAX_SEARCH_ROW.coerceAtMost(sheet.lastRowNum) // 0 based
        for (r in 0..lastRow) {
            val row = sheet.getRow(r) ?: continue
            val component = getCellValue(row, componentCol) ?: continue
            val key = getCellValue(row, keyCol) ?: continue

            if (!foundTableHeader) {
                if (component.equals(COMPONENT, ignoreCase = true) && key.equals(KEY, ignoreCase = true)) {
                    foundTableHeader = true
                }
            } else {
                if (component.isBlank() || key.isBlank()) continue
                return r
            }
        }
        return -1
    }

    /**
     * Searches in a clamped area of r= 20 to c = 40 and returns the index of the first occurence of the specified string.
     */
    private fun findColumnWithString(sheet: Sheet?, string: String): Int {
        if (sheet == null) return -1
        val lastRow = MAX_SEARCH_ROW.coerceAtMost(sheet.lastRowNum) // 0 based
        for (r in 0..lastRow) {
            val row = sheet.getRow(r) ?: continue
            for (c in 0..<MAX_SEARCH_COLUMN) {
                val value = getCellValue(row, c)
                if (value != null && value.equals(string, ignoreCase = true)) return c
            }
        }
        return -1
    }

    private fun extractSheet(sheet: Sheet?): List<Language>? {
        if (sheet == null) return null
        val locales = findLocales(sheet) ?: return null
        val componentCol = findColumnWithString(sheet, "component")
        val keyCol = findColumnWithString(sheet, "key")
        if (componentCol == -1 || keyCol == -1) return null

        val languages = mutableListOf<Language>()

        for ((identifier, valueCol) in locales) {
            val firstRow = findFirstValueRow(sheet, componentCol, keyCol, valueCol)
            val lang = extractLanguage(sheet, identifier, firstRow, componentCol, keyCol, valueCol) ?: continue
            languages.add(lang)
        }
        return languages
    }

    private fun containsLanguage(languages: List<Language>?, otherLanguage: Language): Boolean {
        if (languages == null) return false
        for (language in languages) {
            if (language.isSameLanguageIdentifier(otherLanguage)) return true
        }
        return false
    }

    private fun appendLanguage(languages: List<Language>?, other: Language) {
        if (languages == null) return
        for (language in languages) {
            if (language.isSameLanguageIdentifier(other)) {
                language.appendTable(other)
            }
        }
    }

    private fun buildRowMap(languages: List<Language>): LanguageRowMap {
        val rowMap = LanguageRowMap()
        for (lang in languages) {
            for (component in lang.tree.entries) {
                for (key in component.value.keys) {
                    rowMap.addUnique(component.key, key)
                }
            }
        }
        rowMap.buildRowMap()
        return rowMap
    }

    suspend fun importExcelFiles(): LanguageTable? {
        if (files == null) return null
        statNumEmptyCells = 0
        val sheets = mutableSetOf<Sheet>()
        files?.forEach { file ->
            getSheetsFromExcel(sheets, file)
        }
        val sumLanguages = mutableListOf<Language>()
        for (sheet in sheets) {
            val languages = extractSheet(sheet) ?: continue
            for (language in languages) {
                if (containsLanguage(sumLanguages, language)) {
                    appendLanguage(sumLanguages, language)
                } else {
                    sumLanguages.add(language)
                }
            }
        }

        if (sumLanguages.isEmpty()) return null

        val rowMap = buildRowMap(sumLanguages)
        val numLangs = sumLanguages.size
        val data = Array(rowMap.rowMap.size) { index ->
            arrayOfNulls<String>(numLangs + 2).also {
                it[0] = rowMap.rowMap[index][0]
                it[1] = rowMap.rowMap[index][1]
            }
        }
        val header = Array(sumLanguages.size) {
            val lang = sumLanguages[it]
            lang.identifier
        }

        sortColumns(header, sumLanguages)

        for (c in 0..<numLangs) {
            val lang = sumLanguages[c]
            for ((r, row) in rowMap.rowMap.withIndex()) {
                val value = lang.findValue(row[0], row[1])
                if (value.isBlank()) ++statNumEmptyCells
                data[r][c + 2] = value
            }
        }
        languageTable = LanguageTable(header, data)

        return languageTable
    }

    fun startTimeTrace() {
        statCalculationTime = System.currentTimeMillis()
    }

    fun stopTimeTrace() {
        statCalculationTime = System.currentTimeMillis() - statCalculationTime
    }

    private fun sortColumns(identifiers: Array<LanguageIdentifier>, languages: MutableList<Language>) {
        sort(0, identifiers.size - 1, identifiers, languages)
    }

    private fun sort(l: Int, r: Int, identifiers: Array<LanguageIdentifier>, languages: MutableList<Language>) {
        if (l < r) {
            val q = (l + r) / 2

            sort(l, q, identifiers, languages)
            sort(q + 1, r, identifiers, languages)
            mergeSort(l, q, r, identifiers, languages)
        }
    }

    private fun mergeSort(l: Int, q: Int, r: Int, identifiers: Array<LanguageIdentifier>, languages: MutableList<Language>) {
        val identifierCopy = arrayOfNulls<LanguageIdentifier>(identifiers.size)
        val dataCopy = arrayOfNulls<Language>(languages.size)
        for (i in l..q) {
            identifierCopy[i] = identifiers[i]
            dataCopy[i] = languages[i]
        }
        for (j in (q + 1)..r) {
            val o = r + q + 1 - j
            identifierCopy[o] = identifiers[j]
            dataCopy[o] = languages[j]
        }
        var i = l
        var j = r
        for (k in l..r) {
            val one = identifierCopy[i]!!
            val two = identifierCopy[j]!!
            val bPreceedingBrand = one.brand < two.brand
            val bSameBrand = one.brand == two.brand
            val bPreceedingLocale = one.locale < two.locale

            if (bPreceedingBrand || (bSameBrand && bPreceedingLocale)) {
                identifiers[k] = one
                languages[k] = dataCopy[i]!!
                i++
            } else {
                identifiers[k] = identifierCopy[j]!!
                languages[k] = dataCopy[j]!!
                j--
            }
        }
    }
}
