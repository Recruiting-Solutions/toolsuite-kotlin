package widgets.table

import java.awt.Dimension
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

class ColumnGroup(val headerValue: String?, var headerRenderer: TableCellRenderer? = null) {

    private val columns = mutableListOf<TableColumn>()
    private val groups = mutableListOf<ColumnGroup>()

    var columnMargin = 0
        set(value) {
            field = value
            for (columnGroup in groups) {
                columnGroup.columnMargin = value
            }
        }

    fun add(column: TableColumn) {
        columns.add(column)
    }

    fun add(group: ColumnGroup) {
        groups.add(group)
    }

    fun getColumnGroups(column: TableColumn): List<ColumnGroup> {
        if (!contains(column)) {
            return emptyList()
        }
        val result = mutableListOf<ColumnGroup>()
        result.add(this)
        if (columns.contains(column)) {
            return result
        }
        for (columnGroup in groups) {
            result.addAll(columnGroup.getColumnGroups(column))
        }
        return result
    }

    private fun contains(column: TableColumn): Boolean {
        if (columns.contains(column)) {
            return true
        }
        for (group in groups) {
            if (group.contains(column)) {
                return true
            }
        }
        return false
    }

    fun getSize(table: JTable): Dimension {
        val renderer = headerRenderer ?: table.tableHeader.defaultRenderer
        val comp = renderer.getTableCellRendererComponent(
                table,
                headerValue?.takeIf { it.isNotBlank() } ?: " ",
                false,
                false,
                -1,
                -1
        )
        val height = comp.preferredSize.height
        var width = 0
        for (columnGroup in groups) {
            width += columnGroup.getSize(table).width
        }
        for (tableColumn in columns) {
            width += tableColumn.width
            width += columnMargin
        }
        return Dimension(width, height)
    }
}