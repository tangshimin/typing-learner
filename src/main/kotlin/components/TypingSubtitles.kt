package components

import LocalCtrl
import Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matthewn4444.ebml.EBMLReader
import data.Caption
import dialog.removeItalicSymbol
import dialog.removeLocationInfo
import dialog.replaceNewLine
import kotlinx.coroutines.launch
import player.isMacOS
import player.mediaPlayer
import state.GlobalState
import state.TypingSubtitlesState
import state.getSettingsDirectory
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.TransferHandler
import javax.swing.filechooser.FileSystemView

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TypingSubtitles(
    typingSubtitles : TypingSubtitlesState,
    globalState: GlobalState,
    saveSubtitlesState:() -> Unit,
    saveGlobalState:() -> Unit,
    toTypingWord: () -> Unit,
    isOpenSettings: Boolean,
    setIsOpenSettings: (Boolean) -> Unit,
    window: ComposeWindow,
    title:String,
    playerWindow: JFrame,
    videoVolume: Float,
    mediaPlayerComponent: Component,
    futureFileChooser: FutureTask<JFileChooser>,
    closeLoadingDialog: () -> Unit,
    wrongColor: Color
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val captionList = remember{ mutableStateListOf<Caption>()}
    var isPlaying by remember { mutableStateOf(false) }
    var showOpenFile by remember { mutableStateOf(false) }
    var selectedPath by remember { mutableStateOf("") }
    var showSelectTrack by remember { mutableStateOf(false) }
    val trackList = remember { mutableStateListOf<Pair<Int, String>>() }
    val videoPlayerBounds by remember { mutableStateOf(Rectangle(0, 0, 540, 303)) }

    if (typingSubtitles.subtitlesPath.isNotEmpty() && captionList.isEmpty()) {
        parseSubtitles(
            subtitlesPath = typingSubtitles.subtitlesPath,
            setMaxLength = {
                scope.launch {
                    typingSubtitles.sentenceMaxLength = it
                    saveSubtitlesState()
                }
            },
            setCaptionList = {
                captionList.clear()
                captionList.addAll(it)
            }
        )
    }

    /** 播放按键音效 */
    val playKeySound = {
        if (globalState.isPlayKeystrokeSound) {
            playSound("audio/keystroke.wav", globalState.keystrokeVolume)
        }
    }

    /** 设置字幕列表的被回调函数 */
    val setTrackList: (List<Pair<Int, String>>) -> Unit = {
        trackList.clear()
        trackList.addAll(it)
    }

    /** 打开文件 */
    val openFileChooser: () -> Unit = {
        val fileChooser = futureFileChooser.get()
        fileChooser.dialogTitle = "选择 MKV 视频"
        fileChooser.fileSystemView = FileSystemView.getFileSystemView()
        fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.selectedFile = null
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            if (typingSubtitles.videoPath != file.absolutePath) {
                selectedPath = file.absolutePath
                parseTrackList(
                    mediaPlayerComponent,
                    playerWindow,
                    file.absolutePath,
                    setTrackList = { setTrackList(it) },
                )
            } else {
                JOptionPane.showMessageDialog(null, "文件已打开")
            }

            closeLoadingDialog()
        } else {
            closeLoadingDialog()
        }
    }

    /**  使用按钮播放视频时调用的回调函数   */
    val buttonEventPlay: (Caption) -> Unit = { caption ->
        val file = File(typingSubtitles.videoPath)
        if (file.exists()) {
            if (!isPlaying) {
                scope.launch {
                    isPlaying = true
                    val playTriple = Triple(caption, typingSubtitles.videoPath, typingSubtitles.trackID)
                    play(
                        window = playerWindow,
                        setIsPlaying = { isPlaying = it },
                        volume = videoVolume,
                        playTriple = playTriple,
                        videoPlayerComponent = mediaPlayerComponent,
                        bounds = videoPlayerBounds
                    )
                }
            }

        } else {
            println("视频地址错误")
        }

    }

    /** 保存轨道 ID 时被调用的回调函数 */
    val saveTrackID:(Int) -> Unit =  {
        scope.launch {
            typingSubtitles.trackID = it
            saveSubtitlesState()
        }
    }

    /** 保存轨道名称时被调用的回调函数 */
    val saveTrackDescription:(String) -> Unit = {
        scope.launch {
            typingSubtitles.trackDescription = it
            saveSubtitlesState()
        }
    }

    /** 保存轨道数量时被调用的回调函数 */
    val saveTrackSize:(Int) -> Unit = {
        scope.launch {
            typingSubtitles.trackSize = it
            saveSubtitlesState()
        }
    }

    /** 保存视频路径时被调用的回调函数 */
    val saveVideoPath:(String) -> Unit = {
        typingSubtitles.videoPath = it
        saveSubtitlesState()
    }

    /** 保存一个新的字幕时被调用的回调函数 */
    val saveSubtitlesPath:(String) -> Unit = {
        scope.launch {
            typingSubtitles.subtitlesPath = it
            typingSubtitles.firstVisibleItemIndex = 0
            typingSubtitles.currentIndex = 0
            focusManager.clearFocus()
            /** 把之前的字幕列表清除才能触发解析字幕的函数重新运行 */
            captionList.clear()
            saveSubtitlesState()
        }
    }

    /** 保存是否时深色模式时被调用的回调函数 */
    val saveIsDarkTheme:(Boolean) -> Unit = {
        scope.launch {
            globalState.isDarkTheme = it
            saveGlobalState()
        }
    }

    /** 保存是否启用击键音效时被调用的回调函数 */
    val saveIsPlayKeystrokeSound:(Boolean) -> Unit = {
        scope.launch {
            globalState.isPlayKeystrokeSound = it
            saveGlobalState
        }
    }

    /** 当前界面的快捷键 */
    val boxKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        when {
            (keyEvent.isCtrlPressed && keyEvent.key == Key.T && keyEvent.type == KeyEventType.KeyUp) -> {
                toTypingWord()
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.O && keyEvent.type == KeyEventType.KeyUp) -> {
                openFileChooser()
                showOpenFile = true
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.S && keyEvent.type == KeyEventType.KeyUp) -> {
                showSelectTrack = true
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.D && keyEvent.type == KeyEventType.KeyUp) -> {
                saveIsDarkTheme(!globalState.isDarkTheme)
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.M && keyEvent.type == KeyEventType.KeyUp) -> {
                saveIsPlayKeystrokeSound(!globalState.isPlayKeystrokeSound)
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.Z && keyEvent.type == KeyEventType.KeyUp) -> {
                val caption = captionList[typingSubtitles.currentIndex]
                val playTriple = Triple(caption, typingSubtitles.videoPath, typingSubtitles.trackID)
                if (!isPlaying) {
                    scope.launch {
                        isPlaying = true
                        play(
                            window = playerWindow,
                            setIsPlaying = { isPlaying = it },
                            volume = videoVolume,
                            playTriple = playTriple,
                            videoPlayerComponent = mediaPlayerComponent,
                            bounds = videoPlayerBounds
                        )
                    }

                }
                true
            }
            else -> false
        }


    }


    /**  处理拖放文件的函数 */
    val transferHandler = createTransferHandler(
        showWrongMessage = { message ->
            JOptionPane.showMessageDialog(window, message)
        },
        parseImportFile = { files ->
            val file = files.first()
            scope.launch {
                if (file.extension == "mkv") {
                    if (typingSubtitles.videoPath != file.absolutePath) {
                        selectedPath = file.absolutePath
                        parseTrackList(
                            mediaPlayerComponent,
                            playerWindow,
                            file.absolutePath,
                            setTrackList = { setTrackList(it) },
                        )
                    } else {
                        JOptionPane.showMessageDialog(window, "文件已打开")
                    }

                } else if(file.extension == "json"){
                    JOptionPane.showMessageDialog(window, "想要打开词库文件，需要先切换到记忆单词界面")
                }
                else {
                    JOptionPane.showMessageDialog(window, "暂时只能读取 mkv 格式的视频文件")
                }


            }
        }
    )

    window.transferHandler = transferHandler

    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colors.background)
            .focusRequester(focusRequester)
            .onKeyEvent(boxKeyEvent)
            .focusable()
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Row(Modifier.fillMaxSize()) {
            SubtitlesSidebar(
                isOpen = isOpenSettings,
                back = { toTypingWord() },
                trackSize = typingSubtitles.trackSize,
                openFile = { showOpenFile = true },
                openFileChooser = { openFileChooser() },
                selectTrack = { showSelectTrack = true },
                isDarkTheme = globalState.isDarkTheme,
                setIsDarkTheme = { saveIsDarkTheme(it) },
                isPlayKeystrokeSound = globalState.isPlayKeystrokeSound,
                setIsPlayKeystrokeSound = { saveIsPlayKeystrokeSound(it) },
            )
            if (isOpenSettings) {
                val topPadding = if (isMacOS()) 30.dp else 0.dp
                Divider(Modifier.fillMaxHeight().width(1.dp).padding(top = topPadding))
            }
            Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {

                if (captionList.isNotEmpty()) {

                    val listState = rememberLazyListState(typingSubtitles.firstVisibleItemIndex)
                    val stateHorizontal = rememberScrollState(0)
                    val isAtTop by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                        }
                    }
                    val startPadding = 150.dp
                    val startTimeWidth = 141.dp
                    val endPadding = 10.dp
                    val maxWidth = startPadding + startTimeWidth + endPadding + (typingSubtitles.sentenceMaxLength * 13).dp
                    LazyColumn(
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .horizontalScroll(stateHorizontal),
                    ) {
                        itemsIndexed(captionList) { index, caption ->
                            val captionContent = caption.content
                            val typingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }
                            var textFieldValue by remember { mutableStateOf("") }

                            /** 检查输入的回调函数 */
                            val checkTyping: (String) -> Unit = { input ->
                                scope.launch {
                                    if (textFieldValue.length > captionContent.length) {
                                        typingResult.clear()
                                        textFieldValue = ""
                                    } else if (input.length <= captionContent.length) {
                                        textFieldValue = input
                                        typingResult.clear()
                                        val inputChars = input.toList()
                                        for (i in inputChars.indices) {
                                            val inputChar = inputChars[i]
                                            val char = captionContent[i]
                                            if (inputChar == char) {
                                                typingResult.add(Pair(inputChar, true))
                                                // 方括号的语义很弱，又不好输入，所以可以使用空格替换
                                            } else if (inputChar == ' ' && (char == '[' || char == ']')) {
                                                typingResult.add(Pair(char, true))
                                            } else {
                                                typingResult.add(Pair(inputChar, false))
                                            }
                                        }

                                    }
                                }
                            }
                            val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                                if ((it.key != Key.ShiftLeft && it.key != Key.ShiftRight) && it.type == KeyEventType.KeyDown) {
                                    playKeySound()
                                }
                                if ((it.key == Key.Enter || it.key == Key.NumPadEnter)
                                    && it.type == KeyEventType.KeyUp
                                ) {
                                    scope.launch {
                                        typingResult.clear()
                                        textFieldValue = ""
                                        val end =
                                            listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size - 1
                                        if (index >= end) {
                                            listState.scrollToItem(index)
                                        }
                                        focusManager.moveFocus(FocusDirection.Next)
                                        focusManager.moveFocus(FocusDirection.Next)
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                    true
                                } else false
                            }
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(maxWidth)
                                    .padding(start = 150.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color.DarkGray,
                                                fontSize = MaterialTheme.typography.h5.fontSize,
                                                letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                fontFamily = MaterialTheme.typography.h5.fontFamily,
                                            )
                                        ) {
                                            append(caption.start)
                                        }
                                    },
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Box(Modifier.width(IntrinsicSize.Max)) {
                                    CompositionLocalProvider(
                                        LocalTextInputService provides null
                                    ) {
                                        BasicTextField(
                                            value = textFieldValue,
                                            onValueChange = { checkTyping(it) },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                            textStyle = MaterialTheme.typography.h5.copy(
                                                color = Color.Transparent,
                                                fontFamily = FontFamily.Monospace
                                            ),

                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp)
                                                .align(Alignment.CenterStart)
                                                .focusable()
                                                .onKeyEvent { textFieldKeyEvent(it) }
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        scope.launch{
                                                            typingSubtitles.currentIndex = index
                                                            typingSubtitles.firstVisibleItemIndex = listState.firstVisibleItemIndex
                                                            saveSubtitlesState()
                                                        }
                                                    } else if (textFieldValue.isNotEmpty()) {
                                                        typingResult.clear()
                                                        textFieldValue = ""
                                                    }
                                                }
                                        )
                                    }
                                    Text(
                                        text = buildAnnotatedString {
                                            typingResult.forEach { (char, correct) ->
                                                if (correct) {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = MaterialTheme.colors.primary,
                                                            fontSize = MaterialTheme.typography.h5.fontSize,
                                                            letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                            fontFamily = FontFamily.Monospace,
                                                        )
                                                    ) {
                                                        append(char)
                                                    }
                                                } else {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = wrongColor,
                                                            fontSize = MaterialTheme.typography.h5.fontSize,
                                                            letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                            fontFamily = FontFamily.Monospace,
                                                        )
                                                    ) {
                                                        if (char == ' ') {
                                                            append("_")
                                                        } else {
                                                            append(char)
                                                        }

                                                    }
                                                }
                                            }
                                            var remainChars = captionContent.substring(typingResult.size)
                                            withStyle(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colors.onBackground,
                                                    fontSize = MaterialTheme.typography.h5.fontSize,
                                                    letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                    fontFamily = FontFamily.Monospace,
                                                )
                                            ) {
                                                append(remainChars)
                                            }
                                        },
                                        textAlign = TextAlign.Start,
                                        color = MaterialTheme.colors.onBackground,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                    )

                                    if (typingSubtitles.currentIndex == index) {
                                        Divider(
                                            Modifier.align(Alignment.BottomCenter)
                                                .background(MaterialTheme.colors.primary)
                                        )
                                    }
                                }

                                Row(Modifier.width(48.dp).height(48.dp)) {
                                    if (typingSubtitles.currentIndex == index) {
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    elevation = 4.dp,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                    ),
                                                    shape = RectangleShape
                                                ) {
                                                    val ctrl = LocalCtrl.current
                                                    val shift = if (isMacOS()) "⇧" else "Shift"
                                                    Text(text = "播放 $ctrl+$shift+Z", modifier = Modifier.padding(10.dp))
                                                }
                                            },
                                            delayMillis = 300,
                                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                                anchor = Alignment.CenterEnd,
                                                alignment = Alignment.CenterEnd,
                                                offset = DpOffset.Zero
                                            )
                                        ) {
                                            IconButton(onClick = {
                                                buttonEventPlay(caption)
                                            },
                                                modifier = Modifier
                                                    .onKeyEvent {
                                                        if (it.key == Key.Spacebar && it.type == KeyEventType.KeyUp) {
                                                            buttonEventPlay(caption)
                                                            true
                                                        } else false
                                                    }
                                                    .onGloballyPositioned { coordinates ->
                                                        val rect = coordinates.boundsInWindow()
                                                        videoPlayerBounds.x = window.x + rect.left.toInt() + 48
                                                        videoPlayerBounds.y = window.y + rect.top.toInt() - 100

                                                        // 判断屏幕边界
                                                        val graphicsDevice =
                                                            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
                                                        val width = graphicsDevice.displayMode.width
                                                        val height = graphicsDevice.displayMode.height
                                                        if (videoPlayerBounds.x + 540 > width) {
                                                            videoPlayerBounds.x = width - 540
                                                        }

                                                        if (videoPlayerBounds.y < 0) videoPlayerBounds.y = 0
                                                        if (videoPlayerBounds.y + 303 > height) {
                                                            videoPlayerBounds.y = height - 303
                                                        }

                                                    }
                                            ) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = "Localized description",
                                                    tint = MaterialTheme.colors.primary
                                                )
                                            }

                                        }

                                    }
                                }

                            }

                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                    HorizontalScrollbar(
                        modifier = Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        adapter =  rememberScrollbarAdapter(stateHorizontal)
                    )
                    if (!isAtTop) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(0)
                                    typingSubtitles.currentIndex = 0
                                    typingSubtitles.firstVisibleItemIndex = 0
                                    focusManager.clearFocus()
                                    saveSubtitlesState()
                                }
                            },
                            backgroundColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 100.dp, bottom = 100.dp)
                        ) {
                            Icon(
                                Icons.Filled.North,
                                contentDescription = "Localized description",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                }

                if (showOpenFile || selectedPath.isNotEmpty() || captionList.isEmpty()) {
                    OpenFileComponent(
                        cancel = { showOpenFile = false },
                        openFileChooser = { openFileChooser() },
                        showCancel = captionList.isNotEmpty(),
                        setTrackId = { saveTrackID(it) },
                        setTrackDescription = { saveTrackDescription(it) },
                        trackList = trackList,
                        setTrackList = { setTrackList(it) },
                        setVideoPath = { saveVideoPath(it) },
                        selectedPath = selectedPath,
                        setSelectedPath = { selectedPath = it },
                        setSubtitlesPath = { saveSubtitlesPath(it) },
                        setTrackSize = { saveTrackSize(it) },
                    )
                }
                if (showSelectTrack) {
                    Box(
                        Modifier.fillMaxSize()
                            .align(Alignment.Center)
                            .background(MaterialTheme.colors.background)
                    ) {
                        var loading by remember { mutableStateOf(false) }
                        Row(Modifier.align(Alignment.Center)) {
                            if (trackList.isEmpty()) {
                                parseTrackList(
                                    mediaPlayerComponent,
                                    playerWindow,
                                    typingSubtitles.videoPath,
                                    setTrackList = { setTrackList(it) },
                                )
                            }
                            SelectTrack(
                                close = { showSelectTrack = false },
                                setTrackId = { saveTrackID(it) },
                                setTrackDescription = { saveTrackDescription(it) },
                                trackList = trackList,
                                setTrackList = { setTrackList(it) },
                                setVideoPath = { saveVideoPath(it) },
                                selectedPath = typingSubtitles.videoPath,
                                setSelectedPath = { selectedPath = it },
                                setSubtitlesPath = { saveSubtitlesPath(it) },
                                setTrackSize = { saveTrackSize(it) },
                                setIsLoading = { loading = it }
                            )
                            OutlinedButton(onClick = { showSelectTrack = false }) {
                                Text("取消")
                            }
                        }
                        if (loading || trackList.isEmpty()) {
                            CircularProgressIndicator(
                                Modifier.width(60.dp).align(Alignment.Center).padding(bottom = 200.dp)
                            )
                        }
                    }

                }
            }

        }

        if(isMacOS()){
            MacOSTitle(
                title = title,
                window = window,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
            )
        }
        Settings(
            isOpen = isOpenSettings,
            setIsOpen = { setIsOpenSettings(it) },
            modifier = Modifier.align(Alignment.TopStart)
        )

    }

}

