import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import java.io.File

class Game {

    @Serializable
    data class GameData(
        val minutes: Int,
        val seconds: Int,
        val secondsPlaying: Boolean,
        val minutesPlaying: Boolean
    )

    val minutes = mutableStateOf(0)
    val seconds = mutableStateOf(0)
    val secondsPlaying = mutableStateOf(false)
    val minutesPlaying = mutableStateOf(false)

    fun updateMinutes(newMinutes: Int) {
        minutes.value = newMinutes
        seconds.value = seconds.value % 60 + newMinutes * 60
    }

    fun updateSeconds(newSeconds: Int) {
        minutes.value = newSeconds / 60
        seconds.value = newSeconds
    }

    private suspend fun toggleTick(ticked: MutableState<Int>, toggle: MutableState<Boolean>, update: (Int) -> Unit, delayAmount: Long = 50L) {
        while(toggle.value) {
            delay(delayAmount)
            update(ticked.value + 1)
        }
    }

    suspend fun toggleSeconds() {
        secondsPlaying.value = !secondsPlaying.value
        toggleTick(seconds, secondsPlaying, ::updateSeconds)
    }

    suspend fun toggleMinutes() {
        minutesPlaying.value = !minutesPlaying.value
        toggleTick(
            ticked = minutes,
            toggle = minutesPlaying,
            update = ::updateMinutes,
            delayAmount = 150L
        )
    }

    // File I/O

    private fun toData(): GameData {
        return GameData(minutes.value, seconds.value, secondsPlaying.value, minutesPlaying.value)
    }

    private suspend fun fromData(data: GameData) {
        minutes.value = data.minutes
        seconds.value = data.seconds
        // TODO: Weird hack, also it double-plays?? Replace with something reasonable
        secondsPlaying.value = !data.secondsPlaying
        toggleSeconds()
        minutesPlaying.value = !data.minutesPlaying
        toggleMinutes()
    }

    fun saveToFile(scope: CoroutineScope) {
        val fileName = "latest.sav.txt"
        scope.launch {
            val file = File(fileName)
            file.writeText(Json.encodeToString(toData()))
        }
    }

    fun loadFromFile(scope: CoroutineScope) {
        val fileName = "latest.sav.txt"
        scope.launch {
            val file = File(fileName)
            println(file.readText())
            fromData(Json.decodeFromString(file.readText()))
        }
    }

}

@Composable
fun IncDec(
    title: String,
    count: MutableState<Int>,
    playing: MutableState<Boolean>,
    updateCount: (Int) -> Unit,
    togglePlaying: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(10.dp)) {
        Text(title, modifier = Modifier.align(Alignment.CenterHorizontally))
        Divider(
            modifier = Modifier
                .fillMaxWidth(0.1F)
                .padding(5.dp)
                .align(Alignment.CenterHorizontally))
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                updateCount(0)
            }) {
            Text(count.value.toString())
        }
        Row {
            Button(onClick = {
                updateCount(count.value + 1)
            }) {
                Text("++")
            }
            Button(onClick = {
                updateCount(count.value - 1)
            }) {
                Text("--")
            }
            Button(onClick = {
                scope.launch{
                    togglePlaying()
                }
            }) {
                // TODO: replace with vector graphics
                if (playing.value) {
                    Text("❚❚")
                } else {
                    Text("►")
                }
            }
        }
    }
}

fun main() = Window {
    val game = remember { Game() }
        MaterialTheme {
            Row {
                IncDec("Minutes",
                    count = game.minutes,
                    playing = game.minutesPlaying,
                    updateCount = game::updateMinutes,
                    togglePlaying = game::toggleMinutes
                )
                IncDec("Seconds",
                    count = game.seconds,
                    playing = game.secondsPlaying,
                    updateCount = game::updateSeconds,
                    togglePlaying = game::toggleSeconds
                )
                Column {
                    val scope = rememberCoroutineScope()
                    Button(onClick = { game.saveToFile(scope) }) {
                        Text("Save to File")
                    }
                    Button(onClick = { game.loadFromFile(scope) }) {
                        Text("Load from File")
                    }
                }
            }
        }
    }