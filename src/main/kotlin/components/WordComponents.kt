import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import components.Captions
import components.getPlayTripleMap
import data.Dictionary
import data.Word
import data.saveVocabulary
import dialog.ChapterFinishedDialog
import player.AudioButton
import state.AppState
import state.getResourcesFile
import java.awt.Rectangle
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import kotlin.concurrent.fixedRateTimer


@OptIn(ExperimentalFoundationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WordComponents(
    state: AppState,
    word: Word,
    correctTime: Int,
    wrongTime: Int,
    toNext: () -> Unit,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (String) -> Unit,
    showChapterFinishedDialog: Boolean,
    changeShowChapterFinishedDialog: (Boolean) -> Unit,
    showEditWordDialog: Boolean,
    changeShowEditWordDialog: (Boolean) -> Unit,
    isVocabularyFinished: Boolean,
    setIsVocabularyFinished: (Boolean) -> Unit,
    chapterCorrectTime: Float,
    chapterWrongTime: Float,
    dictationWrongWords: Map<Word, Int>,
    resetChapterTime: () -> Unit,
    playKeySound: () -> Unit,

) {
    val wordValue = word.value
    val focusRequester = remember { FocusRequester() }
    var lastInputTime by remember { mutableStateOf(0L) }
    var activeMenu by remember { mutableStateOf(false) }
    val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
        when {
            ((it.key == Key.Enter || it.key == Key.NumPadEnter)
                    && it.type == KeyEventType.KeyUp) -> {
                toNext()
                true
            }
            (it.type == KeyEventType.KeyDown
                    && !it.isCtrlPressed
                    && !it.isAltPressed
                    && it.key != Key.Escape
                    && it.key != Key.Enter
                    && it.key != Key.NumPadEnter
                    ) -> {
                playKeySound()
                lastInputTime = System.currentTimeMillis()
                // 计时器
                if (!state.isStart) {
                    state.isStart = true
                    state.timer = fixedRateTimer("timer", false, 0L, 1 * 1000) {
                        state.time = state.time.plusSeconds(1)
                    }
                    // 超过一分钟没有输入自动暂停
                    state.autoPauseTimer =
                        fixedRateTimer("autoPause", false, 0L, 60 * 1000) {
                            val span = System.currentTimeMillis() - lastInputTime
                            if (span > 60 * 1000) {
                                state.isStart = false
                                state.timer.cancel()
                            }
                        }
                }
                true
            }
            else -> false
        }

    }
    Box(
        Modifier.onPointerEvent(PointerEventType.Exit) { activeMenu = false }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 48.dp).height(66.dp)
        ) {
            Box(Modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .padding(start = 50.dp)
                .onPointerEvent(PointerEventType.Enter) { activeMenu = true }) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { input ->
                        checkTyping(input)
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        color = Color.Transparent,
                        fontSize = 3.5.em,
                        letterSpacing = 5.sp,
                        fontFamily = FontFamily.Monospace
                    ),

                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .focusRequester(focusRequester)
                        .onKeyEvent { textFieldKeyEvent(it) }

                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = buildAnnotatedString {
                        typingResult.forEach { (char, correct) ->
                            if (correct) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.primary,
                                        fontSize = 3.5.em,
                                        letterSpacing = 5.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                ) {
                                    append(char)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.Red,
                                        fontSize = 3.5.em,
                                        letterSpacing = 5.sp,
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
                        val remainChars = wordValue.substring(typingResult.size)
                        if (state.isDictation && !state.isReviewWrongList) {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.onBackground,
                                    fontSize = 3.5.em,
                                    letterSpacing = 5.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            ) {
                                repeat(remainChars.length) {
                                    append(" ")
                                }

                            }
                        } else {
                            if (state.typing.wordVisible) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = 3.5.em,
                                        letterSpacing = 5.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                ) {
                                    append(remainChars)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = 3.5.em,
                                        letterSpacing = 5.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                ) {
                                    repeat(remainChars.length) {
                                        append("_")
                                    }
                                }

                            }

                        }
                    }
                )
            }
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "${if (correctTime > 0) correctTime else ""}", color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "${if (wrongTime > 0) wrongTime else ""}", color = Color.Red)
            }
            AudioButton(
                word = wordValue,
                pronunciation = state.typing.pronunciation,
            )
        }
        val clipboardManager = LocalClipboardManager.current
        if (activeMenu) {
            Row(modifier = Modifier.align(Alignment.TopCenter)) {
                var showConfirmationDialog by remember { mutableStateOf(false) }
                if (showConfirmationDialog) {
                    ConfirmationDelete(
                        message = "确定要删除单词 $wordValue ?",
                        confirm = {
                            Thread(Runnable {
                                val index = state.typing.index
                                state.vocabulary.wordList.removeAt(index)
                                state.vocabulary.size = state.vocabulary.wordList.size
                                saveVocabulary(
                                    state.vocabulary.vocabulary,
                                    state.typing.vocabularyPath
                                )
                            }).start()

                            showConfirmationDialog = false
                        },
                        close = { showConfirmationDialog = false }
                    )
                }

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "删除", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = { showConfirmationDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "编辑", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = {
                        changeShowEditWordDialog(true)
                    }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    if (showEditWordDialog) {
                        EditWord(
                            word = word,
                            state = state,
                            save = { newWord ->
                                val current = state.getCurrentWord()
                                val index = state.typing.index
                                newWord.captions = current.captions
                                newWord.links = current.links
                                state.vocabulary.wordList.removeAt(index)
                                state.vocabulary.wordList.add(index, newWord)
                                saveVocabulary(
                                    state.vocabulary.vocabulary,
                                    state.typing.vocabularyPath
                                )
                                changeShowEditWordDialog(false)
                            },
                            close = { changeShowEditWordDialog(false) }
                        )
                    }
                }

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "复制", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(wordValue))
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }

    }


    val decreaseIndex = {
        if (state.vocabulary.size > 19) state.typing.index -= 19
        else state.typing.index = 0
    }

    //正确率
    val correctRate: () -> Float = {
        if (chapterCorrectTime == 0F) {
            0F
        } else {
            println("chapterCorrectTime:$chapterCorrectTime,chapterWrongTime:$chapterWrongTime")
            println("rate: ${chapterCorrectTime.div(chapterCorrectTime + chapterWrongTime)}")
            val rateDouble = chapterCorrectTime.div(chapterCorrectTime + chapterWrongTime).toDouble()
            val rateD = BigDecimal(rateDouble).setScale(3, RoundingMode.HALF_EVEN)

            rateD.times(BigDecimal(100)).toFloat()
        }

    }

    // 重复学习本章
    val learnAgain: () -> Unit = {
        decreaseIndex()
        resetChapterTime()
        changeShowChapterFinishedDialog(false)
    }
    // 复习错误单词
    val reviewWrongWords: () -> Unit = {
        val reviewList = dictationWrongWords.keys.toList()
        state.enterReviewMode(reviewList)
        resetChapterTime()
        changeShowChapterFinishedDialog(false)
    }
    // 下一章
    val nextChapter: () -> Unit = {
        if (state.isDictation) state.exitDictationMode()
        state.typing.index += 1
        state.typing.chapter++
        resetChapterTime()
        state.saveTypingState()
        changeShowChapterFinishedDialog(false)
    }
    // 默写模式
    val enterDictation: () -> Unit = {
        // 正常地进入默写模式，或从复习错误单词进入默写模式
        if (!state.isDictation || state.isReviewWrongList) {
            state.isReviewWrongList = false
            state.enterDictationMode()
        } else {
            // 再默写一次
            state.dictationIndex = 0
            // 重新生成一个乱序的单词列表
            state.dictationWords = state.generateDictationWords(wordValue)
        }
        resetChapterTime()
        changeShowChapterFinishedDialog(false)
    }
    // 重置索引
    val resetIndex: (isShuffle: Boolean) -> Unit = { isShuffle ->

        state.typing.index = 0
        state.typing.chapter = 1
        if (isShuffle) {
            state.vocabulary.wordList.shuffle()
            saveVocabulary(
                state.vocabulary.vocabulary,
                state.typing.vocabularyPath
            )
        }
        state.saveTypingState()
        resetChapterTime()
        changeShowChapterFinishedDialog(false)
        setIsVocabularyFinished(false)

    }
    if (showChapterFinishedDialog) {
        ChapterFinishedDialog(
            close = { changeShowChapterFinishedDialog(false) },
            isVocabularyFinished = isVocabularyFinished,
            correctRate = correctRate(),
            isDictation = state.isDictation,
            isReviewWrongList = state.isReviewWrongList,
            dictationWrongWords = dictationWrongWords,
            enterDictation = { enterDictation() },
            learnAgain = { learnAgain() },
            reviewWrongWords = { reviewWrongWords() },
            nextChapter = { nextChapter() },
            resetIndex = { resetIndex(it) }
        )
    }

}

