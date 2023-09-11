package widgets

import java.awt.Color
import java.awt.Component

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

private val RENDERER: TableCellRenderer = DefaultTableCellRenderer()

class ExceliburCellRenderer : TableCellRenderer {

    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val c = RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        val obj = table?.getValueAt(row, column)?.toString()

        when {
            obj.isNullOrBlank() -> {
                c.background = if (isSelected) Color(255, 192, 192) else Color(255, 110, 110)
            }

            column < 2 -> {
                c.background = if (isSelected) Color(57, 135, 213) else Color(255, 255, 255, 32)
                c.foreground = if (isSelected) Color(255, 255, 255) else Color(187, 187, 187, 128)
            }

            else -> {
                c.background = if (isSelected) Color(57, 135, 213) else Color(0, 0, 0, 0)
                c.foreground = if (isSelected) Color(255, 255, 255) else Color(187, 187, 187)
            }
        }

        return c
    }
}