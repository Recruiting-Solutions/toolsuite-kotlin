package structs

class LanguageRowMap {

    @JvmField
    var rowMap: Array<Array<String>> = emptyArray()
    private val tree: MutableMap<String, MutableSet<String>> = mutableMapOf()

    fun buildRowMap() {
        val flat = tree.entries.flatMap { component -> component.value.map { key -> arrayOf(component.key, key) } }
        rowMap = flat.toTypedArray()
        quickSort(0, rowMap.size - 1)
    }

    private fun quickSort(low: Int, high: Int) {
        var i = low
        var j = high
        // Get the pivot element from the middle of the list
        val rowPivot = rowMap[low + (high - low) / 2]

        // Divide into two lists
        while (i <= j) {
            // If the current value from the left list is smaller than the pivot
            // element then get the next element from the left list
            while ((rowMap[i][0].compareTo(rowPivot[0]) == 0 && rowMap[i][1] < rowPivot[1]) || rowMap[i][0] < rowPivot[0]) {
                i++
            }
            // If the current value from the right list is larger than the pivot
            // element then get the next element from the right list
            while ((rowMap[j][0].compareTo(rowPivot[0]) == 0 && rowMap[j][1] > rowPivot[1]) || rowMap[j][0] > rowPivot[0]) {
                j--
            }

            // If we have found a value in the left list which is larger than
            // the pivot element and if we have found a value in the right list
            // which is smaller than the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(i, j)
                i++
                j--
            }
        }
        // Recursion
        if (low < j) quickSort(low, j)
        if (i < high) quickSort(i, high)
    }

    private fun exchange(a: Int, b: Int) {
        val row = rowMap[a];
        rowMap[a] = rowMap[b]
        rowMap[b] = row
    }

    fun addUnique(component: String, key: String) {
        val list = tree[component] ?: mutableSetOf<String>().also { tree[component] = it }
        list.add(key)
    }
}
