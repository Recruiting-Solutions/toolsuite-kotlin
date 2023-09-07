package core

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.GridLayout
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JCheckBoxMenuItem
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf

import widgets.MainMenu
import widgets.Throbber
import kotlin.concurrent.thread

class App : JFrame() {

    companion object {

        const val TOOL_NAME = "Toolsuite 1.0.4"
        const val NORMAL_MESSAGE = 0
        const val WARNING_MESSAGE = 1
        const val ERROR_MESSAGE = 2

        private const val serialVersionUID = 1L

        private var INSTANCE: App? = null
        fun get(): App {
            return INSTANCE ?: App().also { INSTANCE = it }
        }

        fun loadResource(resource: String): BufferedImage? {
            // Try 868.154. all other ways to place, load and put dependencies for resources
            // DID NOT WORK.
            // FUCK JAVA. This is used for a packaged build
            val input = App::class.java.getResourceAsStream("/resources/$resource")
            // This is used inside the IDE. Fuck java.
                    ?: App::class.java.getResourceAsStream("/$resource")
                    ?: return null
            try {
                return ImageIO.read(input)
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            return null
        }

        fun createButtonWithTextAndIcon(text: String, iconPath: String): JButton {
            val url = loadResource(iconPath)
            return JButton(text, url?.let { ImageIcon(it) }).apply {
                setBackground(Color(31, 144, 255))
                setForeground(Color.WHITE)
                preferredSize = Dimension(200, 32)
                setHorizontalAlignment(JButton.LEFT)
                setIconTextGap(24)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            get()
        }
    }

    private val layout = BorderLayout()

    private val prefs = Preferences.userRoot().node(App::class.java.getSimpleName() + "-" + "MyExcelibur")
    private val statusBar = JLabel()
    private val colorLabel = JPanel(BorderLayout())

    private var useDarkMode = false
    private val throbber by lazy {
        val ico = ImageIcon(loadResource("icon_loading_circle.png"))
        Throbber(ico.image, 20, 20, 2, 2)
    }

    private val itemDark = JCheckBoxMenuItem("Dark Theme").apply {
        addActionListener { setThemeDark() }
    }
    private val itemLight = JCheckBoxMenuItem("Light Theme").apply {

        addActionListener { setThemeLight() }
    }

    init {
        setDefaultCloseOperation(EXIT_ON_CLOSE)
        createMenuBar()
        registerFrame(100, 100, 800, 600, MAXIMIZED_BOTH)
        contentPane.setLayout(layout)

        ToolTipManager.sharedInstance().initialDelay = 100
        ToolTipManager.sharedInstance().dismissDelay = 10000

        val b = CompoundBorder(statusBar.border, EmptyBorder(4, 8, 4, 8))
        statusBar.setBorder(b)

        val panel = JPanel()
        panel.setLayout(BorderLayout())

        panel.add(colorLabel, BorderLayout.WEST)
        panel.add(statusBar, BorderLayout.CENTER)
        statusBar.setBackground(Color(31, 144, 255, 255))
        statusBar.setForeground(Color.WHITE)
        statusBar.setOpaque(true)
        colorLabel.setOpaque(true)
        colorLabel.setBackground(Color.RED)
        colorLabel.preferredSize = Dimension(24, 24)

        colorLabel.add(throbber, BorderLayout.CENTER)

        addScreen(MainMenu(this), TOOL_NAME)
        contentPane.add(panel, BorderLayout.SOUTH)

        isVisible = true

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            prefs.putInt("X", location.x)
            prefs.putInt("Y", location.y)
            prefs.putInt("W", size.width)
            prefs.putInt("H", size.height)
            prefs.putInt("EXTENDED_STATE", extendedState)
            prefs.putBoolean("USE_DARK_MODE", useDarkMode)
        })
    }

    private fun createMenuBar() {
        val menuBar = JMenuBar()
        jMenuBar = menuBar

        // Help Menu
        val menuHelp = JMenu("Help")
        menuBar.add(menuHelp)
        val itemDocs = JMenuItem("Documentation")
        itemDocs.addActionListener { showDocumentation() }
        menuHelp.add(itemDocs)

        // Theme Menu
        val menuWindow = JMenu("Window")
        menuBar.add(menuWindow)
        val appearance = JMenu("Appearence")
        menuWindow.add(appearance)
        appearance.add(itemDark)
        appearance.add(itemLight)
    }

    private fun setThemeDark() {
        itemDark.setSelected(true)
        itemLight.setSelected(false)
        useDarkMode = true
        try {
            UIManager.setLookAndFeel(FlatDarkLaf())
            SwingUtilities.updateComponentTreeUI(this)
        } catch (e: UnsupportedLookAndFeelException) {
            setStatus(e.localizedMessage, ERROR_MESSAGE)
        }
    }