@Composable
fun OpenFileComponent(
    cancel: () -> Unit,
    openFileChooser: () -> Unit,
    showCancel: Boolean,
    setTrackId: (Int) -> Unit,
    setTrackDescription: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    setVideoPath: (String) -> Unit,
    selectedPath: String,
    setSelectedPath: (String) -> Unit,
    setSubtitlesPath: (String) -> Unit,
    setTrackSize: (Int) -> Unit,
) {

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        var loading by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(IntrinsicSize.Max).align(Alignment.Center)
        ) {

           Text(text = "可以拖放 MKV 文件到这里",
               color = MaterialTheme.colors.primary,
           modifier = Modifier.padding(end = 20.dp))
            OutlinedButton(
                modifier = Modifier.padding(end = 20.dp),
                onClick = { openFileChooser() }) {
                Text("打开")
            }

            SelectTrack(
                close = { cancel() },
                setTrackId = { setTrackId(it) },
                setTrackDescription = { setTrackDescription(it) },
                trackList = trackList,
                setTrackList = { setTrackList(it) },
                setVideoPath = { setVideoPath(it) },
                selectedPath = selectedPath,
                setSelectedPath = { setSelectedPath(it) },
                setSubtitlesPath = { setSubtitlesPath(it) },
                setTrackSize = { setTrackSize(it) },
                setIsLoading = { loading = it }
            )
            if (showCancel) {
                OutlinedButton(onClick = {
                    setTrackList(listOf())
                    setSelectedPath("")
                    cancel()
                }) {
                    Text("取消")
                }
            }
        }
        if (loading) {
            CircularProgressIndicator(Modifier.width(60.dp).align(Alignment.Center).padding(bottom = 200.dp))
        }
    }

}

