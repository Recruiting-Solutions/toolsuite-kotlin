package widgets.table

import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicTableHeaderUI

class GroupableTableHeaderUI : BasicTableHeaderUI() {

    private fun getHeader(): GroupableTableHeader {
        return header as GroupableTableHeader
    }

    override fun paint(g: Graphics, c: JComponent) {
        val clipBounds = g.clipBounds
        if (header.columnModel.columnCount == 0) {
            return
        }
        var column = 0
        val size = header.size
        val cellRect = Rectangle(0, 0, size.width, size.height)
        val groupSizeMap = mutableMapOf<ColumnGroup, Rectangle>()

        val enumeration = header.columnModel.columns
        while (enumeration.hasMoreElements()) {
            cellRect.height = size.height
            cellRect.y = 0
            val aColumn = enumeration.nextElement()
            val groups = getHeader().getColumnGroups(aColumn)
            var groupHeight = 0
            for (group in groups) {
                val groupRect = groupSizeMap[group] ?: Rectangle(cellRect).also {
                    val d = group.getSize(header.table)
                    it.width = d.width
                    it.height = d.height
                    groupSizeMap[group] = it
                }
                paintCell(g, groupRect, group)
                groupHeight += groupRect.height
                cellRect.height = size.height - groupHeight
                cellRect.y = groupHeight
            }
            cellRect.width = aColumn.width;
            if (cellRect.intersects(clipBounds)) {
                paintCell(g, cellRect, column)
            }
            cellRect.x += cellRect.width
            column++
        }
    }

    private fun paintCell(g: Graphics, cellRect: Rectangle, columnIndex: Int) {
        val aColumn = header.columnModel.getColumn(columnIndex)
        val renderer = aColumn.headerRenderer ?: getHeader().defaultRenderer
        val c = renderer.getTableCellRendererComponent(header.table, aColumn.headerValue, false, false, -1, columnIndex)

        c.setBackground(UIManager.getColor("control"))

        rendererPane.paintComponent(g, c, header, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true)
    }

    private fun paintCell(g: Graphics, cellRect: Rectangle, cGroup: ColumnGroup) {
        val renderer = cGroup.headerRenderer ?: getHeader().defaultRenderer

        val component = renderer.getTableCellRendererComponent(header.table, cGroup.headerValue, false, false, -1, -1)
        rendererPane.paintComponent(g, component, header, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true);
    }

    private fun getHeaderHeight(): Int {
        var headerHeight = 0
        val columnModel = header.columnModel
        for (column in 0..<columnModel.columnCount) {
            val aColumn = columnModel.getColumn(column)
            val renderer = aColumn.headerRenderer ?: getHeader().defaultRenderer

            val comp = renderer.getTableCellRendererComponent(header.table, aColumn.headerValue, false, false, -1, column)
            var cHeight = comp.preferredSize.height
            val groups = getHeader().getColumnGroups(aColumn)
            for (group in groups) {
                cHeight += group.getSize(header.table).height
            }
            headerHeight = headerHeight.coerceAtLeast(cHeight)
        }
        return headerHeight
    }

    override fun getPreferredSize(c: JComponent): Dimension {
        var width = 0;
        val enumeration = header.columnModel.columns
        while (enumeration.hasMoreElements()) {
            val aColumn = enumeration.nextElement()
            width += aColumn.preferredWidth
        }
        return createHeaderSize(width)
    }

    private fun createHeaderSize(width: Int): Dimension {
        val columnModel = header.columnModel
        val newWidth = width.toLong() + columnModel.columnMargin * columnModel.columnCount
        return Dimension(newWidth.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), getHeaderHeight())
    }
}
