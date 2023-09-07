package structs

class LanguageTable(val identifiers: Array<LanguageIdentifier>, val jTableData: Array<Array<String?>>) {

    fun getIdentifierIndex(otherIdentifier: LanguageIdentifier): Int {
        return identifiers.indexOfFirst { it.brand == otherIdentifier.brand && it.locale == otherIdentifier.locale }
    }

    fun getJTableHeader(): Array<String> {
        return arrayOf("Component", "Key", *identifiers.map { it.locale }.toTypedArray())
    }
}
