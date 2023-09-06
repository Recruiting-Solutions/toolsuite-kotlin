// https://stackoverflow.com/questions/21347647/how-to-combine-two-column-headers-in-jtable-in-swings

package widgets.table

import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

class GroupableTableHeader(model: TableColumnModel) : JTableHeader(model) {

    companion object {
        @Suppress("unused")
        private const val uiClassID = "GroupableTableHeaderUI"
    }

    private val columnGroups = mutableListOf<ColumnGroup>()

    init {
        setUI(GroupableTableHeaderUI())
        setReorderingAllowed(false)
        // setDefaultRenderer(MultiLineHeaderRenderer())
    }

    override fun updateUI() {
        setUI(GroupableTableHeaderUI())
    }

    override fun setReorderingAllowed(b: Boolean) {
        super.setReorderingAllowed(false)
    }

    fun addColumnGroup(g: ColumnGroup) {
        columnGroups.add(g)
    }

    fun getColumnGroups(col: TableColumn): List<ColumnGroup> {
        for (group in columnGroups) {
            val groups = group.getColumnGroups(col)
            if (groups.isNotEmpty()) {
                return groups
            }
        }
        return emptyList()
    }

    fun setColumnMargin() {
        val columnMargin = getColumnModel().columnMargin
        for (group in columnGroups) {
            group.columnMargin = columnMargin
        }
    }

}