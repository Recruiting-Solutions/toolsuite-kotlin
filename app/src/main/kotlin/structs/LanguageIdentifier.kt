package structs

data class LanguageIdentifier(val brand: String, val locale: String) {
    companion object {
        fun sortHeader(identifiers: Array<LanguageIdentifier>, languages: MutableList<Language>) {
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
}