@Composable
fun SelectTrack(
    close: () -> Unit,
    setTrackId: (Int) -> Unit,
    setTrackDescription: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    setVideoPath: (String) -> Unit,
    selectedPath: String,
    setSelectedPath: (String) -> Unit,
    setSubtitlesPath: (String) -> Unit,
    setTrackSize: (Int) -> Unit,
    setIsLoading: (Boolean) -> Unit,
) {
    if (trackList.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        var selectedSubtitle by remember { mutableStateOf("    ") }
        Box(Modifier.width(IntrinsicSize.Max).padding(end = 20.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .width(282.dp)
                    .background(Color.Transparent)
                    .border(1.dp, Color.Transparent)
            ) {
                Text(
                    text = selectedSubtitle, fontSize = 12.sp,
                )
                Icon(
                    Icons.Default.ExpandMore, contentDescription = "Localized description",
                    modifier = Modifier.size(20.dp, 20.dp)
                )
            }
            val dropdownMenuHeight = (trackList.size * 40 + 20).dp
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(282.dp)
                    .height(dropdownMenuHeight)
            ) {
                trackList.forEach { (trackId, description) ->
                    DropdownMenuItem(
                        onClick = {
                            setIsLoading(true)
                            Thread(Runnable {
                                expanded = false
                                setTrackId(trackId)
                                setTrackDescription(description)
                                setVideoPath(selectedPath)
                                val subtitles = writeToFile(selectedPath, trackId)
                                if (subtitles != null) {
                                    setSubtitlesPath(subtitles.absolutePath)
                                }
                                setTrackSize(trackList.size)
                                setTrackList(listOf())
                                setSelectedPath("")
                                setIsLoading(false)
                                close()
                            }).start()
                        },
                        modifier = Modifier.width(282.dp).height(40.dp)
                    ) {
                        Text(
                            text = "$description ", fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun Settings(
    modifier: Modifier
) {
    Icon(
        Icons.Filled.ArrowBack,
        contentDescription = "Localized description",
        tint = MaterialTheme.colors.primary,
        modifier = modifier,
    )
}

@Composable
fun SubtitlesSidebar(
    isOpen: Boolean,
    isDarkTheme: Boolean,
    setIsDarkTheme: (Boolean) -> Unit,
    isPlayKeystrokeSound: Boolean,
    setIsPlayKeystrokeSound: (Boolean) -> Unit,
    trackSize: Int,
    back: () -> Unit,
    openFile: () -> Unit,
    openFileChooser: () -> Unit,
    selectTrack: () -> Unit,
) {
    if (isOpen) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.fillMaxWidth().height(if (isMacOS()) 78.dp else 48.dp))
            Divider()
            val ctrl = LocalCtrl.current

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(48.dp).clickable { back() }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("记忆单词", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+T",
                        color = MaterialTheme.colors.onBackground
                    )
                }
                Spacer(Modifier.width(15.dp))
                Icon(
                    Icons.Filled.TextFields,
                    contentDescription = "Localized description",
                    tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        openFileChooser()
                        openFile()
                    }
                    .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("打开文件", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+O",
                        color = MaterialTheme.colors.onBackground
                    )
                }
                Spacer(Modifier.width(15.dp))
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = "Localized description",
                    tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }
            if (trackSize > 1) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { selectTrack() }
                        .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("选择字幕", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+S",
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Localized description",
                        tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("深色模式", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+D",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isDarkTheme,
                    onCheckedChange = { setIsDarkTheme(it) },
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("击键音效", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+M",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isPlayKeystrokeSound,
                    onCheckedChange = { setIsPlayKeystrokeSound(it) },
                )
            }

        }
    }
}

 /** 创建拖放处理器
  * @param singleFile 是否只接收单个文件
  * @param parseImportFile 处理导入的文件的函数
  * @param showWrongMessage 显示提示信息的函数
  */
fun createTransferHandler(
    singleFile:Boolean = true,
    parseImportFile: (List<File>) -> Unit,
    showWrongMessage: (String) -> Unit,
): TransferHandler {
    return object : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val transferable = support.transferable
            try {
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                if(singleFile){
                    if (files.size == 1) {
                        parseImportFile(files)
                    } else {
                        showWrongMessage("一次只能读取一个文件")
                    }
                }else{
                    parseImportFile(files)
                }


            } catch (exception: UnsupportedFlavorException) {
                return false
            } catch (exception: IOException) {
                return false
            }
            return true
        }
    }
}

