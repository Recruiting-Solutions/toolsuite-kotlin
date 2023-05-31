package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import structs.Language;
import structs.LanguageTable;
import structs.LanguageRowMap;

public class TranslationMgr
{
	private LanguageTable languageTable;
	// Grid size in which this tool searches for all necessary data to extract the rest of a single sheet.
	private static final int MAX_SEARCH_COLUMN = 40;
	private static final int MAX_SEARCH_ROW = 20;
	// These are variables for stat tracking.
	public int statNumEmptyCells = 0;
	public long statCalculationTime = 0;
	public File[] files;
	public boolean appendBrandToLocale = false;
	private int importFlags = 0;
	private int exportFlags = 0;

	private boolean getFlag(TranslationMgrFlags.Import flag)
	{
		return ((importFlags >> flag.ordinal()) & 1) == 1;
	}

	private boolean getFlag(TranslationMgrFlags.Export flag)
	{
		return ((exportFlags >> flag.ordinal()) & 1) == 1;
	}

	public void setFlag(TranslationMgrFlags.Import flag, boolean bEnable)
	{
		if (bEnable)
		{
			importFlags |= 1 << flag.ordinal();
		}
		else
		{
			importFlags &= ~(1 << flag.ordinal());
		}
	}

	public void setFlag(TranslationMgrFlags.Export flag, boolean bEnable)
	{
		if (bEnable)
		{
			exportFlags |= 1 << flag.ordinal();
		}
		else
		{
			exportFlags &= ~(1 << flag.ordinal());
		}
	}

	public int getNumSelectedFiles()
	{
		if (files == null) return 0;
		return files.length;
	}

	private static void getSheetsFromExcel(HashSet<Sheet> sheets, File file)
	{
		if (file == null) return;
		FileInputStream fis;
		try
		{
			fis = new FileInputStream(file);
			Workbook workbook = new XSSFWorkbook(fis);
			int numSheets = workbook.getNumberOfSheets();
			for (int i = 0; i < numSheets; ++i)
			{
				sheets.add(workbook.getSheetAt(i));
			}
			workbook.close();
		}
		catch (Exception e)
		{
			App.get().setStatus(e.getLocalizedMessage(), App.ERROR_MESSAGE);
		}
	}

	private String emptyCellValue = "";

	private String getCellValue(Row row, int col)
	{
		if (row == null) return null;
		Cell cell = row.getCell(col, MissingCellPolicy.CREATE_NULL_AS_BLANK);
		if (cell == null) return null;
		switch (cell.getCellType())
		{
		default:
		case BLANK:
			return emptyCellValue;
		case BOOLEAN:
			return fixString(String.valueOf(cell.getBooleanCellValue()));
		case STRING:
			return fixString(cell.getStringCellValue());
		case NUMERIC:
			double value = cell.getNumericCellValue();
			if (value % 1 == 0)
			{
				return String.valueOf((int) value);
			}
			else
			{
				return String.valueOf(value);
			}
		}
	}

	private String fixString(String str)
	{
		String value = str.replace("\n", "").replace("\r", "").replace(System.getProperty("line.separator"), "");
		// Replace double spacebar
		value = value.replace("\"", "\\\"");
		return value.trim();
	}

	public String getFileName(String path)
	{
		int dot = path.lastIndexOf(".");
		int slash = path.lastIndexOf("\\");
		if (slash == -1) slash = path.lastIndexOf("/");
		if (dot > slash) return path.substring(slash > 0 ? slash + 1 : 0, dot);
		return path.substring(slash > 0 ? slash + 1 : 0);
	}

