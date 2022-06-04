package dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import components.toAwt
import components.toCompose
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState
import theme.createColors
import java.awt.Dimension
import javax.swing.JColorChooser

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun ColorChooserDialog(
    close: () -> Unit,
    state:AppState
    ) {
    Dialog(
        title = "选择主题色",
        icon = painterResource("logo/logo.svg"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(900.dp, 600.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            var selectedColor by remember { mutableStateOf(state.global.primaryColor) }
            val initialColor = state.global.primaryColor.toAwt()
            val colorChooser = JColorChooser(initialColor)
            val colorModel = colorChooser.selectionModel

            colorModel.addChangeListener {
                selectedColor = colorModel.selectedColor.toCompose()
            }
            val previewPanel = ComposePanel()
            previewPanel.setContent {
                MaterialTheme(colors = state.colors) {
                    val fontFamily by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }
                    Column(verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray)){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().height(120.dp)){
                            Box(Modifier.width(40.dp).height(40.dp).background(selectedColor))
                            Spacer(Modifier.width(25.dp))
                            Row(horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(300.dp).fillMaxHeight().background(darkColors().background)){
                                Text(
                                    fontSize = 2.em,
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(color = selectedColor, fontFamily = fontFamily)
                                        ) {
                                            append("typing-l")
                                        }

                                        withStyle(
                                            style = SpanStyle(color = Color.Red,fontFamily = fontFamily)
                                        ) {
                                            append("e")
                                        }
                                        withStyle(
                                            style = SpanStyle(color = darkColors().onBackground,fontFamily = fontFamily)
                                        ) {
                                            append("arner")
                                        }
                                    }
                                )
                                Spacer(Modifier.width(5.dp))
                                Column{
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "3", color = selectedColor)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "1", color = Color.Red)
                                }
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "Localized description",
                                    tint = selectedColor,
                                    modifier = Modifier.padding(top = 8.dp),
                                )

                            }
                            Spacer(Modifier.width(15.dp))
                            Row(horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(300.dp).fillMaxHeight().background(lightColors().background)){
                                Text(
                                    fontSize = 2.em,
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(color = selectedColor,fontFamily = fontFamily)
                                        ) {
                                            append("typing-l")
                                        }

                                        withStyle(
                                            style = SpanStyle(color = Color.Red,fontFamily = fontFamily)
                                        ) {
                                            append("e")
                                        }
                                        withStyle(
                                            style = SpanStyle(color = lightColors().onBackground,fontFamily = fontFamily)
                                        ) {
                                            append("arner")
                                        }
                                    }
                                )
                                Spacer(Modifier.width(5.dp))
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "3", color = selectedColor)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "1", color = Color.Red)
                                }
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "Localized description",
                                    tint = selectedColor,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    state.global.primaryColor = selectedColor
                                    state.colors = createColors(state.global.isDarkTheme, state.global.primaryColor)
                                    state.saveGlobalState()
                                    close()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = selectedColor)
                            ) {
                                Text("确定")
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    close()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = selectedColor)
                            ) {
                                Text("取消")
                            }
                        }
                    }
                }
            }
            previewPanel.size = Dimension(1200,220)
            colorChooser.previewPanel = previewPanel
            SwingPanel(
                modifier = Modifier.fillMaxSize(),
                factory = {colorChooser}
            )
        }
    }
}