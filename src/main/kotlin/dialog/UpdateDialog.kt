package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.GitHubRelease
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun UpdateDialog(
    version: String,
    close: () -> Unit
) {
    Dialog(
        title = "检查更新",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(645.dp, 650.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            var detecting by remember { mutableStateOf(true) }
            var downloadable by remember { mutableStateOf(false) }
            var body by remember { mutableStateOf("") }

            fun detectingUpdates(version: String) {
                val client = OkHttpClient()
                val url = "https://api.github.com/repos/tangshimin/typing-learner/releases/latest"
                val headerName = "Accept"
                val headerValue = "application/vnd.github.v3+json"
                val request = Request.Builder()
                    .url(url)
                    .addHeader(headerName, headerValue)
                    .build()
                try{
                    client.newCall(request).execute().use { response ->
                        detecting = false
                        if (response.code == 200) {
                            if (response.body != null) {
                                val string = response.body!!.string()
                                val releases = Json.decodeFromString<GitHubRelease>(string)
                                body = if (version != releases.tag_name) {
                                    downloadable = true
                                    "有可用更新，版本为：${releases.tag_name}"
                                } else {
                                    downloadable = false
                                    "没有可用更新"
                                }
                            }
                        } else if (response.code == 404) {
                            body = "网页没找到"
                        } else if (response.code == 500) {
                            body = "服务器错误"
                        }
                    }
                }catch (exception: IOException){
                    detecting = false
                    body = exception.toString()

                }

            }

            LaunchedEffect(Unit) {
                detectingUpdates(version)
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("当前版本为：$version")
                }
                if (detecting) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Box(Modifier.width(50.dp).height(50.dp)) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Text("正在检查")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Text("$body")
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    OutlinedButton(onClick = { close() }) {
                        Text("取消")
                    }
                    Spacer(Modifier.width(20.dp))
                    val uriHandler = LocalUriHandler.current
                    val latest = "https://github.com/tangshimin/typing-learner/releases/latest"
                    OutlinedButton(
                        onClick = { uriHandler.openUri(latest)},
                        enabled = downloadable
                    ) {
                        Text("下载最新版")
                    }
                }

            }
        }
    }
}

