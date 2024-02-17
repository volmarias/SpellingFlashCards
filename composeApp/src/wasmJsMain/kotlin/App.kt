import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import js_interop.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechFactory
import nl.marc_apps.tts.TextToSpeechInstance
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val greeting = remember { Greeting().greet() }
        val ttsInstance = rememberTextToSpeechOrNull()
        val scope = rememberCoroutineScope()
        val fSpeech: () -> Unit = { scope.launch { ttsInstance?.say("Hello World") }}
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (ttsInstance != null) {
                Button(onClick = fSpeech) {
                    Text("TTS Test")
                }
            }
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource("compose-multiplatform.xml"), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

@Composable
fun rememberTextToSpeechOrNull(): TextToSpeechInstance? {
    var textToSpeech by remember { mutableStateOf<TextToSpeechInstance?>(null) }

    LaunchedEffect(Unit) {
        val tts = TextToSpeechFactory(window).createOrNull()
        delay(500)
        textToSpeech = tts
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.close()
        }
    }

    return textToSpeech
}
