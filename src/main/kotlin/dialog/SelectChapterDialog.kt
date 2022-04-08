package dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState

@OptIn(ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalSerializationApi::class
)
@ExperimentalComposeUiApi
@Composable
fun SelectChapterDialog(state: AppState) {
    Dialog(title = "选择章节",
        onCloseRequest = { state.openSelectChapter = false},
        undecorated = !MaterialTheme.colors.isLight,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(930.dp, 785.dp)
        ),
    ) {
        SelectChapter(
            state = state,
            chapter = state.typing.chapter,
            onChapterChanged = {
                state.typing.chapter = it
            },
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun SelectChapter(
    state: AppState,
    chapter: Int,
    onChapterChanged: (Int) -> Unit,
) {
    Surface (
        elevation = 5.dp,
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    ){

        Box(
            modifier = Modifier
                .size(930.dp, 785.dp) .background(color = MaterialTheme.colors.background)
        ) {
            Column (modifier = Modifier.align(Alignment.TopCenter)){
                Row(horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp,bottom = 5.dp)){
                    Text("${state.vocabulary.name}  ${state.vocabulary.size}个单词",color = MaterialTheme.colors.onBackground)
                }
                Divider()
            }
            Row(modifier = Modifier.align(Alignment.Center).padding(top = 31.dp,bottom = 55.dp)){
                Chapters(
                    checkedChapter = chapter,
                    size = state.vocabulary.size,
                    onChapterChanged = {
                        onChapterChanged(it)
                    },
                )
            }
            Footer(
                modifier = Modifier.align(Alignment.BottomCenter).onGloballyPositioned { coordinate ->
                    println(coordinate.size)
                },
                confirm = {
                    if (chapter == 0) state.typing.chapter = 1
                    state.typing.chapter = chapter
                    state.typing.index = (chapter - 1) * 20
                    state.openSelectChapter = false
//                    state.typingVisible = true
                    state.saveTypingState()

                },
                exit = {
                    state.openSelectChapter = false
//                    state.typingVisible = true
                })
        }
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Chapters(size: Int, checkedChapter: Int, onChapterChanged: (Int) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colors.background)
    ) {
        var count = size / 20
        val mod = size % 20
        if (mod != 0 && size > 20) count += 1
        if (size < 20) count = 1
        val chapters = (1 until count + 1).map { "Chapter $it" }.toList()
        val listState = rememberLazyListState()
        LazyVerticalGrid(
            cells = GridCells.Adaptive(144.dp),
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 60.dp),
            state = listState
        ) {
            itemsIndexed(chapters) { index: Int, item: String ->
                val chapter = index + 1
                var checkedState = chapter == checkedChapter
                Card(
                    modifier = Modifier
                        .padding(7.5.dp)
                        .clickable {
                            onChapterChanged(if (checkedState) 0 else chapter)
                            println(if (checkedState) 0 else chapter)
                        },
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 3.dp
                ) {
                    Box(Modifier.size(width = 144.dp, height = 60.dp)) {
                        Text(
                            text = item,
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                        )
                        Text(
                            text = "${if (index == count - 1) mod else 20} 词",
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                        )
                        if (checkedState) {
                            Checkbox(
                                checked = checkedState,
                                onCheckedChange = { checkedState = it },
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }
                }
            }

        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = listState
            )
        )

    }
}

@Composable
fun Footer(modifier: Modifier, confirm: () -> Unit, exit: () -> Unit) {
    Box(modifier = modifier) {
        Column {
            Divider(Modifier.fillMaxWidth())
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .background(color = MaterialTheme.colors.background)
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(top = 10.dp, bottom = 10.dp)
            ) {
                OutlinedButton(onClick = { confirm() }) {
                    Text(text = "确认", color = MaterialTheme.colors.onBackground)
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = { exit() }) {
                    Text(text = "取消", color = MaterialTheme.colors.onBackground)
                }
                Spacer(Modifier.width(10.dp))
            }
        }
    }


}