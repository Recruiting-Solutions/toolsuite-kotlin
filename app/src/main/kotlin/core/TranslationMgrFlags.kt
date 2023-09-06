package core

object TranslationMgrFlags {
    enum class Import {
        USE_HYPERLINK_IF_AVAILABLE
    }

    enum class Export {
        CONCAT_COMPONENT_AND_KEY, DONT_EXPORT_EMPTY_VALUES
    }

    enum class FolderNaming {
        LOCALE_BRAND, BRAND_LOCALE, BRAND_AND_LOCALE_AS_SUBFOLDER;

        companion object {
            fun getValue(i: Int): FolderNaming {
                for ((index, rule) in entries.withIndex()) {
                    if (i == index)
                        return rule;
                }
                return LOCALE_BRAND;
            }
        }
    }
}
