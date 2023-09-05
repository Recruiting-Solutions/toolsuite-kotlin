package structs

class Language(@JvmField val identifier: LanguageIdentifier) {

    private val _tree: MutableMap<String, MutableMap<String, String>> = mutableMapOf();
    val tree: Map<String, Map<String, String>>
        get() = _tree;

    fun isSameLanguageIdentifier(otherLanguage: Language): Boolean {
        return identifier.brand.equals(otherLanguage.identifier.brand, ignoreCase = true)
                && identifier.locale.equals(otherLanguage.identifier.locale, ignoreCase = true)
    }

    fun addUnique(component: String, key: String, value: String) {
        val keyMap = _tree[component] ?: mutableMapOf<String, String>().also { _tree[component] = it; }
        if (keyMap.containsKey(key)) return
        keyMap[key] = value
}

    fun findValue(component: String, key: String): String {
        val componentMap = tree[component] ?: return ""
        val value = componentMap[key] ?: return ""
        return value
    }

    fun appendTable(other: Language) {
        for (componentMap in other.tree.entries) {
            for (keyMap in componentMap.value.entries) {
                addUnique(componentMap.key, keyMap.key, keyMap.value)
            }
        }
    }
}
