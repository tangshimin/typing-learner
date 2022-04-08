package components

import LocalCtrl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import state.AppState
import java.math.RoundingMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.fixedRateTimer

@OptIn(ExperimentalFoundationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@Composable
fun Speed(state: AppState, modifier: Modifier) {
    if (state.typing.speedVisible) {

        Box(modifier = modifier) {
            Row (
                Modifier
                    .padding(start = 48.dp)
            ){
                Surface(elevation = 1.dp,
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    modifier = Modifier.padding(top = 20.dp,end = 20.dp)
                ){
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(20.dp)
                    ) {
                        val textColor = MaterialTheme.colors.onBackground

                        StartButton(state)
                        Spacer(Modifier.width(10.dp))
                        ResetButton(state)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.width(IntrinsicSize.Max)) {
                            var minute = state.time.toSecondOfDay().div(60F)
                            if(minute<1) minute = 1F
                            val speed = state.correctCount.div(minute).toInt()
                            Row(Modifier.width(110.dp)){
                                Text("速度:",color = textColor,)
                                Text("${if(speed != 0) speed else ""}",color = textColor,textAlign = TextAlign.Center,modifier = Modifier.width(60.dp))
                            }
                            Divider(Modifier.fillMaxWidth())
                            Row(Modifier.width(110.dp)){
                                Text(text = "时间:",color = textColor,)
                                Text(text = state.time.format( DateTimeFormatter.ofPattern("HH:mm:ss")),color = textColor,textAlign = TextAlign.Center)
                            }
                        }

                    }
                }
            }
        }

    }
}
@OptIn(ExperimentalFoundationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@Composable
fun ResetButton(state: AppState) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ){
                val ctrl = LocalCtrl.current
                val shift = if(isMacOS()) "⇧" else "Shift"
                Text(text = "$ctrl+$shift+Space",modifier = Modifier.padding(10.dp))
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
            reset(state)
        }){
            Text(text = "重置")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@Composable
fun StartButton(state: AppState) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ){
                val ctrl = LocalCtrl.current
                Text(text = "$ctrl+Space",modifier = Modifier.padding(10.dp))
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
            startTimer(state)
        }){
            Text(text = if(state.isStart) "暂停" else "开始")
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
fun startTimer(state: AppState) {
    state.isStart = !state.isStart
    if(state.isStart){
        state.timer = fixedRateTimer("timer", false, 0L, 1 * 1000) {
            state.time = state.time.plusSeconds(1)
        }
    }else{
        state.timer.cancel()
    }
}
@OptIn(ExperimentalSerializationApi::class)
fun reset(state: AppState) {
    state.time = LocalTime.parse("00:00:00", DateTimeFormatter.ofPattern("HH:mm:ss"))
    state.inputCount = 0
    state.correctCount = 0F
    state.wrongCount = 0
}
