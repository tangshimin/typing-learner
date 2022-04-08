//package components.table
//
//import VocabularyChooserDialog
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.window.WindowDraggableArea
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.ExpandLess
//import androidx.compose.material.icons.filled.ExpandMore
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.ExperimentalComposeUiApi
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.awt.ComposePanel
//import androidx.compose.ui.awt.SwingPanel
//import androidx.compose.ui.awt.awtEventOrNull
//import androidx.compose.ui.graphics.RectangleShape
//import androidx.compose.ui.input.pointer.PointerEventType
//import androidx.compose.ui.input.pointer.onPointerEvent
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.DpSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.WindowPosition
//import androidx.compose.ui.window.rememberDialogState
//import com.formdev.flatlaf.FlatClientProperties
//import com.formdev.flatlaf.FlatLaf
//import com.formdev.flatlaf.extras.FlatSVGIcon
//import com.formdev.flatlaf.extras.FlatSVGUtils
//import com.formdev.flatlaf.extras.components.FlatButton
//import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon
//import components.play
//import data.*
//import dialog.VocabularyType
//import kotlinx.serialization.ExperimentalSerializationApi
//import player.LocalMediaPlayerComponent
//import player.rememberMediaPlayerComponent
//import state.AppState
//import state.MutableTableState
//import state.SearchState
//import state.getFile
//import theme.DarkColorScheme
//import theme.LightColorScheme
//import java.awt.*
//import java.awt.event.*
//import java.io.File
//import java.util.*
//import java.util.regex.Pattern
//import java.util.regex.PatternSyntaxException
//import javax.swing.*
//import javax.swing.border.CompoundBorder
//import javax.swing.event.*
//import javax.swing.table.*
//import javax.swing.text.BadLocationException
//import javax.swing.text.DefaultHighlighter
//
//// TODO 这个版本的编辑词库不好，等compose desktop 有Table 组件之后再重新写一个版本
//fun showEditVocabulary(state: AppState) {
//    SwingUtilities.invokeLater {
//        val window = JFrame()
//        window.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
//        window.title = "编辑词库 - ${state.vocabulary.name}"
//        val iconFile = File("src/main/resources/logo.svg")
//        val iconImages = FlatSVGUtils.createWindowIconImages(iconFile.toURI().toURL())
//        window.iconImages = iconImages
//        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
//        splitPane.dividerLocation = 0
//        val toolPanel = ComposePanel()
//        toolPanel.setSize(216, Int.MAX_VALUE)
//        toolPanel.minimumSize = Dimension(216, 0)
//        var settingsOpen = false
//        var dividerLocation = 216
//        val onEditSettings = {
//            if (!settingsOpen) {
//                settingsOpen = true
//                splitPane.dividerLocation = dividerLocation
//            } else {
//                settingsOpen = false
//                dividerLocation = splitPane.dividerLocation
//                splitPane.dividerLocation = 0
//            }
//        }
//        toolPanel.setContent {
//            TableToolbar(state)
//        }
//        toolPanel.isVisible = true
//
//        val tablePanel = ComposePanel()
//        // 重新载入当前词库，这个词库不是可观察的，更改或删除之后typing 界面不会改变
////        val wordList = loadVocabulary(state.typing.vocabularyPath).wordList
//        val wordList = state.vocabulary.wordList
//
//        tablePanel.setContent {
//            Table(
//                darkTheme = state.typing.darkTheme,
//                searchState = state.search,
//                tableState = state.table,
//                vocabulary = state.vocabulary,
//                saveVocabulary = {
//                    state.vocabulary.wordList = wordList
//                    saveVocabulary(state.vocabulary.vocabulary, state.typing.vocabularyPath)},
//                subtitlesVocabularyMap = state.captionsMap,
//                wordList = wordList,
//                editSettings = state.editSettings,
//                onEditSettings = { onEditSettings() }
//            )
//
//        }
//        splitPane.leftComponent = toolPanel
//        splitPane.rightComponent = tablePanel
//        splitPane.isContinuousLayout = true
//        window.contentPane.add(splitPane, BorderLayout.CENTER)
//        window.setSize(900, 900)
//        window.isVisible = true
//
//    }
//
//}
//
//
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun TableToolbar(state: AppState) {
//    MaterialTheme(colors = if (state.typing.darkTheme) DarkColorScheme else LightColorScheme) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Top,
//            modifier = Modifier
//                .fillMaxWidth()
//                .fillMaxHeight()
//                .background(MaterialTheme.colors.background)
//        ) {
//            Column {
//                Column {
//
////                    // 当前词库类型为文档，同时字幕文件夹有文件
////                    if (state.vocabulary.type == VocabularyType.DOCUMENT && subtitleDirectoryIsNotEmpty()) {
////                        Divider()
////                        Row(
////                            horizontalArrangement = Arrangement.SpaceBetween,
////                            verticalAlignment = Alignment.CenterVertically,
////                            modifier = Modifier
////                                .fillMaxWidth().height(48.dp).clickable {
////                                }
////                                .padding(start = 16.dp, end = 16.dp)
////                        ) {
////                            Text("导入字幕", color = MaterialTheme.colors.onBackground)
////                            Icon(
////                                Icons.Default.Add,
////                                contentDescription = "Localized description",
////                                tint = MaterialTheme.colors.onBackground
////                            )
////                        }
////
////                    }
//                    Divider()
//
//                }
//            }
//
//            val stateVertical = rememberScrollState(0)
//            Box(modifier = Modifier.fillMaxHeight()) {
//                var expand by remember { mutableStateOf(false) }
//                if (expand) {
//                    Box(
//                        Modifier.fillMaxHeight()
//                            .padding(top = 48.dp)
//                            .verticalScroll(stateVertical)
//                    ) {
//                        Column {
//
//                            ListItem(
//                                text = { Text("中文释义", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.translationVisible,
//                                        onCheckedChange = {
//                                            state.table.translationVisible = it
//                                        },
//                                    )
//                                }
//                            )
//
//                            ListItem(
//                                text = { Text("英文释义", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.definitionVisible,
//                                        onCheckedChange = {
//                                            state.table.definitionVisible = it
//                                        },
//                                    )
//                                }
//                            )
//
//                            ListItem(
//                                text = { Text("英国音标", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.uKPhoneVisible,
//                                        onCheckedChange = {
//                                            state.table.uKPhoneVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = { Text("美国音标", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.usPhoneVisible,
//                                        onCheckedChange = {
//                                            state.table.usPhoneVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = { Text("词形变化", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.exchangeVisible,
//                                        onCheckedChange = {
//                                            state.table.exchangeVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = { Text("字幕", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.captionsVisible,
//                                        onCheckedChange = {
//                                            state.table.captionsVisible = it
//
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = { Text("标签", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.tagVisible,
//                                        onCheckedChange = {
//                                            state.table.tagVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = { Text("牛津核心词", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.oxfordVisible,
//                                        onCheckedChange = {
//                                            state.table.oxfordVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = { Text("柯林斯星级", color = MaterialTheme.colors.onBackground) },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.collinsVisible,
//                                        onCheckedChange = {
//                                            state.table.collinsVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = {
//                                    Text(
//                                        "英国国家语料库词频顺序", color = MaterialTheme.colors.onBackground,
//                                        modifier = Modifier.width(120.dp)
//                                    )
//                                },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.bncVisible,
//                                        onCheckedChange = {
//                                            state.table.bncVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                            ListItem(
//                                text = {
//                                    Text(
//                                        "当代语料库词频顺序", color = MaterialTheme.colors.onBackground,
//                                        modifier = Modifier.width(120.dp)
//                                    )
//                                },
//                                trailing = {
//                                    Switch(
//                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
//                                        checked = state.table.frqVisible,
//                                        onCheckedChange = {
//                                            state.table.frqVisible = it
//                                        },
//                                    )
//                                }
//                            )
//                        }
//                    }
//                }
//                if (expand) {
//                    VerticalScrollbar(
//                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
//                        adapter = rememberScrollbarAdapter(stateVertical)
//                    )
//                }
//                Column(modifier = Modifier
//                    .clickable { expand = !expand }
//                    .align(Alignment.TopCenter)
//                    .background(MaterialTheme.colors.background)) {
//                    ListItem(
//                        text = { Text("隐藏和显示列", color = MaterialTheme.colors.onBackground) },
//                        trailing = {
//                            Icon(
//                                imageVector = if (expand) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
//                                contentDescription = "隐藏和显示列",
//                                tint = MaterialTheme.colors.onBackground
//                            )
//                        }
//
//                    )
//                    Divider()
//                }
//
//
//            }
//        }
//    }
//
//}
//
//
//
//@OptIn(ExperimentalSerializationApi::class)
//@Composable
//fun Table(
//    wordList: List<Word>,
//    searchState: SearchState,
//    tableState: MutableTableState,
//    vocabulary: MutableVocabulary,
//    saveVocabulary:( List<Word>) ->Unit,
//    subtitlesVocabularyMap: MutableMap<String, HashMap<String, List<Caption>>>,
//    darkTheme: Boolean,
//    editSettings: Boolean,
//    onEditSettings: () -> Unit,
//) {
//    val data = Array(wordList.size) {
//        val word = wordList[it]
//        var captions = ""
//        if (vocabulary.type == VocabularyType.DOCUMENT) {
//            captions = getDisplayCaption(word.links, subtitlesVocabularyMap)
//        } else {
//            word.captions.forEachIndexed { index, caption ->
//                var num = (index + 1).toString() + ". "
//                captions += num + caption.content
//                var isNewline = index + 1 == word.captions.size
//                if (!isNewline) captions += "\r\n"
//            }
//        }
//
//        arrayOf(
//            it + 1,
//            word.value,
//            word.translation,
//            word.definition,
//            word.usphone,
//            word.ukphone,
//            word.exchange,
//            captions,
//            word.tag,
//            word.oxford,
//            word.collins,
//            word.bnc,
//            word.frq,
//        )
//    }
//
//
//    val columnNames = arrayOf(
//        "  ",
//        "单词",
//        "中文释义",
//        "英文释义",
//        "美国音标",
//        "英国音标",
//        "词形变化",
//        "字幕",
//        "标签",
//        "牛津核心词",
//        "柯林斯星级",
//        "英国国家语料库词频顺序",
//        "当代语料库词频顺序"
//    )
//
//    val model: TableModel = object : DefaultTableModel(data, columnNames) {
//        override fun isCellEditable(row: Int, column: Int): Boolean {
//            val columnName = this.getColumnName(column)
//            if(columnName == "字幕") return false
//            if (column == 0) return false
//            return super.isCellEditable(row, column)
//        }
//    }
//
//
//    val table = JTable(model)
//
//    table.setShowGrid(true)
//    table.rowSelectionAllowed = false
//    table.cellSelectionEnabled = true
//    table.autoCreateRowSorter = true
//    //DefaultCellEditor
//    val textField = JTextField()
//    val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
//    val outsideBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 175, 0))
//    textField.border = CompoundBorder(outsideBorder, insideBorder)
//
//    val indexColumn = table.columnModel.getColumn(0)
//    val valueColumn = table.columnModel.getColumn(1)
//    val translationColumn = table.columnModel.getColumn(2)
//    val definitionColumn = table.columnModel.getColumn(3)
//    val usPhoneColumn = table.columnModel.getColumn(4)
//    val ukPhoneColumn = table.columnModel.getColumn(5)
//    val exchangeColumn = table.columnModel.getColumn(6)
//    val captionsColumn = table.columnModel.getColumn(7)
//    val tagColumn = table.columnModel.getColumn(8)
//    val oxfordColumn = table.columnModel.getColumn(9)
//    val collinsColumn = table.columnModel.getColumn(10)
//    val bncColumn = table.columnModel.getColumn(11)
//    val frqColumn = table.columnModel.getColumn(12)
//
//    val textFieldCellEditor = DefaultCellEditor(textField)
//    valueColumn.cellEditor = textFieldCellEditor
//    usPhoneColumn.cellEditor = textFieldCellEditor
//    ukPhoneColumn.cellEditor = textFieldCellEditor
//    oxfordColumn.cellEditor = textFieldCellEditor
//    collinsColumn.cellEditor = textFieldCellEditor
//    bncColumn.cellEditor = textFieldCellEditor
//    frqColumn.cellEditor = textFieldCellEditor
//
//    val textAreaCellEditor = TextAreaCellEditor(darkTheme)
//    translationColumn.cellEditor = textAreaCellEditor
//    definitionColumn.cellEditor = textAreaCellEditor
//    exchangeColumn.cellEditor = textAreaCellEditor
//    tagColumn.cellEditor = textAreaCellEditor
//
////    captionsColumn.cellEditor = CaptionCellEditor(darkTheme, vocabulary, subtitlesVocabularyMap)
//
//    indexColumn.cellRenderer = FirstCellRenderer(darkTheme)
//    val customCellRenderer = CustomCellRenderer()
//    val rowHeightCellRenderer = RowHeightCellRenderer()
//
//    valueColumn.cellRenderer = customCellRenderer
//    usPhoneColumn.cellRenderer = customCellRenderer
//    ukPhoneColumn.cellRenderer = customCellRenderer
//    oxfordColumn.cellRenderer = customCellRenderer
//    collinsColumn.cellRenderer = customCellRenderer
//
//    bncColumn.cellRenderer = customCellRenderer
//    frqColumn.cellRenderer = customCellRenderer
//
//
//    translationColumn.cellRenderer = rowHeightCellRenderer
//    definitionColumn.cellRenderer = rowHeightCellRenderer
//    exchangeColumn.cellRenderer = rowHeightCellRenderer
//    captionsColumn.cellRenderer = rowHeightCellRenderer
//    tagColumn.cellRenderer = rowHeightCellRenderer
//
//    indexColumn.minWidth = 33
//    indexColumn.preferredWidth = 45
//    indexColumn.maxWidth = 50
//    valueColumn.preferredWidth = 150
//    translationColumn.preferredWidth = 300
//    definitionColumn.preferredWidth = 500
//    usPhoneColumn.preferredWidth = 95
//    ukPhoneColumn.preferredWidth = 95
//    exchangeColumn.preferredWidth = 420
//    captionsColumn.preferredWidth = 350
//    tagColumn.preferredWidth = 150
//    oxfordColumn.preferredWidth = 90
//    collinsColumn.preferredWidth = 90
//    bncColumn.preferredWidth = 190
//    frqColumn.preferredWidth = 160
//
//    table.autoResizeMode = JTable.AUTO_RESIZE_OFF
//
//
//
//
//    table.model.addTableModelListener { modelEvent ->
//        val model = modelEvent.source as TableModel
//
//        val row = modelEvent.firstRow
//        val column = modelEvent.column
//
//        when (modelEvent.type) {
//            TableModelEvent.INSERT -> {
//                println("Insert")
//            }
//            TableModelEvent.DELETE -> {
//                println("删除了第${row + 1} 行")
//            }
//            TableModelEvent.UPDATE -> {
//                val columnName = model.getColumnName(column)
//                if (columnName == "字幕") {
//                } else {
//                    val value = model.getValueAt(row, column) as String
//                    if (data[row][column] != value) {
//                        when (columnName) {
//                            "单词" -> {
//                                wordList[row].value = value
//                            }
//                            "中文释义" -> {
//                                wordList[row].translation = value
//                            }
//                            "英文释义" -> {
//                                wordList[row].definition = value
//                            }
//                            "美国音标" -> {
//                                wordList[row].usphone = value
//                            }
//                            "英国音标" -> {
//                                wordList[row].ukphone = value
//                            }
//                            "词形变化" -> {
//                                wordList[row].exchange = value
//                            }
//                            "字幕" -> {
//                              println("现在还不能修改字幕")
//                            }
//                            "标签" -> {
//                                wordList[row].tag = value
//                            }
//                            "牛津核心词" -> {
//                                wordList[row].oxford = value.toBoolean()
//                            }
//                            "柯林斯星级" -> {
//                                wordList[row].collins = value.toInt()
//                            }
//                            "英国国家语料库词频顺序" -> {
//                                wordList[row].bnc = value.toInt()
//                            }
//                            "当代语料库词频顺序" -> {
//                                wordList[row].frq = value.toInt()
//                            }
////                            else -> {
////                                println("$columnName 列的更改还没有实现")
////                            }
//                        }
//                    }
//                }
//                saveVocabulary(wordList)
//
//            }
//        }
//
//    }
//
//    val sorter = TableRowSorter(model)
//    table.rowSorter = sorter
//
//    val scrollPane =
//        JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
//
//
//    val addRow = {
//        val model = table.model as DefaultTableModel
//        val index = model.rowCount + 1
//        val array = arrayOf(index, "", "", "", "", "")
//        model.addRow(array)
//        SwingUtilities.invokeLater {
//            val vertical = scrollPane.verticalScrollBar
//            vertical.value = vertical.maximum
//        }
//    }
//    val removeRow = {
//        val rows = table.selectedRows
//        if (rows.isNotEmpty()) {
//            val model = table.model as DefaultTableModel
//            val first = rows[0]
//            rows.reverse()
////            var list = ArrayList(wordList)
//            for (row in rows) {
//                model.removeRow(row)
//                vocabulary.wordList.removeAt(row)
//            }
////            vocabulary.wordList = list
//            for (i in first..model.columnCount) {
//                var index = (i + 1).toString()
//                model.setValueAt(index, i, 0)
//            }
//        }
//
//    }
//
//
//    val primaryColor = Color(9, 175, 0)
//    val onBackgroundColor = if (FlatLaf.isLafDark()) Color(133, 144, 151) else Color.BLACK
//
//    val settings = FlatButton()
//    val addButton = FlatButton()
//    val removeButton = FlatButton()
//
//    settings.addActionListener {
//        onEditSettings()
//    }
//
//    settings.preferredSize = Dimension(48, 48);
//    addButton.preferredSize = Dimension(48, 48)
//    removeButton.preferredSize = Dimension(48, 48)
//
//    settings.margin = Insets(10, 10, 10, 10)
//    addButton.margin = Insets(10, 10, 10, 10)
//    removeButton.margin = Insets(10, 10, 10, 10)
//
//    settings.buttonType = FlatButton.ButtonType.borderless
//    addButton.buttonType = FlatButton.ButtonType.borderless
//    removeButton.buttonType = FlatButton.ButtonType.borderless
//
//
//    val settingsIcon = FlatSVGIcon(File("src/main/resources/svg/tune_white_18dp.svg"))
//    settingsIcon.colorFilter = FlatSVGIcon.ColorFilter { primaryColor }
//    settings.icon = settingsIcon
//
//    val addIcon = FlatSVGIcon(File("src/main/resources/svg/add_white_18dp.svg"))
//    addIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
//    addButton.icon = addIcon
//
//    val removeIcon = FlatSVGIcon(File("src/main/resources/svg/remove_white_18dp.svg"))
//    removeIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
//    removeButton.icon = removeIcon
//
//    val compsTextField = JTextField(50)
//    compsTextField.preferredSize = Dimension(650, 48);
//    compsTextField.isFocusable = true
//
//    // search history button
//    val searchHistoryButton = JButton(FlatSearchWithHistoryIcon(true))
//    searchHistoryButton.preferredSize = Dimension(48, 48)
//    searchHistoryButton.toolTipText = "搜索历史记录"
//    var searchHistoryList = ArrayList<String>()
//
//    compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, searchHistoryButton)
//
//    // match case button
//    val matchCaseButton = JToggleButton(FlatSVGIcon(File("src/main/resources/svg/matchCase.svg")))
//    matchCaseButton.rolloverIcon = FlatSVGIcon(File("src/main/resources/svg/matchCaseHovered.svg"))
//    matchCaseButton.selectedIcon = FlatSVGIcon(File("src/main/resources/svg/matchCaseSelected.svg"))
//    matchCaseButton.toolTipText = "区分大小写"
//    matchCaseButton.isSelected = searchState.matchCaseIsSelected
//    // whole words button
//    val wordsButton = JToggleButton(FlatSVGIcon(File("src/main/resources/svg/words.svg")))
//    wordsButton.rolloverIcon = FlatSVGIcon(File("src/main/resources/svg/wordsHovered.svg"))
//    wordsButton.selectedIcon = FlatSVGIcon(File("src/main/resources/svg/wordsSelected.svg"))
//    wordsButton.toolTipText = "单词"
//    wordsButton.isSelected = searchState.wordsIsSelected
//    wordsButton.isEnabled = searchState.wordsIsEnable
//    // regex button
//    val regexButton = JToggleButton(FlatSVGIcon(File("src/main/resources/svg/regex.svg")))
//    regexButton.rolloverIcon = FlatSVGIcon(File("src/main/resources/svg/regexHovered.svg"))
//    regexButton.selectedIcon = FlatSVGIcon(File("src/main/resources/svg/regexSelected.svg"))
//    regexButton.toolTipText = "正则表达式"
//    regexButton.isSelected = searchState.regexIsSelected
//    // search toolbar
//    val searchToolbar = JToolBar()
//    searchToolbar.isFocusable = true
//    searchToolbar.focusTraversalKeysEnabled = true
//    searchToolbar.add(matchCaseButton)
//    searchToolbar.add(wordsButton)
//    searchToolbar.addSeparator()
//    searchToolbar.add(regexButton)
//    compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, searchToolbar)
//    compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
//    compsTextField.putClientProperty(FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY, true)
//
//    var resultCounter = JLabel("")
//    var upButton = FlatButton()
//    var downButton = FlatButton()
//
//
//    upButton.preferredSize = Dimension(48, 48);
//    downButton.preferredSize = Dimension(48, 48);
//
//    upButton.margin = Insets(10, 10, 10, 10)
//    downButton.margin = Insets(10, 10, 10, 10)
//
//    upButton.buttonType = FlatButton.ButtonType.borderless
//    downButton.buttonType = FlatButton.ButtonType.borderless
//
//    val upButtonIcon = FlatSVGIcon(File("src/main/resources/svg/north_white_18dp.svg"))
//    upButtonIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
//    upButton.icon = upButtonIcon
//
//    val downButtonIcon = FlatSVGIcon(File("src/main/resources/svg/south_white_18dp.svg"))
//    downButtonIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
//    downButton.icon = downButtonIcon
//
//
//    val resultRectangleList = mutableListOf<Pair<Cell, Rectangle>>()
//    var resultIndex = 0
//
//    val searchUp = {
//        if (resultRectangleList.size > 0 && resultIndex >= 0) {
//            resultIndex--
//            if (resultIndex == -1) resultIndex = resultRectangleList.size - 1
//            val cell = resultRectangleList[resultIndex].first
//            val rectangle = resultRectangleList[resultIndex].second
//            resultCounter.text = "${resultIndex + 1}/${resultRectangleList.size}"
//            scrollToCenter(table, cell.row, cell.column)
//            table.changeSelection(cell.row, cell.column, false, false)
//            table.repaint(rectangle)
//        }
//    }
//    val searchDown = {
//        if (resultRectangleList.size > 0 && resultIndex >= 0) {
//            resultIndex++
//            if (resultIndex == resultRectangleList.size) resultIndex = 0
//            val cell = resultRectangleList[resultIndex].first
//            val rectangle = resultRectangleList[resultIndex].second
//
//            resultCounter.text = "${resultIndex + 1}/${resultRectangleList.size}"
//
//            scrollToCenter(table, cell.row, cell.column)
//            table.changeSelection(cell.row, cell.column, false, false)
//            table.repaint(rectangle)
//        }
//    }
//
//    upButton.addActionListener { searchUp() }
//    downButton.addActionListener { searchDown() }
//
//    val cleanHighLight = {
//        resultCounter.text = ""
//        sorter.rowFilter = null
//        val columns = table.columnCount - 1
//        for (col in 1..columns) {
//            var renderer = table.columnModel.getColumn(col).cellRenderer as HighLightCell
//            renderer.clear()
//        }
//        table.repaint()
//    }
//    val search = {
//        val length = compsTextField.text.length
//        val searchTime = System.currentTimeMillis()
//        resultRectangleList.clear()
//        resultIndex = 0
//
//        if (length != 0) {
//            val keyword = compsTextField.text
//
//            var filterRegex = "$keyword"
//            var pattern: Pattern
//            try {
//
//                if (wordsButton.isSelected) {
//                    // 匹配单词
//                    filterRegex = "\\b$filterRegex\\b"
//                }
//                if (!regexButton.isSelected && !wordsButton.isSelected) {
//                    filterRegex = Pattern.quote(filterRegex)
//                }
//                if (!matchCaseButton.isSelected) {
//                    // 不区分大小写
//                    filterRegex = "(?i)$filterRegex"
//                }
//                sorter.rowFilter = RowFilter.regexFilter(filterRegex)
//                pattern = Pattern.compile(filterRegex)
//
//                val rows = table.rowCount - 1
//                val columns = table.columnCount - 1
//                for (row in 0..rows) {
//                    for (column in 1..columns) {
//                        val value = table.getValueAt(row, column).toString()
//                        val matcher = pattern.matcher(value)
//                        var finded = false
//                        val highlightSpans = mutableMapOf<Int, Int>()
//                        while (matcher.find()) {
//                            finded = true
//                            val start = matcher.start()
//                            val end = matcher.end()
//                            highlightSpans.put(start, end)
//                        }
//
//                        if (finded) {
//                            val columnName = table.model.getColumnName(column)
//                            if (columnName == "英国国家语料库词频顺序" || columnName == "当代语料库词频顺序") {
//                                continue
//                            }
//                            var renderer = table.getCellRenderer(row, column) as HighLightCell
//                            renderer.setSearchTime(searchTime)
//                            val cell = Cell(row, column)
//                            renderer.addHighlightCell(cell, highlightSpans)
//                            renderer.setKeyword(keyword)
//                            val rectangle = table.getCellRect(row, column, true)
//                            val pair = cell to rectangle
//                            resultRectangleList.add(pair)
//                            table.repaint(rectangle)
//                        }
//
//
//                    }
//                }
//
//            } catch (syntaxException: PatternSyntaxException) {
//                println(syntaxException.description)
//            }
//
//            resultCounter.text =
//                if (resultRectangleList.size > 0) "${resultIndex + 1}/${resultRectangleList.size}" else "0"
//            if (resultRectangleList.size > 0) {
//                val cell = resultRectangleList[resultIndex].first
//                table.changeSelection(cell.row, cell.column, false, false)
//                val rectangle = resultRectangleList[resultIndex].second
//                table.scrollRectToVisible(rectangle)
//            }
//            if (!searchHistoryList.contains(keyword)) {
//                searchHistoryList.add(keyword)
//            }
//
////            table.repaint()
//        } else {
//            cleanHighLight()
//        }
//
//    }
//    val setPlaceholder = {
//        var placeholderText = ""
//        if (matchCaseButton.isSelected && !wordsButton.isSelected && !regexButton.isSelected) {
//            placeholderText += "区分大小写"
//        } else if (matchCaseButton.isSelected && (wordsButton.isSelected || regexButton.isSelected)) {
//            placeholderText += "区分大小写和"
//        }
//
//        if (wordsButton.isSelected && !regexButton.isSelected) {
//            placeholderText += "单词"
//        }
//        if (regexButton.isSelected) {
//            placeholderText += "正则表达式"
//        }
//        compsTextField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholderText)
//    }
//    setPlaceholder()
//    searchHistoryButton.addActionListener {
//        val popupMenu = JPopupMenu()
//        if (searchHistoryList.isEmpty()) {
//            popupMenu.add("(empty)")
//        } else {
//            for (history in searchHistoryList.reversed()) {
//                val menuItem = JMenuItem(history)
//                menuItem.addActionListener {
//                    compsTextField.text = history
//                    search()
//                }
//                popupMenu.add(menuItem)
//            }
//        }
//
//        popupMenu.show(searchHistoryButton, 0, searchHistoryButton.height)
//    }
//    matchCaseButton.addActionListener {
//        searchState.matchCaseIsSelected = matchCaseButton.isSelected
//        setPlaceholder()
//        cleanHighLight()
//        search()
//    }
//    wordsButton.addActionListener {
//        searchState.wordsIsSelected = wordsButton.isSelected
//        setPlaceholder()
//        cleanHighLight()
//        search()
//    }
//    regexButton.addActionListener {
//        searchState.wordsIsEnable = !regexButton.isSelected
//        if (regexButton.isSelected && wordsButton.isSelected) {
//            searchState.wordsIsSelected = false
//
//        }
//        searchState.regexIsSelected = regexButton.isSelected
//        setPlaceholder()
//        cleanHighLight()
//        search()
//    }
//    compsTextField.document.addDocumentListener(object : DocumentListener {
//        override fun insertUpdate(documentEvent: DocumentEvent?) {
////            searchState.keyword = compsTextField.text
//            search()
//        }
//
//        override fun changedUpdate(documentEvent: DocumentEvent?) {
////            searchState.keyword = compsTextField.text
//            search()
//        }
//
//        override fun removeUpdate(documentEvent: DocumentEvent?) {
////            searchState.keyword = compsTextField.text
//            search()
//        }
//
//    })
//    compsTextField.addKeyListener(object : KeyListener {
//        override fun keyTyped(keyEvent: KeyEvent) {}
//        override fun keyReleased(keyEvent: KeyEvent) {}
//        override fun keyPressed(keyEvent: KeyEvent) {
//            when (keyEvent.keyCode) {
//                10 -> {
//                    searchDown()
//                }
//                38 -> {
//                    searchUp()
//                }
//                40 -> {
//                    searchDown()
//                }
//            }
//        }
//
//    })
//
//    addButton.addActionListener { addRow() }
//    removeButton.addActionListener { removeRow() }
//
//    val panel = JPanel()
//    panel.background = if (!darkTheme) Color.WHITE else Color(18, 18, 18)
//    panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
//    panel.border = BorderFactory.createMatteBorder(1, 0, 0, 0, table.gridColor)
//    compsTextField.border = BorderFactory.createMatteBorder(0, 1, 0, 1, table.gridColor)
//    scrollPane.border = BorderFactory.createMatteBorder(1, 0, 1, 1, table.gridColor)
//    panel.add(settings)
//    panel.add(addButton)
//    panel.add(removeButton)
//    panel.add(compsTextField)
//    panel.add(resultCounter)
//    panel.add(upButton)
//    panel.add(downButton)
//
//    val popupMenu = JPopupMenu()
//
//    val addNewRow = JMenuItem("Add Row", addIcon)
//    val removeSelected = JMenuItem("删除行", removeIcon)
//
//
//    addNewRow.addActionListener { addRow() }
//
//    removeSelected.addActionListener { removeRow() }
//    // 暂时不添加增加行的功能
////    popupMenu.add(addNewRow)
//    popupMenu.add(removeSelected)
//    table.componentPopupMenu = popupMenu;
//
//    displayOrHideColumn(tableState, table)
//
//    SwingPanel(
//        modifier = Modifier.fillMaxSize(),
//        factory = {
//            JPanel().apply {
//                layout = BorderLayout()
//                add(panel, BorderLayout.NORTH)
//                add(scrollPane, BorderLayout.CENTER)
//            }
//        },
//    )
//}
//
//fun scrollToCenter(table: JTable, rowIndex: Int, vColIndex: Int) {
//    if (table.parent !is JViewport) {
//        return
//    }
//    val viewport = table.parent as JViewport
//    val rect = table.getCellRect(rowIndex, vColIndex, true)
//    val viewRect = viewport.viewRect
//    rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y)
//    var centerX = (viewRect.width - rect.width) / 2
//    var centerY = (viewRect.height - rect.height) / 2
//    if (rect.x < centerX) {
//        centerX = -centerX
//    }
//    if (rect.y < centerY) {
//        centerY = -centerY
//    }
//    rect.translate(centerX, centerY)
//    viewport.scrollRectToVisible(rect)
//}
//
//// 隐藏和显示列
//fun displayOrHideColumn(tableState: MutableTableState, table: JTable) {
//    var hideColumnList: MutableList<Int> = mutableListOf()
//    if (!tableState.translationVisible) {
//        hideColumnList.add(2)
//    }
//    if (!tableState.definitionVisible) {
//        hideColumnList.add(3)
//    }
//    if (!tableState.usPhoneVisible) {
//        hideColumnList.add(4)
//    }
//    if (!tableState.uKPhoneVisible) {
//        hideColumnList.add(5)
//    }
//    if (!tableState.exchangeVisible) {
//        hideColumnList.add(6)
//    }
//    if (!tableState.captionsVisible) {
//        hideColumnList.add(7)
//    }
//    if (!tableState.tagVisible) {
//        hideColumnList.add(8)
//    }
//    if (!tableState.oxfordVisible) {
//        hideColumnList.add(9)
//    }
//    if (!tableState.collinsVisible) {
//        hideColumnList.add(10)
//    }
//    if (!tableState.bncVisible) {
//        hideColumnList.add(11)
//    }
//    if (!tableState.frqVisible) {
//        hideColumnList.add(12)
//    }
//    hideColumnList.reverse()
//    hideColumnList.forEach { columnIndex ->
//        val column = table.columnModel.getColumn(columnIndex)
//        table.removeColumn(column)
//    }
//}
//
//
//class CustomCellRenderer() : JTextField(), TableCellRenderer, HighLightCell {
//    private var highlightCells: HashMap<Cell, MutableMap<Int, Int>> = HashMap()
//    private var lastTime: Long = -1L
//    private var keyword: String = ""
//
//    override fun addHighlightCell(cell: Cell, highlightSpans: MutableMap<Int, Int>) {
//        highlightCells.put(cell, highlightSpans)
//    }
//
//    override fun clear() {
//        highlightCells.clear()
//
//    }
//
//    override fun setSearchTime(lastTime: Long) {
//        if (lastTime != this.lastTime) {
//            highlightCells.clear()
//        }
//        this.lastTime = lastTime
//    }
//
//    override fun setKeyword(keyword: String) {
//        this.keyword = keyword
//    }
//
//
//    override fun getTableCellRendererComponent(
//        table: JTable, value: Any,
//        isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
//    ): Component {
//        text = value.toString()
//        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
//        val outsideBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, table.gridColor)
//        val focusBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 161, 0))
//        border = CompoundBorder(outsideBorder, insideBorder)
//
//        if (isSelected) {
//            background = table.selectionBackground;
//            foreground = table.selectionForeground;
//        } else {
//            background = table.background;
//            foreground = table.foreground;
//        }
//
//
//        val cell = Cell(row, column)
//        val spans = highlightCells.get(cell)
//        if (!spans.isNullOrEmpty()) {
//            spans.forEach { (start, end) ->
//                highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
//            }
//        }
//
//        if (highlightCells.contains(Cell(row, column)) && isSelected) {
//            background = table.background;
//            foreground = table.foreground;
//            border = CompoundBorder(focusBorder, insideBorder)
//        }
//
//
//        return this
//    }
//
//}
//
//internal class FirstCellRenderer(private val darkTheme: Boolean) : DefaultTableCellRenderer() {
//    override fun getTableCellRendererComponent(
//        table: JTable, obj: Any,
//        isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
//    ): Component {
//        val cell = super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column)
//        if (darkTheme) {
//            cell.background = Color(59, 59, 59)
//            cell.background = Color(35, 35, 35)
//            border = (BorderFactory.createMatteBorder(0, 0, 1, 1, Color(45, 45, 45)))
//        } else {
//            cell.background = Color(239, 239, 239)
//            border = (BorderFactory.createMatteBorder(0, 0, 1, 1, Color(219, 219, 219)))
//        }
//        horizontalAlignment = JLabel.CENTER;
//
//        if (isSelected) {
//            table.addColumnSelectionInterval(0, table.columnCount - 1)
//            if (darkTheme) {
//                cell.background = table.selectionBackground
//            }
//        }
//
//        return this
//    }
//
//
//}
//
//
//internal open class RowHeightCellRenderer() : JTextArea(), TableCellRenderer, HighLightCell {
//    private var highlightCells: HashMap<Cell, MutableMap<Int, Int>> = HashMap()
//    private var lastTime: Long = -1L
//    private var keyword: String = ""
//
//    override fun addHighlightCell(cell: Cell, highlightSpans: MutableMap<Int, Int>) {
//        highlightCells.put(cell, highlightSpans)
//    }
//
//    override fun setSearchTime(lastTime: Long) {
//        if (lastTime != this.lastTime) {
//            highlightCells.clear()
//        }
//        this.lastTime = lastTime
//    }
//
//    override fun setKeyword(keyword: String) {
//        this.keyword = keyword
//    }
//
//    override fun clear() {
//        highlightCells.clear()
//    }
//
//    override fun getTableCellRendererComponent(
//        table: JTable,
//        value: Any,
//        isSelected: Boolean,
//        hasFocus: Boolean,
//        row: Int,
//        column: Int
//    ): Component {
//        text = value as String?
//        lineWrap = true
//        wrapStyleWord = true
//        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
//        val outsideBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, table.gridColor)
//        val focusBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 161, 0))
//        border = CompoundBorder(outsideBorder, insideBorder)
//
//        if (isSelected) {
//            background = table.selectionBackground;
//            foreground = table.selectionForeground;
//
//        } else {
//            background = table.background;
//            foreground = table.foreground;
//        }
//
//        val cell = Cell(row, column)
//        val spans = highlightCells.get(cell)
//        if (!spans.isNullOrEmpty()) {
//            spans.forEach { (start, end) ->
//                highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
//            }
//        }
//
//
//        if (highlightCells.contains(Cell(row, column)) && isSelected) {
//            background = table.background
//            foreground = table.foreground
//            border = CompoundBorder(focusBorder, insideBorder)
//        }
//
//
//        // Set the component width to match the width of its table cell
//        // and make the height arbitrarily large to accomodate all the contents
//        setSize(table.columnModel.getColumn(column).width, Short.MAX_VALUE.toInt())
//
//        // Now get the fitted height for the given width
//        val rowHeight = this.preferredSize.height
//
//
//        // Get the current table row height
//        val actualRowHeight = table.getRowHeight(row)
//
//        // Set table row height to fitted height.
//        // Important to check if this has been done already
//        // to prevent a never-ending loop.
//        if (rowHeight > actualRowHeight) {
//            table.setRowHeight(row, rowHeight)
//        }
//
//        validate()
//        return this
//    }
//
//    override fun getPreferredSize(): Dimension {
//        try {
//            // Get Rectangle for position after last text-character
//            val rectangle: Rectangle? = modelToView(document.length)
//            if (rectangle != null) {
//                return Dimension(
//                    width,
//                    this.insets.top + rectangle.y + rectangle.height +
//                            this.insets.bottom
//                )
//            }
//        } catch (e: BadLocationException) {
//            e.printStackTrace()
//        }
//        return super.getPreferredSize()
//    }
//
//}
//
//private val formatPattern: Pattern = Pattern.compile("\\((.*?)\\)\\[(.*?)\\]\\[(.*?)\\]")
//private fun getDisplayCaption(
//    links: List<String>,
//    captionsMap: MutableMap<String, HashMap<String, List<Caption>>>
//): String {
//    var displayCaption = ""
//
//    for (i in links.indices) {
//        var num = (i + 1).toString() + ". "
//        val link = links[i]
//        val matcher = formatPattern.matcher(link)
//        if (matcher.find()) {
//            val subtitleName = matcher.group(1)
//            val word = matcher.group(2)
//            val i = matcher.group(3).toInt()
//            if (!captionsMap.containsKey(subtitleName)) {
//                captionsMap[subtitleName] = loadCaptionsMap(subtitleName)
//            }
//            val caption = captionsMap[subtitleName]?.get(word)?.get(i)?.content
//            displayCaption += num + caption + "\r\n"
//            var isNewline = i + 1 == links.size - 1
//            if (!isNewline) displayCaption += "\r\n"
//        }
//
//    }
//    return displayCaption
//}
//
//internal class TextAreaCellEditor(private val darkTheme: Boolean) : TableCellEditor {
//    private var listenerList = EventListenerList()
//    private val textArea = JTextArea()
//    override fun getCellEditorValue(): Any {
//        return textArea.text
//    }
//
//    override fun getTableCellEditorComponent(
//        table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
//    ): Component {
//        textArea.margin = Insets(5, 5, 5, 5)
//        textArea.lineWrap = true
//        textArea.font = table.font
//        textArea.text = value?.toString()
//        textArea.lineWrap = true;
//        textArea.wrapStyleWord = true;
//        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
//        val outsideBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 175, 0))
//        textArea.border = CompoundBorder(outsideBorder, insideBorder)
//        if (darkTheme) {
//            textArea.selectionColor = Color(33, 66, 131)
//            textArea.background = Color(18, 18, 18)
//        }
//
//        return textArea
//    }
//
//    override fun isCellEditable(event: EventObject): Boolean {
//        return if (event is MouseEvent) {
//            event.clickCount > 1
//        } else {
//            false
//        }
//
//    }
//
//    override fun shouldSelectCell(e: EventObject): Boolean {
//        return true
//    }
//
//    override fun stopCellEditing(): Boolean {
//        fireEditingStopped()
//        return true
//    }
//
//    override fun cancelCellEditing() {
//        fireEditingCanceled()
//    }
//
//    override fun addCellEditorListener(l: CellEditorListener) {
//        listenerList.add(CellEditorListener::class.java, l)
//    }
//
//    override fun removeCellEditorListener(l: CellEditorListener) {
//        listenerList.remove(CellEditorListener::class.java, l)
//    }
//
//    private fun fireEditingStopped() {
//        val listeners = listenerList.listenerList
//        for (i in listeners.indices) {
//            if (listeners[i] is CellEditorListener) {
//                (listeners[i] as CellEditorListener).editingStopped(ChangeEvent(this))
//            }
//        }
//    }
//
//    private fun fireEditingCanceled() {
//        val listeners = listenerList.listenerList
//        for (i in listeners.indices) {
//            if (listeners[i] is CellEditorListener) {
//                (listeners[i] as CellEditorListener).editingCanceled(ChangeEvent(this))
//            }
//        }
//    }
//
//}
//
//internal class CaptionCellEditor(
//    private val darkTheme: Boolean,
//    private val vocabulary: MutableVocabulary,
//    private val subtitlesVocabularyMap: MutableMap<String, HashMap<String, List<Caption>>>
//) : TableCellEditor {
//    private var listenerList = EventListenerList()
//
//    //    private var values = Vector<String>()
//    private val model = DefaultListModel<String>()
//    private val list = JList(model)
//    private val popupMenu = JPopupMenu()
//    private val deleteItem = JMenuItem("删除字幕")
//    private val addItem = JMenuItem("添加字幕")
//
//    private var links = mutableListOf<String>()
//    private var editedRow = -1
//
//    init {
//        addItem.addActionListener {
//            val point = MouseInfo.getPointerInfo().location
//            val x =point.x-110
//            val y = point.y-20
//
//            val comboBox = JComboBox<String>()
//            comboBox.addItem("请选择字幕")
//            comboBox.size = Dimension(300,30)
//            val confirm = JButton("确定")
//            val cancel = JButton("取消")
//
//            val panel = JPanel()
//            panel.size = Dimension(500, 40)
//            panel.add(comboBox)
//            panel.add(confirm)
//            panel.add(cancel)
//
//
//            val factory = PopupFactory.getSharedInstance()
//            val popup = factory.getPopup(list,panel,x,y)
//            popup.show()
//            confirm.addActionListener {
//                popup.hide()
//            }
//            cancel.addActionListener {
//                popup.hide()
//            }
//
//
//        }
//        deleteItem.addActionListener {
//            model.remove(list.selectedIndex)
//        }
//        popupMenu.add(addItem)
//        popupMenu.add(deleteItem)
//        list.componentPopupMenu = popupMenu
//    }
//
//    override fun getCellEditorValue(): Any {
//        var captions = ""
//        if (vocabulary.type == VocabularyType.DOCUMENT) {
//            captions = getDisplayCaption(links, subtitlesVocabularyMap)
//        } else {
//
//            model.elements().toList().forEachIndexed { index, caption ->
//                var num = (index + 1).toString() + ". "
//                captions += num + caption
//                var isNewline = index + 1 == captions.length
//                if (!isNewline) captions += "\r\n"
//            }
//        }
//        // 需要清理
//        model.removeAllElements()
//        links.clear()
//        return captions
//    }
//
//
//    override fun getTableCellEditorComponent(
//        table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
//    ): Component {
//        val word = vocabulary.wordList[row]
//        if (vocabulary.type == VocabularyType.DOCUMENT) {
//            links = word.links
//            val captions = Vector(getDisplayCaption(links, subtitlesVocabularyMap).split("\r\n"))
//            captions.forEach { caption ->
////                values.add(caption)
//                model.addElement(caption)
//            }
//        } else {
//            word.captions.forEach { caption ->
////                values.add(caption.content)
//                model.addElement(caption.content)
//            }
//
//        }
//
//        editedRow = row
//
//        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
//        val outsideBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 175, 0))
//        list.border = CompoundBorder(outsideBorder, insideBorder)
//        list.font = table.font
//        return list
//    }
//
//    override fun isCellEditable(event: EventObject): Boolean {
//        return if (event is MouseEvent) {
//            event.clickCount > 1
//        } else {
//            false
//        }
//
//    }
//
//    override fun shouldSelectCell(e: EventObject): Boolean {
//        return true
//    }
//
//    override fun stopCellEditing(): Boolean {
//        fireEditingStopped()
//        return true
//    }
//
//    override fun cancelCellEditing() {
//        fireEditingCanceled()
//    }
//
//    override fun addCellEditorListener(l: CellEditorListener) {
//        listenerList.add(CellEditorListener::class.java, l)
//    }
//
//    override fun removeCellEditorListener(l: CellEditorListener) {
//        listenerList.remove(CellEditorListener::class.java, l)
//    }
//
//    private fun fireEditingStopped() {
//        val listeners = listenerList.listenerList
//        for (i in listeners.indices) {
//            if (listeners[i] is CellEditorListener) {
//                (listeners[i] as CellEditorListener).editingStopped(ChangeEvent(this))
//            }
//        }
//    }
//
//    private fun fireEditingCanceled() {
//        val listeners = listenerList.listenerList
//        for (i in listeners.indices) {
//            if (listeners[i] is CellEditorListener) {
//                (listeners[i] as CellEditorListener).editingCanceled(ChangeEvent(this))
//            }
//        }
//    }
//
//
//}
//
//
//
//