/**
 * 解析选择的文件，返回字幕名称列表，用于用户选择具体的字幕。
 * @param mediaPlayerComponent VLC 组件
 * @param playerWindow 播放视频的窗口
 * @param videoPath 视频路径
 * @param setTrackList 解析完成后，用来设置字幕列表的回调。
 */
fun parseTrackList(
    mediaPlayerComponent: Component,
    playerWindow: JFrame,
    videoPath: String,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
) {
    mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            val list = mutableListOf<Pair<Int, String>>()
            mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index, trackDescription ->
                if (index != 0) {
                    list.add(Pair(index - 1, trackDescription.description()))
                }
            }
            mediaPlayer.controls().pause()
            playerWindow.isAlwaysOnTop = true
            playerWindow.title = "视频播放窗口"
            playerWindow.isVisible = false
            setTrackList(list)
            mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    playerWindow.title = "正在读取字幕列表"
    playerWindow.isAlwaysOnTop = false
    playerWindow.toBack()
    playerWindow.size = Dimension(10, 10)
    playerWindow.location = Point(0, 0)
    playerWindow.layout = null
    playerWindow.contentPane.add(mediaPlayerComponent)
    playerWindow.isVisible = true
    // 打开了一个 ASS 字幕为默认轨道的 MKV 文件，再打开另一个 MKV 文件会可能出现 `Invalid memory access` 错误
    mediaPlayerComponent.mediaPlayer().media().play(videoPath)
}

