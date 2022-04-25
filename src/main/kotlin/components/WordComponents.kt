package components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.*
import dialog.ChapterFinishedDialog
import dialog.EditWordDialog
import kotlinx.coroutines.launch
import player.AudioButton
import state.AppState
import state.getResourcesFile
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import kotlin.concurrent.fixedRateTimer

/** 单词组件
 * @param state 应用程序的状态
 * @param word 单词
 * @param correctTime 单词的正确数
 * @param wrongTime 单词的错误数
 * @param toNext 当切换到下一个单词时被调用的回调
 * @param dictationSkip 当用户在默写模式按 Enter 键被调用的回调
 * @param textFieldValue 用户输入的字符串
 * @param typingResult 用户输入字符的结果
 * @param checkTyping 检查用户的输入是否正确的回调
 * @param showChapterFinishedDialog 是否显示当前单元已经完成对话框
 * @param setShowEditWordDialog 设置是否显示当前单元已经完成对话框
 * @param isVocabularyFinished 是否整个词库都已经学习完
 * @param setIsVocabularyFinished 设置是否整个词库都已经学习完
 * @param chapterCorrectTime 整个章节的正确数，在默写模式时使用
 * @param chapterWrongTime 整个章节的错误数，在默写模式时使用
 * @param dictationWrongWords 默写模式的错误单词
 * @param resetChapterTime 默写结束时被调用的回调，用于清理  [chapterWrongTime] 和 [resetChapterTime] 和 [dictationWrongWords]
 * @param playKeySound 播放敲击键盘的音效
 */
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
    dictationSkip: () -> Unit,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (String) -> Unit,
    showChapterFinishedDialog: Boolean,
    changeShowChapterFinishedDialog: (Boolean) -> Unit,
    showEditWordDialog: Boolean,
    setShowEditWordDialog: (Boolean) -> Unit,
    isVocabularyFinished: Boolean,
    setIsVocabularyFinished: (Boolean) -> Unit,
    chapterCorrectTime: Float,
    chapterWrongTime: Float,
    dictationWrongWords: Map<Word, Int>,
    resetChapterTime: () -> Unit,
    playKeySound: () -> Unit,

    ) {

    /**
     * 协程构建器
     */
    val scope = rememberCoroutineScope()

    val wordValue = word.value
    val focusRequester = remember { FocusRequester() }
    var lastInputTime by remember { mutableStateOf(0L) }
    var activeMenu by remember { mutableStateOf(false) }
    val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
        when {
            ((it.key == Key.Enter || it.key == Key.NumPadEnter)
                    && it.type == KeyEventType.KeyUp) -> {
                toNext()
                if (state.isDictation) {
                    dictationSkip()
                }
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
                if (!state.speed.isStart) {
                    state.speed.isStart = true
                    state.speed.timer = fixedRateTimer("timer", false, 0L, 1 * 1000) {
                        state.speed.time = state.speed.time.plusSeconds(1)
                    }
                    // 超过一分钟没有输入自动暂停
                    state.speed.autoPauseTimer =
                        fixedRateTimer("autoPause", false, 0L, 60 * 1000) {
                            val span = System.currentTimeMillis() - lastInputTime
                            if (span > 60 * 1000) {
                                state.speed.isStart = false
                                state.speed.timer.cancel()
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
                .onPointerEvent(PointerEventType.Enter) {
                    if(!state.isDictation || (state.isDictation && state.isReviewWrongList)){
                        activeMenu = true
                    } }) {
                CompositionLocalProvider(
                    LocalTextInputService provides null
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            scope.launch {
                                checkTyping(input)
                            }
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
                }
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
                volume = state.typing.audioVolume,
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
                            scope.launch {
                                val index = state.typing.index
                                state.vocabulary.wordList.removeAt(index)
                                state.vocabulary.size = state.vocabulary.wordList.size
                                state.saveCurrentVocabulary()
                                showConfirmationDialog = false
                            }
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
                        setShowEditWordDialog(true)
                    }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    if (showEditWordDialog) {
                        EditWordDialog(
                            word = word,
                            state = state,
                            save = { newWord ->
                                scope.launch{
                                    val current = state.getCurrentWord()
                                    val index = state.typing.index
                                    newWord.captions = current.captions
                                    newWord.externalCaptions = current.externalCaptions
                                    state.vocabulary.wordList.removeAt(index)
                                    state.vocabulary.wordList.add(index, newWord)
                                    state.saveCurrentVocabulary()
                                    setShowEditWordDialog(false)
                                }

                            },
                            close = { setShowEditWordDialog(false) }
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


    /**
     * 索引递减
     */
    val decreaseIndex = {
        if (state.vocabulary.size > 19) state.typing.index -= 19
        else state.typing.index = 0
    }

    /**
     * 计算正确率
     */
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

    /**
     * 重复学习本章
     */
    val learnAgain: () -> Unit = {
        decreaseIndex()
        resetChapterTime()
        changeShowChapterFinishedDialog(false)
    }

    /**
     * 复习错误单词
     */
    val reviewWrongWords: () -> Unit = {
        val reviewList = dictationWrongWords.keys.toList()
        if (reviewList.isNotEmpty()) {
            state.enterReviewMode(reviewList)
            resetChapterTime()
            changeShowChapterFinishedDialog(false)
        }
    }

    /**
     * 下一章
     */
    val nextChapter: () -> Unit = {
        if (state.isDictation) state.exitDictationMode()
        state.typing.index += 1
        state.typing.chapter++
        resetChapterTime()
        state.saveTypingState()
        changeShowChapterFinishedDialog(false)
    }

    /**
     * 进入默写模式
     */
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

    /**
     * 重置索引
     * @param isShuffle 是否打乱词库
     */
    val resetIndex: (isShuffle: Boolean) -> Unit = { isShuffle ->

        state.typing.index = 0
        state.typing.chapter = 1
        if (isShuffle) {
            state.vocabulary.wordList.shuffle()
            state.saveCurrentVocabulary()
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

/**
 * 音标组件
 */
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



/**
 * 确认删除对话框
 * @param message 要显示的消息
 * @param confirm 点击确认之后调用的回调
 * @param close 点击取消之后调用的回调
 */
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

/**
 * 播放音效
 * @param path 路径
 * @param volume 音量
 */
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