@Composable
fun Phonetic(
    word: Word,
    phoneticVisible: Boolean,
) {
    if (phoneticVisible) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (word.usphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "美:${word.usphone}",
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            if (word.ukphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "英:${word.ukphone}", color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@Composable
fun EditWord(
    word: Word,
    state: AppState,
    save: (Word) -> Unit,
    close: () -> Unit
) {
    Dialog(
        title = "编辑单词",
        onCloseRequest = { close() },
        undecorated = !MaterialTheme.colors.isLight,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(610.dp, 634.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        ) {

            var mutableWord by remember { mutableStateOf(word) }
            var inputWordStr by remember { mutableStateOf(TextFieldValue(word.value)) }
            var translationFieldValue by remember { mutableStateOf(TextFieldValue(word.translation)) }
            var definitionFieldValue by remember { mutableStateOf(TextFieldValue(word.definition)) }


            Column(Modifier.fillMaxSize()) {
                val textStyle = TextStyle(
                    color = MaterialTheme.colors.onBackground
                )
                val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                val modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                if (!MaterialTheme.colors.isLight) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("编辑单词", modifier = Modifier.align(Alignment.Center))
                        var isHover by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { close() },
                            modifier = Modifier
                                .onPointerEvent(PointerEventType.Enter) { isHover = true }
                                .onPointerEvent(PointerEventType.Exit) { isHover = false }
                                .background(if (isHover) Color(196, 43, 28) else Color.Transparent)
                                .align(Alignment.CenterEnd)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "",
                                tint = MaterialTheme.colors.onBackground,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                ) {
                    Text("单词：")
                    Spacer(Modifier.width(20.dp))
                    BasicTextField(
                        value = inputWordStr,
                        onValueChange = { inputWordStr = it },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = TextStyle(
                            lineHeight = 26.sp,
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onBackground
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .border(border = border).padding(start = 10.dp)
                    )
                    Spacer(Modifier.width(10.dp))

                    var updateFailed by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = {
                        Thread(Runnable {
                            val newWord = Dictionary.query(inputWordStr.text)
                            if (newWord != null) {
                                mutableWord = newWord
                                translationFieldValue = TextFieldValue(newWord.translation)
                                definitionFieldValue = TextFieldValue(newWord.definition)
                                updateFailed = false
                            } else {
                                updateFailed = true
                            }
                        }).start()
                    }) {
                        Text("查询")
                    }
                    if (updateFailed) {
                        Text("没有相关信息", color = Color.Red, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                val boxModifier = Modifier.fillMaxWidth().height(115.dp).border(border = border)
                Column(modifier = modifier) {
                    Text("中文释义：")
                    Box(modifier = boxModifier) {

                        val stateVertical = rememberScrollState(0)
                        BasicTextField(
                            value = translationFieldValue,
                            onValueChange = {
                                translationFieldValue = it
                            },
                            textStyle = textStyle,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            modifier = Modifier
                                .verticalScroll(stateVertical)
                                .fillMaxSize().padding(10.dp)
                        )
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(stateVertical),
                            style = LocalScrollbarStyle.current.copy(shape = RectangleShape),
                        )
                    }

                }
                Column(modifier = modifier) {
                    Text("英语释义：")
                    Box(modifier = boxModifier) {

                        val stateVertical = rememberScrollState(0)
                        BasicTextField(
                            value = definitionFieldValue,
                            onValueChange = {
                                definitionFieldValue = it
                            },
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = textStyle,
                            modifier = Modifier
                                .verticalScroll(stateVertical)
                                .fillMaxWidth().padding(10.dp)
                        )
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(stateVertical),
                            style = LocalScrollbarStyle.current.copy(shape = RectangleShape),
                        )
                    }

                }

                Captions(
                    captionsVisible = state.typing.subtitlesVisible,
                    playTripleMap = getPlayTripleMap(state, word),
                    vocabularyType = state.vocabulary.type,
                    videoPlayerWindow = state.videoPlayerWindow,
                    isPlaying = state.isPlaying,
                    volume = state.typing.audioVolume,
                    setIsPlaying = { state.isPlaying = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(intrinsicSize = IntrinsicSize.Max),
                    word = word,
                    isEditing = true,
                    bounds = Rectangle(0, 0, 0, 0),
                    textFieldValueList = listOf(),
                    typingResultMap = mapOf(),
                    putTypingResultMap = { _, _ -> },
                    checkTyping = { _, _, _ -> },
                    playKeySound = {},
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)
                ) {
                    OutlinedButton(onClick = {
                        // 单词可以从词典查询到，或者只修改了中文释义或英文释义
                        // TODO 保存字幕的方法在save() 方法里面，保存方法需要重构
                        if (inputWordStr.text == mutableWord.value) {
                            mutableWord.translation = translationFieldValue.text
                            mutableWord.definition = definitionFieldValue.text
                            save(mutableWord)
                        } else {
                            // 词典里没有这个单词，用户手动修改中文释义和英文释义
                            val newWord = Word(
                                value = inputWordStr.text,
                                usphone = "",
                                ukphone = "",
                                definition = definitionFieldValue.text,
                                translation = translationFieldValue.text,
                                pos = "",
                                collins = 0,
                                oxford = false,
                                tag = "",
                                bnc = 0,
                                frq = 0,
                                exchange = "",
                                links = mutableListOf(),
                                captions = mutableListOf()
                            )
                            save(newWord)
                        }

                    }) {
                        Text("保存")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { close() }) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun ConfirmationDelete(message: String, confirm: () -> Unit, close: () -> Unit) {
    Dialog(
        title = "删除",
        onCloseRequest = { close() },
        undecorated = true,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(400.dp, 300.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)
                ) {
                    Text("$message")
                }

                Spacer(Modifier.height(20.dp))
                Row {
                    OutlinedButton(onClick = { confirm() }) {
                        Text("确定")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { close() }) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun playSound(path: String, volume: Float) {
    try {
        val file = getResourcesFile(path)
        AudioSystem.getAudioInputStream(file).use { audioStream ->
            val format = audioStream.format
            val info: DataLine.Info = DataLine.Info(Clip::class.java, format)
            val clip: Clip = AudioSystem.getLine(info) as Clip
            clip.open(audioStream)
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val range = gainControl.maximum - gainControl.minimum
            val value = (range * volume) + gainControl.minimum
            gainControl.value = value
            clip.start()
        }


    } catch (e: Exception) {
        e.printStackTrace()
    }
}