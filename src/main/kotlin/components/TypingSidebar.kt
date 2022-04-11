package components

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import state.AppState

/**
 * 侧边菜单
 */
@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class
)
@Composable
fun TypingSidebar(state: AppState) {
    if (state.openSettings) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.fillMaxWidth().height(48.dp))
            Divider()
            val ctrl = LocalCtrl.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("显示单词", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+V",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.wordVisible,
                    onCheckedChange = {
                        state.typing.wordVisible = it
                        state.saveTypingState()
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text(text = "显示音标", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+P",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.phoneticVisible,
                    onCheckedChange = {
                        state.typing.phoneticVisible = it
                        state.saveTypingState()
                    },

                    )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("显示词形", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+L",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.morphologyVisible,
                    onCheckedChange = {
                        state.typing.morphologyVisible = it
                        state.saveTypingState()
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("英文释义", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+F",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.definitionVisible,
                    onCheckedChange = {
                        state.typing.definitionVisible = it
                        state.saveTypingState()
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("中文释义", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+K",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.translationVisible,
                    onCheckedChange = {
                        state.typing.translationVisible = it
                        state.saveTypingState()
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("显示字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+S",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.subtitlesVisible,
                    onCheckedChange = {
                        state.typing.subtitlesVisible = it
                        state.saveTypingState()
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("显示速度", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+N",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.speedVisible,
                    onCheckedChange = {
                        state.typing.speedVisible = it
                        state.saveTypingState()
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text(text = "自动切换", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+A",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.isAuto,
                    onCheckedChange = {
                        state.typing.isAuto = it
                        state.saveTypingState()
                    },

                    )
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
                    checked = state.typing.isDarkTheme,
                    onCheckedChange = {
                        state.typing.isDarkTheme = it
                        state.saveTypingState()
                    },
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
                    checked = state.typing.isPlayKeystrokeSound,
                    onCheckedChange = {
                        state.typing.isPlayKeystrokeSound = it
                        state.saveTypingState()
                    },

                    )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("提示音效", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+W",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = state.typing.isPlaySoundTips,
                    onCheckedChange = {
                        state.typing.isPlaySoundTips = it
                        state.saveTypingState()
                    },

                    )
            }

            Box(Modifier.fillMaxWidth()) {

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("音量控制", color = MaterialTheme.colors.onBackground)
                    }
                    Spacer(Modifier.width(15.dp))
                    var expanded by remember { mutableStateOf(false) }
                    CursorDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {expanded = false},
                    ){
                        Surface(
                            elevation = 4.dp,
                            shape = RectangleShape,
                        ) {
                            Column (Modifier.width(300.dp).height(140.dp).padding(start = 16.dp, end = 16.dp)){
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("击键音效")
                                    Slider(value = state.typing.keystrokeVolume, onValueChange = {state.typing.keystrokeVolume = it})
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("提示音效")
                                    Slider(value = state.typing.soundTipsVolume, onValueChange = {state.typing.soundTipsVolume = it})
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("音频视频")
                                    Slider(value = state.typing.audioVolume, onValueChange = {state.typing.audioVolume = it})
                                }

                            }
                        }
                    }
                    IconButton(onClick = { expanded = true },) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp)
            ) {
                Text("自动发音", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(35.dp))
                var expanded by remember { mutableStateOf(false) }
                val selectedText = when (state.typing.pronunciation) {
                    "us" -> "美音"
                    "uk" -> "英音"
                    "jp" -> "日语"
                    else -> "关闭"
                }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .width(87.dp)
                            .background(Color.Transparent)
                            .border(1.dp, Color.Transparent)
                    ) {
                        Text(text = selectedText)
                        Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(87.dp)
                            .height(140.dp)
                    ) {
                        if (state.vocabulary.language == "english") {
                            DropdownMenuItem(
                                onClick = {
                                    state.typing.pronunciation = "uk"
                                    state.saveTypingState()
                                    expanded = false
                                },
                                modifier = Modifier.width(87.dp).height(40.dp)
                            ) {
                                Text("英音")
                            }
                            DropdownMenuItem(
                                onClick = {
                                    state.typing.pronunciation = "us"
                                    state.saveTypingState()
                                    expanded = false
                                },
                                modifier = Modifier.width(87.dp).height(40.dp)
                            ) {
                                Text("美音")
                            }
                        }

                        if (state.vocabulary.language == "japanese") {
                            DropdownMenuItem(
                                onClick = {
                                    state.typing.pronunciation = "jp"
                                    state.saveTypingState()
                                    expanded = false
                                },
                                modifier = Modifier.width(87.dp).height(40.dp)
                            ) {
                                Text("日语")
                            }
                        }

                        DropdownMenuItem(
                            onClick = {
                                state.typing.pronunciation = "false"
                                state.saveTypingState()
                                expanded = false
                            },
                            modifier = Modifier.width(87.dp).height(40.dp)
                        ) {
                            Text("关闭")
                        }
                    }

                }
            }

        }


    }
}