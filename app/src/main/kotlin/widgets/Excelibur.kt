package widgets

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import java.util.ArrayList

import javax.swing.Action
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

import core.App
import core.TranslationMgr
import core.TranslationMgrFlags
import core.TranslationMgrFlags.FolderNaming.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import structs.LanguageIdentifier
import structs.LanguageTable
import widgets.table.ColumnGroup
import widgets.table.GroupableTableHeader
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class Excelibur(private val owner: App) : JPanel(), CoroutineScope {

    companion object {
        private const val serialVersionUID = 1L
    }

    private val translationMgr = TranslationMgr()
    private var table = JTable(DefaultTableModel(arrayOf("Component", "Key", "Locale"), 0)).apply { setFillsViewportHeight(true) }
    private val fileList = JList<String>();

    private val importButton: JButton = App.createButtonWithTextAndIcon("Choose Excel files...", "icon_import.png").apply {
        setToolTipText("Import files via a selection dialog. If any new file is imported the current selection of files will be removed and the table content is refreshed.");
        addActionListener { openInputDialog() }
    }
    private val exportButton: JButton = App.createButtonWithTextAndIcon("Export", "icon_export.png").apply {
        setToolTipText("Bulk export every language to a .json. The locale is appended to the filename so 'translation' changes to 'translation_de_DE' etc.")
        addActionListener { openOutputDialog() }
    }
    private val returnButton = App.createButtonWithTextAndIcon("Back", "icon_return.png").apply {
        addActionListener { owner.addScreen(MainMenu(owner), App.TOOL_NAME) }
    }
    private val reloadButton: JButton = App.createButtonWithTextAndIcon("Reload tables...", "icon_refresh.png").apply {
        setToolTipText("Reimport the selected files. Be sure to have all imported Excel files closed or they won't be imported as Excel blocks the files when opened.")
        addActionListener { updateTableView() }
    }

    private var lastImportFolder = ""
    private var lastExportFolder = ""

    private val checkBoxAutoResize = JCheckBox("Auto Resize Table").apply {
        setSelected(true)
        addItemListener { updateTableAutoResizing() }
        setToolTipText("Change how the data is displayed in the table. Either fit to the window's size (enabled) or match each column's width to it's content (disabled)")
    }
    private val checkBoxMergeCompAndKey = JCheckBox("Concat. 'Component' and 'Key'").apply {
        setSelected(false)
        setToolTipText("Concatenates component and key. That means 'dialog' and 'heading' become 'dialog_heading'.\nThis eventually reduces the json tree depth by 1")
    }
    private val checkBoxUseHyperlinkIfAvailable = JCheckBox("Get hyperlink").apply {
        setSelected(false)
        setToolTipText("Replaces cell content with hyperlink if any")
    }
    private val checkDoNotExportEmptyCells = JCheckBox("Skip empty values").apply {
        setSelected(true)
        setToolTipText("Don't export key value pairs with empty values. This is for each language individually")
    }

    private val comboFolderNaming: JComboBox<String> = createOutputFolderComboBox()

    private val horSplit = JSplitPane().apply {
        setOrientation(JSplitPane.HORIZONTAL_SPLIT)
        setEnabled(false)
        setDividerSize(0)
        leftComponent = JSplitPane().apply {
            setOrientation(JSplitPane.VERTICAL_SPLIT);
            setEnabled(false);
            setDividerSize(0);

            bottomComponent = JPanel().apply {
                setLayout(BorderLayout())

                val infoImportedFiles = JLabel("Imported file(s):").also {
                    it.setBorder(CompoundBorder(it.border, EmptyBorder(4, 4, 4, 4)))
                }
                add(infoImportedFiles, BorderLayout.NORTH)

                add(JScrollPane(fileList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER)

                val importPanel = JPanel(GridLayout(2, 1, 4, 4)).apply {
                    add(reloadButton);
                    add(importButton);
                }
                add(importPanel, BorderLayout.SOUTH);
            }

            topComponent = JPanel().apply {
                setLayout(BorderLayout())
                add(returnButton, BorderLayout.NORTH)

                val infoPanel = JPanel().also {
                    it.setLayout(GridLayout(12, 1, 8, 0))
                    it.setBorder(CompoundBorder(it.border, EmptyBorder(4, 4, 4, 4)))

                    it.add(JLabel("View settings:"))
                    it.add(checkBoxAutoResize)
                    it.add(JLabel(""))
                    it.add(JLabel("Import settings:"))
                    it.add(checkBoxUseHyperlinkIfAvailable)
                    it.add(JLabel(""))
                    it.add(JLabel("Export settings:"))
                    it.add(checkBoxMergeCompAndKey)
                    it.add(checkDoNotExportEmptyCells)

                    val outputFolderRuleLabel = JLabel("Output folder (struct):").apply {
                        setForeground(Color(128, 128, 128))
                    }
                    it.add(outputFolderRuleLabel)

                    it.add(comboFolderNaming)
                    it.add(JLabel())
                }
                add(infoPanel, BorderLayout.CENTER)
            }

            minimumSize = Dimension(200, 5)
            maximumSize = Dimension(200, 2000)
            preferredSize = Dimension(200, 2000)
        }
        rightComponent = JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
    }

    init {
        owner.setStatus("Welcome to Excelibur..", App.NORMAL_MESSAGE)
        setLayout(BorderLayout())

        add(horSplit, BorderLayout.CENTER);

        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            setEnabled(false);
            setDividerSize(0);

            val flow = FlowLayout(FlowLayout.RIGHT).apply {
                hgap = 0
                vgap = 0
            }

            leftComponent = JPanel(flow).apply {
                setAlignmentX(LEFT_ALIGNMENT)
            }
            rightComponent = JPanel(flow).apply {
                add(exportButton)
                setAlignmentX(RIGHT_ALIGNMENT)
            }

        }
        add(split, BorderLayout.SOUTH)
        enableUserInput(true)
    }

    private fun createDualHeaderTable(langTable: LanguageTable) {
        val localeHeader = mutableListOf<String>()
        localeHeader.add("Component")
        localeHeader.add("Key")
        val brandHeader = mutableListOf<String>()
        val localeCount = mutableListOf<Int>()
        var previousBrand = ""
        for (identifier in langTable.identifiers) {
            if (previousBrand != identifier.brand) {
                brandHeader.add(identifier.brand)
                localeCount.add(1)
                previousBrand = identifier.brand
            } else {
                val i = localeCount.size - 1
                var count = localeCount[i]
                localeCount[i] = ++count
            }
            localeHeader.add(identifier.locale)
        }

        val dm = DefaultTableModel()
        dm.setDataVector(langTable.jTableData, localeHeader.toTypedArray())

        table = object : JTable(dm) {
            override fun createDefaultTableHeader(): JTableHeader {
                return GroupableTableHeader(columnModel)
            }
        };

        val cm = table.columnModel
        val header = table.tableHeader as GroupableTableHeader
        var offset = 2
        for (i in 0..<localeCount.size) {
            val group = ColumnGroup(brandHeader[i])
            for (j in 0..<localeCount[i]) {
                group.add(cm.getColumn(offset + j))
            }
            offset += localeCount[i]
            header.addColumnGroup(group)
        }

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.tableHeader.setReorderingAllowed(false)
        table.setDefaultRenderer(Object::class.java, ExceliburCellRenderer())
        table.setFillsViewportHeight(true)
        horSplit.rightComponent = JScrollPane(table)
        table.setRowSelectionAllowed(true)
        updateTableAutoResizing()
    }

    private fun createOutputFolderComboBox(): JComboBox<String> {
        val list = Array(TranslationMgrFlags.FolderNaming.entries.size) {
            val rule = TranslationMgrFlags.FolderNaming.entries[it];
            when (rule) {
                LOCALE_BRAND -> "locale_Brand"
                BRAND_LOCALE -> "Brand_locale"
                BRAND_AND_LOCALE_AS_SUBFOLDER -> "Brand / locale"
            }
        }
        return JComboBox<String>(list)
    }

    private fun openInputDialog() {
        owner.setStatus("Choosing files to import...", App.NORMAL_MESSAGE)
        val filter = FileNameExtensionFilter("Microsoft Excel Documents (*.xlsx)", "xlsx")

        if (lastImportFolder.isBlank() || lastImportFolder.isEmpty()) {
            val userDir = System.getProperty("user.home")
            lastImportFolder = "$userDir/Desktop"
        }

        JFileChooser(lastImportFolder).apply {
            setMultiSelectionEnabled(true)
            setFileFilter(filter)
            preferredSize = Dimension(800, 600)

            // This sets the default folder view to 'details'
            actionMap.get("viewTypeDetails").apply {
                actionPerformed(null)
            }

            val choice = showOpenDialog(this)
            when (choice) {
                JFileChooser.APPROVE_OPTION -> {
                    if (selectedFiles.isNotEmpty()) {
                        translationMgr.files = selectedFiles
                        lastImportFolder = translationMgr.files[0].getParent()
                        updateListView()
                        updateTableView()
                    }
                }

                JFileChooser.CANCEL_OPTION -> {
                    owner.setStatus("Aborted import...", App.NORMAL_MESSAGE)
                }
            }
        }
    }


    private fun openOutputDialog() {
        if (translationMgr.getNumSelectedFiles() == 0) {
            JOptionPane.showMessageDialog(this, "Please import Excel sheets first", "Info", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        owner.setStatus("Selecting output folder...", App.NORMAL_MESSAGE)

        if (lastExportFolder.isBlank() || lastExportFolder.isEmpty()) {
            val userDir = System.getProperty("user.home")
            lastExportFolder = "$userDir/Desktop"
        }
        val chooser = JFileChooser(lastExportFolder).apply {
            setSelectedFile(File("translations"))
            preferredSize = Dimension(800, 600)

            // This sets the default folder view to 'details'
            actionMap.get("viewTypeDetails").apply {
                actionPerformed(null)
            }
        }
        val choice = chooser.showSaveDialog(this)
        translationMgr.startTimeTrace();
        enableUserInput(false);
        if (choice == JFileChooser.APPROVE_OPTION) {
            launch(Dispatchers.IO) {
                val (outputFolder, fileName) = chooser.selectedFile.toString().let {
                    val i = it.lastIndexOf(System.getProperty("file.separator"))
                    it.substring(0, i) to it.substring(i + 1, it.length)
                }
                lastExportFolder = chooser.selectedFile.getParent()
                val success = exportData(outputFolder, fileName)
                if (success) {
                    translationMgr.stopTimeTrace()
                    val seconds = translationMgr.statCalculationTime / 1000.0
                    val secondsString = String.format("%.2f", seconds)
                    owner.setStatus("Sucessfully exported Excel sheet(s) within " + secondsString + "s...", App.NORMAL_MESSAGE)
                }
                enableUserInput(true)
            }
        } else {
            owner.setStatus("Aborted export...", App.NORMAL_MESSAGE)
            enableUserInput(true)
        }
    }

    private fun updateListView() {
        fileList.setVisibleRowCount(-1)
        fileList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val fileNames = Array<String>(translationMgr.getNumSelectedFiles()) {
            val file = translationMgr.files[it]
            translationMgr.getFileName(file.getName())
        }
        fileList.setListData(fileNames)
        fileList.setSelectedIndex(0)
    }

    private fun updateTableView() {
        if (translationMgr.getNumSelectedFiles() == 0) return

        translationMgr.startTimeTrace();
        owner.setStatus("Extracting data...", App.NORMAL_MESSAGE)

        enableUserInput(false)
        owner.setLoading(true)
        launch(Dispatchers.IO) {
            importData()
            enableUserInput(true)
            owner.setLoading(false)
        }
    }

    private fun importData() {
        translationMgr.setFlag(TranslationMgrFlags.Import.USE_HYPERLINK_IF_AVAILABLE, checkBoxUseHyperlinkIfAvailable.isSelected)
        val languageTable = translationMgr.importExcelFiles()
        val comp = horSplit.rightComponent

        if (languageTable == null) {
            owner.setStatus("No data found inside Excel file(s). Did you set it up properly? Click 'Help' to read the documentation on how to setup an Excel file correctly.", App.ERROR_MESSAGE)
            val model = DefaultTableModel(arrayOf("Component", "Key", "Locale"), 0)
            table = JTable(model).apply {
                setFillsViewportHeight(true)
            }
            updateTableAutoResizing()
            return;
        }

        if (comp != null) horSplit.remove(comp)

        createDualHeaderTable(languageTable);

        translationMgr.stopTimeTrace()
        val seconds = translationMgr.statCalculationTime / 1000.0
        val secondsString = String.format("%.2f", seconds)
        when (translationMgr.statNumEmptyCells) {
            0 -> owner.setStatus("Sucessfully imported Excel sheet(s) within " + secondsString + "s...", App.NORMAL_MESSAGE)
            1 -> owner.setStatus(
                    "Sucessfully imported Excel sheet(s) within " + secondsString + "s but there was " + translationMgr.statNumEmptyCells + " empty cell found! Watch out for the red marked cells!",
                    App.WARNING_MESSAGE
            )

            else -> owner.setStatus(
                    "Sucessfully imported Excel sheet(s) within  " + secondsString + "s but there were " + translationMgr.statNumEmptyCells + " empty cells found! Watch out for the red marked cells!",
                    App.WARNING_MESSAGE
            )
        }
    }

    private fun enableUserInput(bEnabled: Boolean) {
        val bAnyFilesImported = translationMgr.getNumSelectedFiles() > 0 && table.getRowCount() > 0
        // prevent export if no files are in the "imported" list
        exportButton.setEnabled(bEnabled && bAnyFilesImported)
        reloadButton.setEnabled(bEnabled && bAnyFilesImported)
        importButton.setEnabled(bEnabled)
        returnButton.setEnabled(bEnabled)
        checkBoxAutoResize.setEnabled(bEnabled)
        checkBoxMergeCompAndKey.setEnabled(bEnabled)
        checkDoNotExportEmptyCells.setEnabled(bEnabled)
        checkBoxUseHyperlinkIfAvailable.setEnabled(bEnabled)
        comboFolderNaming.setEnabled(bEnabled)
    }

    private fun exportData(outputFolder: String, fileName: String): Boolean {
        translationMgr.setFlag(TranslationMgrFlags.Export.CONCAT_COMPONENT_AND_KEY, checkBoxMergeCompAndKey.isSelected)
        translationMgr.setFlag(TranslationMgrFlags.Export.DONT_EXPORT_EMPTY_VALUES, checkDoNotExportEmptyCells.isSelected)

        translationMgr.folderNamingType = TranslationMgrFlags.FolderNaming.getValue(comboFolderNaming.getSelectedIndex())
        return translationMgr.export2Json(outputFolder, fileName)
    }

    fun updateTableAutoResizing() {
        if (checkBoxAutoResize.isSelected) {
            for (column in 0..<table.columnCount) {
                val tableColumn = table.columnModel.getColumn(column)
                val preferredWidth = 3000 // tableColumn.minWidth
                tableColumn.setPreferredWidth(preferredWidth)
            }
            table.autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        } else {
            table.autoResizeMode = JTable.AUTO_RESIZE_OFF
            for (column in 0..<table.columnCount) {
                val tableColumn = table.columnModel.getColumn(column)
                var preferredWidth = tableColumn.minWidth
                val maxWidth = tableColumn.maxWidth.coerceAtMost(2000)

                for (row in 0..<table.getRowCount()) {
                    val cellRenderer = table.getCellRenderer(row, column)
                    val c = table.prepareRenderer(cellRenderer, row, column)
                    val width = c.preferredSize.width + table.intercellSpacing.width
                    preferredWidth = preferredWidth.coerceAtLeast(width)

                    // We've exceeded the maximum width, no need to check other rows
                    if (preferredWidth >= maxWidth) {
                        preferredWidth = maxWidth;
                        break;
                    }
                }

                tableColumn.setPreferredWidth(preferredWidth)
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}
