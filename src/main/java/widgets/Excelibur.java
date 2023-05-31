package widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import core.App;
import core.TranslationMgr;
import core.TranslationMgrFlags;
import structs.LanguageTable;

public class Excelibur extends JPanel
{

	private TranslationMgr translationMgr = new TranslationMgr();
	private static final long serialVersionUID = 1L;

	App owner;
	JTable table;
	JList<String> fileList = new JList<String>();

	JButton importButton, exportButton, returnButton, reloadButton;

	JSplitPane horSplit = new JSplitPane();

	JCheckBox checkBoxAutoResize = new JCheckBox("Auto Resize Table");
	JCheckBox checkBoxMergeCompAndKey = new JCheckBox("Concat. 'Component' and 'Key'");
	JCheckBox checkAppendBrandToLocale = new JCheckBox("Append 'Brand' to 'Locale'");
	JCheckBox checkDoNotExportEmptyCells = new JCheckBox("Do NOT export empty values.");

	public Excelibur(App owner)
	{
		this.owner = owner;
		owner.setStatus("Welcome to Excelibur..", App.NORMAL_MESSAGE);
		setLayout(new BorderLayout());
		returnButton = App.createButtonWithTextAndIcon("Back", "icon_return.png");
		returnButton.addActionListener(e -> owner.addScreen(new MainMenu(owner), "Toolsuite"));

		// TABLE
		DefaultTableModel model = new DefaultTableModel(new String[] { "Component", "Key", "Locale" }, 0);
		table = new JTable(model);
		table.setFillsViewportHeight(true);

		JPanel infoPanel = new JPanel();
		GridLayout grid = new GridLayout(9, 1, 8, 8);
		infoPanel.setLayout(grid);
		CompoundBorder b = new CompoundBorder(infoPanel.getBorder(), new EmptyBorder(4, 4, 4, 4));
		infoPanel.setBorder(b);

		checkBoxAutoResize.setSelected(true);
		checkBoxAutoResize.addItemListener(e -> updateTableAutoResizing());
		checkBoxAutoResize.setToolTipText("Change how the data is displayed in the table. Either fit to the window's size (enabled) or match each column's width to it's content (disabled)");

		checkBoxMergeCompAndKey.setSelected(false);
		checkBoxMergeCompAndKey.setToolTipText("Concatenates component and key. That means 'dialog' and 'heading' become 'dialog_heading'.\nThis eventually reduces the json tree depth by 1");
		checkDoNotExportEmptyCells.setSelected(true);
		checkDoNotExportEmptyCells.setToolTipText("Don't export key value pairs with empty values. This is for each language individually");
		checkAppendBrandToLocale.setSelected(true);
		checkAppendBrandToLocale.setToolTipText("Appends the brand in cell A1 to the locale. If cell is empty it is like this option is disabled.");

		infoPanel.add(new JLabel("View settings:"));
		infoPanel.add(checkBoxAutoResize);
		infoPanel.add(new JLabel(""));
		infoPanel.add(new JLabel("Import settings:"));
		infoPanel.add(checkAppendBrandToLocale);
		infoPanel.add(new JLabel(""));
		infoPanel.add(new JLabel("Export settings:"));
		infoPanel.add(checkBoxMergeCompAndKey);
		infoPanel.add(checkDoNotExportEmptyCells);

		JLabel infoImportedFiles = new JLabel("Imported file(s):");
		b = new CompoundBorder(infoImportedFiles.getBorder(), new EmptyBorder(4, 4, 4, 4));
		infoImportedFiles.setBorder(b);

		JPanel filePane = new JPanel();
		filePane.setLayout(new BorderLayout());
		filePane.add(infoImportedFiles, BorderLayout.NORTH);
		filePane.add(new JScrollPane(fileList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

		JSplitPane leftSplitPane = new JSplitPane();
		leftSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftSplitPane.setEnabled(false);
		leftSplitPane.setDividerSize(0);

		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new BorderLayout());
		tempPanel.add(returnButton, BorderLayout.NORTH);
		tempPanel.add(infoPanel, BorderLayout.CENTER);

		leftSplitPane.setBottomComponent(filePane);
		leftSplitPane.setTopComponent(tempPanel);

		leftSplitPane.setMinimumSize(new Dimension(200, 5));
		leftSplitPane.setMaximumSize(new Dimension(200, 2000));
		leftSplitPane.setPreferredSize(new Dimension(200, 2000));

		horSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		horSplit.setEnabled(false);
		horSplit.setDividerSize(0);
		horSplit.setLeftComponent(leftSplitPane);
		horSplit.setRightComponent(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		reloadButton = App.createButtonWithTextAndIcon("Reload tables...", "icon_refresh.png");
		reloadButton.setToolTipText("Reimport the selected files. Be sure to have all imported Excel files closed or they won't be imported as Excel blocks the files when opened.");
		reloadButton.addActionListener(e -> updateTableView());
		importButton = App.createButtonWithTextAndIcon("Choose Excel files...", "icon_import.png");
		importButton.setToolTipText("Import files via a selection dialog. If any new file is imported the current selection of files will be removed and the table content is refreshed.");
		importButton.addActionListener(e -> openInputDialog());
		exportButton = App.createButtonWithTextAndIcon("Export", "icon_export.png");
		exportButton.setToolTipText("Bulk export every language to a .json. The locale is appended to the filename so 'translation' changes to 'translation_de_DE' etc.");
		exportButton.addActionListener(e -> openOutputDialog());

		JPanel importPanel = new JPanel(new GridLayout(2, 1, 4, 4));
		importPanel.add(reloadButton);
		importPanel.add(importButton);

		filePane.add(importPanel, BorderLayout.SOUTH);
		FlowLayout flow = new FlowLayout(FlowLayout.RIGHT);
		flow.setHgap(0);
		flow.setVgap(0);

		JPanel leftButtonPanel = new JPanel(flow);
		leftButtonPanel.setAlignmentX(LEFT_ALIGNMENT);

		JPanel rightButtonPanel = new JPanel(flow);
		rightButtonPanel.add(exportButton);
		rightButtonPanel.setAlignmentX(RIGHT_ALIGNMENT);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setEnabled(false);
		split.setDividerSize(0);
		split.setLeftComponent(leftButtonPanel);
		split.setRightComponent(rightButtonPanel);

		add(horSplit, BorderLayout.CENTER);
		add(split, BorderLayout.SOUTH);
		enableUserInput(true);
	}

	void openInputDialog()
	{
		owner.setStatus("Choosing files to import...", App.NORMAL_MESSAGE);
		FileFilter filter = new FileNameExtensionFilter("Microsoft Excel Documents (*.xlsx)", "xlsx");
		String userDir = System.getProperty("user.home");
		JFileChooser fileChooser = new JFileChooser(userDir + "/Desktop");
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setFileFilter(filter);
		fileChooser.setPreferredSize(new Dimension(800, 600));

		// This sets the default folder view to 'details'
		Action details = fileChooser.getActionMap().get("viewTypeDetails");
		details.actionPerformed(null);
		
		int choice = fileChooser.showOpenDialog(this);
		if (choice == JFileChooser.APPROVE_OPTION)
		{
			if (fileChooser.getSelectedFiles().length > 0)
			{
				translationMgr.files = fileChooser.getSelectedFiles();
				updateListView();
				updateTableView();
			}
		}
		else if (choice == JFileChooser.CANCEL_OPTION)
		{
			owner.setStatus("Aborted import...", App.NORMAL_MESSAGE);
		}
	}

	private void openOutputDialog()
	{
		if (translationMgr.getNumSelectedFiles() == 0)
		{
			JOptionPane.showMessageDialog(this, "Please import Excel sheets first", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		owner.setStatus("Selecting output folder...", App.NORMAL_MESSAGE);
		String userDir = System.getProperty("user.home");
		JFileChooser chooser = new JFileChooser(userDir + "/Desktop");
		chooser.setSelectedFile(new File("translation"));
		chooser.setPreferredSize(new Dimension(800, 600));
		// This sets the default folder view to 'details'
		Action details = chooser.getActionMap().get("viewTypeDetails");
		details.actionPerformed(null);
		int choice = chooser.showSaveDialog(this);
		translationMgr.startTimeTrace();
		enableUserInput(false);
		if (choice == JFileChooser.APPROVE_OPTION)
		{
			new Thread(() -> {
				String outputFolder = chooser.getSelectedFile().toString();
				int i = outputFolder.lastIndexOf(System.getProperty("file.separator"));
				String fileName = outputFolder.substring(i + 1, outputFolder.length());
				outputFolder = outputFolder.substring(0, i);
				boolean success = exportData(outputFolder, fileName);
				if (success)
				{
					translationMgr.stopTimeTrace();
					double seconds = (double) translationMgr.statCalculationTime / 1000.0;
					String secondsString = String.format("%.2f", seconds);
					owner.setStatus("Sucessfully exported Excel sheet(s) within " + secondsString + "s...", App.NORMAL_MESSAGE);
				}
				enableUserInput(true);
			}).start();
		}
		else
		{
			owner.setStatus("Aborted export...", App.NORMAL_MESSAGE);
			enableUserInput(true);
		}
	}

	private void updateListView()
	{
		fileList.setVisibleRowCount(-1);
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		String[] fileNames = new String[translationMgr.getNumSelectedFiles()];
		int i = 0;
		for (File file : translationMgr.files)
		{
			fileNames[i] = translationMgr.getFileName(file.getName());
			++i;
		}
		fileList.setListData(fileNames);
		fileList.setSelectedIndex(0);
	}

	private void updateTableView()
	{
		if (translationMgr.getNumSelectedFiles() == 0) return;

		translationMgr.startTimeTrace();
		owner.setStatus("Extracting data...", App.NORMAL_MESSAGE);

		enableUserInput(false);
		owner.setLoading(true);
		new Thread(() -> {
			importData();
			enableUserInput(true);
			owner.setLoading(false);
		}).start();
	}

	private void importData()
	{
		translationMgr.setFlag(TranslationMgrFlags.Import.APPEND_BRAND_TO_LOCALE, checkAppendBrandToLocale.isSelected());
		LanguageTable languageTable = translationMgr.importExcelFiles();
		Component comp = horSplit.getRightComponent();

		if (languageTable == null)
		{
			owner.setStatus("No data found inside Excel file(s). Did you set it up properly? Click 'Help' to read the documentation on how to setup an Excel file correctly.", App.ERROR_MESSAGE);
			DefaultTableModel model = new DefaultTableModel(new String[] { "Component", "Key", "Locale" }, 0);
			table = new JTable(model);
			table.setFillsViewportHeight(true);
			updateTableAutoResizing();
			return;
		}

		if (comp != null) horSplit.remove(comp);

		table = new JTable(languageTable.getJTableData(), languageTable.getJTableHeader())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}

		};
		ExceliburCellRenderer cellRenderer = new ExceliburCellRenderer();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		table.setDefaultRenderer(Object.class, cellRenderer);
		table.setFillsViewportHeight(true);
		horSplit.setRightComponent(new JScrollPane(table));
		table.setRowSelectionAllowed(true);
		updateTableAutoResizing();
		translationMgr.stopTimeTrace();
		double seconds = (double) translationMgr.statCalculationTime / 1000.0;
		String secondsString = String.format("%.2f", seconds);
		switch (translationMgr.statNumEmptyCells)
		{
		case 0:
			owner.setStatus("Sucessfully imported Excel sheet(s) within " + secondsString + "s...", App.NORMAL_MESSAGE);
			break;
		case 1:
			owner.setStatus(
				"Sucessfully imported Excel sheet(s) within " + secondsString + "s but there was " + translationMgr.statNumEmptyCells + " empty cell found! Watch out for the red marked cells!",
				App.WARNING_MESSAGE);
			break;
		default:
			owner.setStatus(
				"Sucessfully imported Excel sheet(s) within  " + secondsString + "s but there were " + translationMgr.statNumEmptyCells + " empty cells found! Watch out for the red marked cells!",
				App.WARNING_MESSAGE);
			break;
		}
	}

	private void enableUserInput(boolean bEnabled)
	{
		boolean bAnyFilesImported = translationMgr.getNumSelectedFiles() > 0 && table.getRowCount() > 0;
		// prevent export if no files are in the "imported" list
		exportButton.setEnabled(bEnabled && bAnyFilesImported);
		reloadButton.setEnabled(bEnabled && bAnyFilesImported);
		importButton.setEnabled(bEnabled);
		returnButton.setEnabled(bEnabled);
		checkBoxAutoResize.setEnabled(bEnabled);
		checkBoxMergeCompAndKey.setEnabled(bEnabled);
		checkDoNotExportEmptyCells.setEnabled(bEnabled);
	}

	private boolean exportData(String outputFolder, String fileName)
	{
		translationMgr.setFlag(TranslationMgrFlags.Export.CONCAT_COMPONENT_AND_KEY, checkBoxMergeCompAndKey.isSelected());
		translationMgr.setFlag(TranslationMgrFlags.Export.DONT_EXPORT_EMPTY_VALUES, checkDoNotExportEmptyCells.isSelected());
		return translationMgr.export2Json(outputFolder, fileName);
	}

	void updateTableAutoResizing()
	{
		if (checkBoxAutoResize.isSelected())
		{
			for (int column = 0; column < table.getColumnCount(); column++)
			{
				TableColumn tableColumn = table.getColumnModel().getColumn(column);
				int preferredWidth = tableColumn.getMinWidth();
				preferredWidth = 3000;
				tableColumn.setPreferredWidth(preferredWidth);
			}
			table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		}
		else
		{
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			for (int column = 0; column < table.getColumnCount(); column++)
			{
				TableColumn tableColumn = table.getColumnModel().getColumn(column);
				int preferredWidth = tableColumn.getMinWidth();
				int maxWidth = Math.min(tableColumn.getMaxWidth(), 2000);

				for (int row = 0; row < table.getRowCount(); row++)
				{
					TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
					Component c = table.prepareRenderer(cellRenderer, row, column);
					int width = c.getPreferredSize().width + table.getIntercellSpacing().width;
					preferredWidth = Math.max(preferredWidth, width);

					// We've exceeded the maximum width, no need to check other rows

					if (preferredWidth >= maxWidth)
					{
						preferredWidth = maxWidth;
						break;
					}
				}

				tableColumn.setPreferredWidth(preferredWidth);
			}
		}
	}
}