    private fun setThemeLight() {
        itemDark.setSelected(false)
        itemLight.setSelected(true)
        useDarkMode = false
        try {
            UIManager.setLookAndFeel(FlatLightLaf())
            SwingUtilities.updateComponentTreeUI(this)
        } catch (e: UnsupportedLookAndFeelException) {
            setStatus(e.localizedMessage, ERROR_MESSAGE)
        }
    }

    fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            throbber.startAnimation()
        } else {
            throbber.stopAnimation()
        }
    }

    fun showDocumentation() {
        val message = "<html><body><h1>Excelibur</h1><br>You can access this window all the time via the <b bgcolor=\"#282828\">Help</b> menu in the top left corner.<br>" +
                "<h3>Preparation:</h3><br><ul><li>Follow the naming scheme shown in the table below to set up the excel sheet(s)</li><li>Use <b bgcolor=\"#282828\">HTML ISO locales</b>" +
                "<li>Rows with an empty <b bgcolor=\"#282828\">component</b> or <b bgcolor=\"#282828\">key</b> cell are skipped during the import</li>" +
                "<li>When importing excel sheets this tool finds all data automatically. For that the tool <br>searches up to <b bgcolor=\"#282828\">column=AS</b> and <b bgcolor=\"#282828\">row=20</b> for any locales, 'component' and 'key' in every file and sheet</li></ul>" +
                "<h3>Result:</h3><ul><li>All found components and keys are extracted and sorted with the values in every language</li></ul>"

        val mainPanel = JPanel(BorderLayout())
        val infoText = JLabel()
        infoText.setText(message)
        val b = CompoundBorder(infoText.border, EmptyBorder(8, 0, 24, 0))
        infoText.setBorder(b)
        mainPanel.add(infoText, BorderLayout.NORTH)

        val grid = JPanel(GridLayout(3, 4, 1, 1))
        grid.setOpaque(true)
        grid.setBackground(Color.RED)

        val colorHeader = Color(230, 230, 230)
        val colorHeaderText = Color.BLACK

        val headers = arrayOf("component", "key", "de_DE", "en_US")
        for (cell in headers) {
            val label = JLabel(cell)
            label.setOpaque(true)
            label.setBackground(colorHeader)
            label.setForeground(colorHeaderText)
            grid.add(label)
        }

        val colorCell = Color.WHITE

        val cells = arrayOf("job_dialog", "loading_heading", "Beispieltext 1", "Exampletext 2", "job_dialog", "loading_title", "Beispieltext 1", "Exampletext 2")
        for (cell in cells) {
            val label = JLabel(cell)
            label.setOpaque(true)
            label.setBackground(colorCell)
            label.setForeground(colorHeaderText)
            grid.add(label)
        }

        mainPanel.add(grid, BorderLayout.CENTER)

        JOptionPane.showMessageDialog(contentPane, mainPanel, "Documentation", JOptionPane.INFORMATION_MESSAGE)
    }

    fun addScreen(panel: JPanel, title: String) {
        setTitle(title)
        layout.getLayoutComponent(BorderLayout.CENTER)?.let {
            contentPane.remove(it)
        }
        contentPane.add(panel, BorderLayout.CENTER)
        contentPane.revalidate()
        contentPane.repaint()
    }

    private fun registerFrame(defaultX: Int, defaultY: Int, defaultW: Int, defaultH: Int, defaultExtendedState: Int) {
        // We need to clamp so that the frame never opens outside of the monitor
        // boundaries
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        var maxW = 0
        var maxH = 0
        for (monitor in env.screenDevices) {
            maxW += monitor.getDisplayMode().width
            if (monitor.getDisplayMode().height > maxH) maxH = monitor.getDisplayMode().height
        }

        val w = prefs.getInt("W", defaultW).coerceAtLeast(500).coerceAtMost(maxW)
        val h = prefs.getInt("H", defaultH).coerceAtLeast(500).coerceAtMost(maxH)

        val x = prefs.getInt("X", defaultX).takeIf { x + w <= maxW } ?: (maxW - w)
        val y = prefs.getInt("Y", defaultY).takeIf { y + h <= maxH } ?: (maxH - h)

        setLocation(x, y)
        setSize(w, h)

        val extendedState = prefs.getInt("EXTENDED_STATE", defaultExtendedState)
        useDarkMode = prefs.getBoolean("USE_DARK_MODE", true)

        if (useDarkMode) {
            setThemeDark()
        } else {
            setThemeLight()
        }

        setExtendedState(extendedState)
        minimumSize = Dimension(500, 500)
    }

    fun setStatus(message: String, type: Int) {
        statusBar.setText(message)
        when (type) {
            WARNING_MESSAGE -> colorLabel.setBackground(Color(178, 139, 35))
            ERROR_MESSAGE -> colorLabel.setBackground(Color(255, 110, 110))
            else -> colorLabel.setBackground(Color(65, 125, 88))
        }
    }
}
