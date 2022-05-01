package components

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import player.isMacOS
import state.AppState
import state.TypingType

/**
 * 侧边菜单
 */
@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class
)
@Composable
fun TypingSidebar(state: AppState) {
    if (state.openSettings && state.global.type == TypingType.WORD) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.fillMaxWidth().height(if(isMacOS()) 78.dp else 48.dp))
            Divider()
            val ctrl = LocalCtrl.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    state.global.type = TypingType.SUBTITLES
                    state.saveGlobalState()
                }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("抄写字幕", color = MaterialTheme.colors.onBackground)
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
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }
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
                    checked = state.typingWord.wordVisible,
                    onCheckedChange = {
                        state.typingWord.wordVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.phoneticVisible,
                    onCheckedChange = {
                        state.typingWord.phoneticVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.morphologyVisible,
                    onCheckedChange = {
                        state.typingWord.morphologyVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.definitionVisible,
                    onCheckedChange = {
                        state.typingWord.definitionVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.translationVisible,
                    onCheckedChange = {
                        state.typingWord.translationVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.subtitlesVisible,
                    onCheckedChange = {
                        state.typingWord.subtitlesVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.speedVisible,
                    onCheckedChange = {
                        state.typingWord.speedVisible = it
                        state.saveTypingWordState()
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
                    checked = state.typingWord.isAuto,
                    onCheckedChange = {
                        state.typingWord.isAuto = it
                        state.saveTypingWordState()
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
                    checked = state.global.isDarkTheme,
                    onCheckedChange = {
                        state.global.isDarkTheme = it
                        state.saveGlobalState()
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
                    checked = state.global.isPlayKeystrokeSound,
                    onCheckedChange = {
                        state.global.isPlayKeystrokeSound = it
                        state.saveGlobalState()
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
                    checked = state.typingWord.isPlaySoundTips,
                    onCheckedChange = {
                        state.typingWord.isPlaySoundTips = it
                        state.saveTypingWordState()
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
                            Column (Modifier.width(300.dp).height(180.dp).padding(start = 16.dp, end = 16.dp)){
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("击键音效")
                                    Slider(value = state.global.keystrokeVolume, onValueChange = {
                                        Thread(Runnable {
                                            state.global.keystrokeVolume = it
                                            state.saveGlobalState()
                                        }).start()
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("提示音效")
                                    Slider(value = state.typingWord.soundTipsVolume, onValueChange = {
                                        Thread(Runnable {
                                            state.typingWord.soundTipsVolume = it
                                        }).start()
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("单词发音")
                                    Slider(value = state.global.audioVolume, onValueChange = {
                                        Thread(Runnable {
                                            state.global.audioVolume = it
                                            state.saveGlobalState()
                                        }).start()
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("视频播放")
                                    Slider(value = state.global.videoVolume, onValueChange = {
                                        Thread(Runnable {
                                            state.global.videoVolume = it
                                            state.saveGlobalState()
                                        }).start()
                                    })
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
                val selectedText = when (state.typingWord.pronunciation) {
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
                                    state.typingWord.pronunciation = "uk"
                                    state.saveTypingWordState()
                                    expanded = false
                                },
                                modifier = Modifier.width(87.dp).height(40.dp)
                            ) {
                                Text("英音")
                            }
                            DropdownMenuItem(
                                onClick = {
                                    state.typingWord.pronunciation = "us"
                                    state.saveTypingWordState()
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
                                    state.typingWord.pronunciation = "jp"
                                    state.saveTypingWordState()
                                    expanded = false
                                },
                                modifier = Modifier.width(87.dp).height(40.dp)
                            ) {
                                Text("日语")
                            }
                        }

                        DropdownMenuItem(
                            onClick = {
                                state.typingWord.pronunciation = "false"
                                state.saveTypingWordState()
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