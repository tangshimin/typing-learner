package dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.Word
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
@Composable
fun ChapterFinishedDialog(
    close: () -> Unit,
    correctRate: Float,
    isVocabularyFinished: Boolean,
    isDictation: Boolean,
    isReviewWrongList: Boolean,
    dictationWrongWords: Map<Word, Int>,
    enterDictation: () -> Unit,
    learnAgain: () -> Unit,
    reviewWrongWords: () -> Unit,
    nextChapter: () -> Unit,
    resetIndex: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val width = if(isVocabularyFinished) 550.dp else 400.dp
    val height = (180 + ((dictationWrongWords.size * 18) + 10)).dp
    Dialog(
        title = "提示",
        onCloseRequest = { close() },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(width,height )
        ),
    ) {

        WindowDraggableArea {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .focusable(true)
                    .fillMaxSize()
                    .focusable()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        when {
                            (it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                                // 进入默写模式
                                enterDictation()
                                true
                            }
                            (it.key == Key.N && it.type == KeyEventType.KeyUp) -> {
                                // 进入复习错误单词模式
                                reviewWrongWords()
                                true
                            }
                            (it.key == Key.Enter && it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                learnAgain()
                                true
                            }
                            ((it.key == Key.Enter || it.key == Key.NumPadEnter)
                                    && it.type == KeyEventType.KeyUp) -> {
                                if (isVocabularyFinished) resetIndex(false)
                                else nextChapter()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                val textColor = MaterialTheme.colors.primary
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    val text = if (isVocabularyFinished) {
                        "您已完成最后一个章节"
                    } else if (isDictation){
                        if(isReviewWrongList){
                            "您已复习完本章节"
                        }else "您已默写完本章节"

                    } else "您已学习完本章节"
                    Text(text = "$text", color = MaterialTheme.colors.onBackground)
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDictation && !isReviewWrongList){
                        Text(text = "正确率 ",color = MaterialTheme.colors.onBackground)
                        Text(text = "$correctRate%",color = MaterialTheme.colors.primary)
                    }
                }

                if (isDictation && !isReviewWrongList) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Column(Modifier.width(IntrinsicSize.Max)) {
                            if(correctRate < 100F){
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(141.dp)
                                ) {
                                    Text(text = "单词", color = MaterialTheme.colors.onBackground)
                                    Text(text = "错误数", color = MaterialTheme.colors.onBackground,modifier = Modifier.width(50.dp))
                                }
                            }

                            if(dictationWrongWords.isNotEmpty()) Divider(Modifier.fillMaxWidth())
                            val treeMap = TreeMap<Int,Word>()
                            dictationWrongWords.forEach{it ->
                                treeMap.put(it.value,it.key)
                            }

                            treeMap.forEach {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = it.value.value, color = MaterialTheme.colors.onBackground)
                                    Spacer(Modifier.width(30.dp))
                                    Text(text = "${it.key}",textAlign = TextAlign.Center, color = Color.Red,modifier = Modifier.width(50.dp))
                                }
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = "快捷键 V", modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        OutlinedButton(
                            onClick = {

                                enterDictation()
                            }
                        ) {
                            val text = if (isDictation) "再默写一次" else "默写本章"
                            Text(text = text, color = textColor)
                        }
                    }

                    Spacer(Modifier.width(20.dp))
                    if (isDictation && !isReviewWrongList) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "快捷键 N", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset.Zero
                            )
                        ){
                            OutlinedButton(onClick = {
                                reviewWrongWords()
                            }) { Text("复习错误单词", color = textColor) }
                        }
                    }
                    if (!isDictation) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "快捷键 Shift+Enter", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset.Zero
                            )
                        ) {
                            OutlinedButton(onClick = {
                                learnAgain()
                            }) { Text("重复本章", color = textColor) }
                        }
                    }


                    Spacer(Modifier.width(20.dp))

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = "快捷键 Enter", modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        if (isVocabularyFinished) {
                            OutlinedButton(onClick = {
                                resetIndex(false)
                            }) {
                                Text("返回到第一章", color = textColor)
                            }
                        } else {
                            OutlinedButton(onClick = {
                                nextChapter()
                            }) {
                                Text("下一章", color = textColor)
                            }
                        }

                    }
                    Spacer(Modifier.width(20.dp))
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = "快捷键 Enter", modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        if (isVocabularyFinished) {
                            OutlinedButton(onClick = {
                                resetIndex(true)
                            }) {
                                Text("随机重置词库", color = textColor)
                            }
                        }

                    }

                }

            }
        }


    }
}