/**
 * 解析字幕，返回最大字符数和字幕列表，用于显示。
 * @param subtitlesPath 字幕的路径
 * @param setMaxLength 用于设置字幕的最大字符数的回调函数
 * @param setCaptionList 用于设置字幕列表的回调函数
 */
fun parseSubtitles(
    subtitlesPath:String,
    setMaxLength:(Int) -> Unit,
    setCaptionList:(List<Caption>) -> Unit,
){
    val formatSRT = FormatSRT()
    val file = File(subtitlesPath)
    val inputStream: InputStream = FileInputStream(file)
    val timedTextObject: TimedTextObject =  formatSRT.parseFile(file.name, inputStream)
    val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
    val captionList = mutableListOf<Caption>()
    var maxLength = 0
    for (caption in captions.values) {
        var content = removeLocationInfo(caption.content)
        content = removeItalicSymbol(content)
        content = replaceNewLine(content)

        val newCaption = Caption(
            start = caption.start.getTime("hh:mm:ss.ms"),
            end = caption.end.getTime("hh:mm:ss.ms"),
            content = content
        )
        if (caption.content.length > maxLength) {
            maxLength = caption.content.length
        }
        captionList.add(newCaption)
    }

    setMaxLength(maxLength)
    setCaptionList(captionList)
}

/**
 * 提取选择的字幕到用户目录
 * */
private fun writeToFile(
    videoPath: String,
    trackId: Int,
): File? {
    var reader: EBMLReader? = null
    val settingsDir = getSettingsDirectory()
    var subtitlesFile: File? = null

    try {
        reader = EBMLReader(videoPath)
        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return subtitlesFile
        }

        /**
         * Read the tracks. This contains the details of video, audio and subtitles
         * in this file
         */
        reader.readTracks()

        /**
         * Check if there are any subtitles in this file
         */
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            return subtitlesFile
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()

        /**
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }

        val subtitles = reader.subtitles[trackId]
        subtitlesFile = File(settingsDir, "subtitles.srt")
        subtitles.writeFile(subtitlesFile.absolutePath)
    } catch (exception: Exception) {
        exception.printStackTrace()
    } finally {
        try {
            reader?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return subtitlesFile
}