	public boolean export2Json(String outputFolder, String fileName)
	{
		// we have only one line off error message, thus we just have to return wether
		// there was an error, the error is already printed and shouldnt be overriden by
		// the success message if succeeding exports were successfull.
		boolean success = true;
		boolean bMergeComponentAndKey = getFlag(TranslationMgrFlags.Export.CONCAT_COMPONENT_AND_KEY);
		boolean bSkipEmptyCells = getFlag(TranslationMgrFlags.Export.DONT_EXPORT_EMPTY_VALUES);
		for (String locale : languageTable.getLocales())
		{
			if (bMergeComponentAndKey)
			{
				success = success && exportSimple(outputFolder, fileName, locale, bSkipEmptyCells);
			}

			else
			{
				success = success && exportAdvanced(outputFolder, fileName, locale, bSkipEmptyCells);
			}
		}
		return success;
	}

	private boolean exportAdvanced(String outputFolder, String fileName, String locale, boolean skipEmptyCells)
	{
		String[][] data = languageTable.getJTableData();
		int langIndex = languageTable.getLocaleIndex(locale);
		if (langIndex == -1) return false;
		// skip component and key columns;
		langIndex += 2;
		try
		{
			// We need this filewriter to allow Umlauts
			Files.createDirectories(Paths.get(outputFolder));
			Writer writer = new OutputStreamWriter(new FileOutputStream(outputFolder + System.getProperty("file.separator") + fileName + "_" + locale + ".json"), StandardCharsets.UTF_8);
			String lastComponent = "";
			boolean isFirstComp = true;
			writer.write("{\n");
			for (String[] row : data)
			{
				String value = row[langIndex];
				if (skipEmptyCells && (value.isBlank() || value.isEmpty())) continue;
				boolean isFirstKeyValue = false;
				if (row[0] != lastComponent)
				{
					// New component
					if (isFirstComp)
					{
						writer.write("\t\"" + row[0] + "\": {\n");
						isFirstKeyValue = true;
						isFirstComp = false;
					}
					else
					{
						writer.write("\n\t},\n\t\"" + row[0] + "\": {\n");
					}
					writer.write("\t\t\"" + row[1] + "\": " + "\"" + value + "\"");
					lastComponent = row[0];
				}
				else
				{
					if (isFirstKeyValue)
					{
						writer.write("\t\t\"" + row[1] + "\": " + "\"" + value + "\"");
					}
					else
					{
						writer.write(",\n\t\t\"" + row[1] + "\": " + "\"" + value + "\"");
					}
				}
			}
			writer.write("\n\t}\n}");
			writer.close();
		}
		catch (Exception e)
		{
			App.get().setStatus(e.getLocalizedMessage(), App.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private boolean exportSimple(String outputFolder, String fileName, String locale, boolean skipEmptyCells)
	{
		String[][] data = languageTable.getJTableData();
		int langIndex = languageTable.getLocaleIndex(locale);
		if (langIndex == -1) return false;
		// skip component and key columns;
		langIndex += 2;
		try
		{
			// We need this filewriter to allow Umlauts
			Files.createDirectories(Paths.get(outputFolder));
			Writer writer = new OutputStreamWriter(new FileOutputStream(outputFolder + System.getProperty("file.separator") + fileName + "_" + locale + ".json"), StandardCharsets.UTF_8);
			writer.write("{");
			boolean isFirstItem = false;
			for (String[] row : data)
			{
				String value = row[langIndex];
				if (value.isBlank() || value.isEmpty()) continue;
				if (!isFirstItem)
				{
					writer.write("\n\t\"" + row[0] + "_" + row[1] + "\": " + "\"" + value + "\"");
					isFirstItem = true;
				}
				else
				{
					writer.write(",\n\t\"" + row[0] + "_" + row[1] + "\": " + "\"" + value + "\"");
				}
			}
			writer.write("\n}");
			writer.close();
		}
		catch (Exception e)
		{
			App.get().setStatus(e.getLocalizedMessage(), App.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private HashSet<String> ISO_CODES = new HashSet<String>(Arrays.asList(new String[] { "af_za", "am_et", "ar_ae", "ar_bh", "ar_dz", "ar_eg", "ar_iq", "ar_jo", "ar_kw", "ar_lb", "ar_ly", "ar_ma",
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
		"zh_hk", "zh_mo", "zh_sg", "zh_tw", "zu_za" }));

	private boolean isLocale(String value)
	{
		return ISO_CODES.contains(value.toLowerCase());
	}

	private String findBrand(Sheet sheet)
	{
		if (sheet == null) return "";
		Row row = sheet.getRow(0);
		if (row == null) return "";
		return getCellValue(row, 0);
	}

	/**
	 * @return List of all locales found in the sheet including their columnID.
	 **/
	private ArrayList<Pair<String, Integer>> findLocales(Sheet sheet)
	{
		if (sheet == null) return null;
		String brand = findBrand(sheet);
		boolean bAddBrand = getFlag(TranslationMgrFlags.Import.APPEND_BRAND_TO_LOCALE);
		ArrayList<Pair<String, Integer>> locales = new ArrayList<Pair<String, Integer>>(32);
		int lastRow = Math.min(MAX_SEARCH_ROW, sheet.getLastRowNum()); // 0 based
		for (int r = 0; r <= lastRow; ++r)
		{
			Row row = sheet.getRow(r);
			if (row == null) continue;
			int lastCol = Math.min(MAX_SEARCH_COLUMN, row.getLastCellNum());
			for (int c = 0; c < lastCol; ++c)
			{
				String value = getCellValue(row, c);
				if (value != null && isLocale(value))
				{
					locales.add(new Pair<String, Integer>(bAddBrand ? brand + value : value, c));
				}
			}
		}
		return locales;
	}

	private Language extractLanguage(Sheet sheet, String locale, int startRow, int componentCol, int keyCol, int valueCol)
	{
		if (sheet == null) return null;

		Language language = new Language();
		language.locale = locale;
		for (int r = startRow; r <= sheet.getLastRowNum(); ++r)// 0 based
		{
			Row row = sheet.getRow(r);
			if (row == null) continue;
			String component = getCellValue(row, componentCol);
			if (component == null || component.isBlank() || component.isEmpty()) continue;

			String key = getCellValue(row, keyCol);
			if (key == null || key.isBlank() || key.isEmpty()) continue;
			String value = getCellValue(row, valueCol);
			language.addUnique(component, key, value);
		}
		return language;
	}

	private int findFirstValueRow(Sheet sheet, int componentCol, int keyCol, int valueCol)
	{
		if (sheet == null) return -1;
		final String COMPONENT = "component";
		final String KEY = "key";
		boolean foundTableHeader = false;
		int lastRow = Math.min(MAX_SEARCH_ROW, sheet.getLastRowNum()); // 0 based
		for (int r = 0; r <= lastRow; ++r)
		{
			Row row = sheet.getRow(r);
			if (row == null) continue;
			String component = getCellValue(row, componentCol);
			if (component == null) continue;

			String key = getCellValue(row, keyCol);
			if (key == null) continue;

			if (!foundTableHeader)
			{
				if (component.equalsIgnoreCase(COMPONENT) && key.equalsIgnoreCase(KEY))
				{
					foundTableHeader = true;
				}
			}
			else
			{
				if (component.isBlank() || component.isEmpty() || key.isBlank() || key.isEmpty()) continue;
				return r;
			}
		}
		return -1;
	}

	/**
	 * Searches in a clamped area of r= 20 to c = 40 and returns the index of the first occurence of the specified string.
	 */
	private int findColumnWithString(Sheet sheet, String string)
	{
		if (sheet == null) return -1;
		int lastRow = Math.min(MAX_SEARCH_ROW, sheet.getLastRowNum()); // 0 based
		for (int r = 0; r <= lastRow; ++r)
		{
			Row row = sheet.getRow(r);
			if (row == null) continue;
			for (int c = 0; c < MAX_SEARCH_COLUMN; ++c)
			{
				String value = getCellValue(row, c);
				if (value != null && value.equalsIgnoreCase(string)) return c;
			}
		}
		return -1;
	}

	private ArrayList<Language> extractSheet(Sheet sheet)
	{
		if (sheet == null) return null;
		ArrayList<Pair<String, Integer>> locales = findLocales(sheet);
		int componentCol = findColumnWithString(sheet, "component");
		int keyCol = findColumnWithString(sheet, "key");
		ArrayList<Language> languages = new ArrayList<Language>(32);

		for (Pair<String, Integer> pair : locales)
		{
			int valueCol = pair.getValue();
			int firstRow = findFirstValueRow(sheet, componentCol, keyCol, valueCol);
			Language lang = extractLanguage(sheet, pair.getKey(), firstRow, componentCol, keyCol, valueCol);
			languages.add(lang);
		}
		return languages;
	}

	private boolean containsLanguage(ArrayList<Language> languages, String locale)
	{
		if (languages == null) return false;
		for (Language language : languages)
		{
			if (language.locale.equalsIgnoreCase(locale)) return true;
		}
		return false;
	}

	private void appendLanguage(ArrayList<Language> languages, Language other)
	{
		if (languages == null) return;
		for (Language language : languages)
		{
			if (language.locale.equals(other.locale))
			{
				language.appendTable(other);
			}
		}
	}

	private LanguageRowMap buildRowMap(ArrayList<Language> languages)
	{
		if (languages == null) return null;
		LanguageRowMap rowMap = new LanguageRowMap();
		for (Language lang : languages)
		{
			for (Map.Entry<String, HashMap<String, String>> component : lang.tree.entrySet())
			{
				for (String key : component.getValue().keySet())
				{
					rowMap.addUnique(component.getKey(), key);
				}
			}
		}
		rowMap.buildRowMap();
		return rowMap;
	}

	public LanguageTable importExcelFiles()
	{
		if (files == null) return null;
		statNumEmptyCells = 0;
		HashSet<Sheet> sheets = new HashSet<Sheet>();
		for (File file : files)
		{
			if (file == null) continue;
			getSheetsFromExcel(sheets, file);
		}
		ArrayList<Language> sumLanguages = new ArrayList<Language>(32);
		for (Sheet sheet : sheets)
		{
			ArrayList<Language> languages = extractSheet(sheet);
			for (Language language : languages)
			{
				if (containsLanguage(sumLanguages, language.locale))
				{
					appendLanguage(sumLanguages, language);
				}
				else
				{
					sumLanguages.add(language);
				}
			}
		}

		if (sumLanguages.size() == 0) return null;

		LanguageRowMap rowMap = buildRowMap(sumLanguages);
		int numLangs = sumLanguages.size();
		String[][] data = new String[rowMap.rowMap.length][numLangs + 2];
		for (int i = 0; i < data.length; ++i)
		{
			data[i][0] = rowMap.rowMap[i][0];
			data[i][1] = rowMap.rowMap[i][1];
		}

		for (int c = 0; c < numLangs; ++c)
		{
			Language lang = sumLanguages.get(c);
			int r = 0;
			for (String[] row : rowMap.rowMap)
			{
				String value = lang.findValue(row[0], row[1]);
				if (value == null || value.isBlank() || value.isEmpty()) ++statNumEmptyCells;
				data[r][c + 2] = value;
				++r;
			}
		}
		String[] header = new String[sumLanguages.size()];
		for (int i = 0; i < sumLanguages.size(); ++i)
		{
			header[i] = sumLanguages.get(i).locale;
		}
		languageTable = new LanguageTable(header, data);
		return languageTable;
	}

	public void startTimeTrace()
	{
		statCalculationTime = System.currentTimeMillis();
	}

	public void stopTimeTrace()
	{
		statCalculationTime = System.currentTimeMillis() - statCalculationTime;
	}
}
