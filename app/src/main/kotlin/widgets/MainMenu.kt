package widgets

import core.App
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel

class MainMenu(owner: App) : JPanel() {
    companion object {
        private const val serialVersionUID = 1L
    }

    init {
        val grid = GridBagLayout()
        layout = grid
        val con = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            insets = Insets(8, 0, 8, 0)
        }
        val button = App.createButtonWithTextAndIcon("Excelibur", "icon_excel.png").apply {
            addActionListener { owner.addScreen(Excelibur(owner), "Excelibur") }
            preferredSize = Dimension(200, 50)
        }
        add(button, con)
        con.gridy = 1
        val helpButton = App.createButtonWithTextAndIcon("Documentation", "icon_help.png").apply {

            addActionListener { owner.showDocumentation() }
            preferredSize = Dimension(200, 50)
        }
        add(helpButton, con)
        owner.setStatus("Welcome to the Toolsuite...", App.NORMAL_MESSAGE)
    }
}