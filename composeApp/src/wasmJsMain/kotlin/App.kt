@file:OptIn(ExperimentalVoiceApi::class)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import js_interop.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechFactory
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.experimental.ExperimentalVoiceApi
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme {
        val ttsScope = rememberCoroutineScope()
        val ttsInstance by rememberTextToSpeechOrNull()
        var isSpeaking by remember { mutableStateOf(false) }
        var isWorking by remember { mutableStateOf(false) }
        val shouldWait by remember { derivedStateOf { isSpeaking || isWorking }}

        ttsInstance?.let { instance ->
            ttsScope.launch {
                println("ttsInstance ready, collecting synthesizing")
                instance.isSynthesizing.collect {
                    isSpeaking = it
                    if (it) {
                        println("speaking")
                    } else {
                        println("done speaking")
                    }
                }
            }
        }

        val focusRequester = remember { FocusRequester() }

        val wordEngine = remember { WordEngine() }
        var wordEngineLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            wordEngine.prepare()
            wordEngineLoaded = true
        }
        var guessWord by remember { mutableStateOf("") }

        val attemptGuess = {
            if (guessWord.trim().isNotEmpty()) {
                ttsScope.launch {
                    val guessResult = wordEngine.guess(guessWord)
                    if (guessResult) {
                        "correct"
                    } else {
                        "incorrect"
                    }.apply {
                        isWorking = true
                        ttsInstance?.say(this, clearQueue = true, callback = {
                            ttsScope.launch {
                                delay(500)
                                (wordEngine.currentWord.value ?: "No more words").apply { ttsInstance?.say(this) }
                                isWorking = false
                                focusRequester.requestFocus()
                            }
                        })
                    }
                }
            }
        }

        if (wordEngineLoaded && ttsInstance != null) {
            LaunchedEffect(ttsInstance) {
                launch {
                    val word = wordEngine.currentWord.value
                    println("Going to say $word")
                    delay(2000)
                    println("delayed 2000ms, speaking")
                    ttsInstance?.say(word?.let { "your word is $it" } ?: "Word not ready yet")

                }
            }
            Column {
//                Text("Current word: ${wordEngine.currentWord.value}")
                TextField(
                    guessWord,
                    onValueChange = {
                        guessWord = it
                    },
                    enabled = !shouldWait,
                    singleLine = true,
                    modifier = Modifier.width(200.dp).wrapContentHeight().focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions {
                        attemptGuess()
                    },
                    textStyle = MaterialTheme.typography.h2
                )
                Button(enabled = !shouldWait, onClick = attemptGuess) {
                    Text("Guess")
                }

                Button(enabled = !shouldWait, onClick = {
                    ttsScope.launch { ttsInstance?.say(wordEngine.currentWord.value ?: "") }
                }) {
                    Text("Repeat the word")
                }

                ttsInstance?.let { VoiceSelectionDropDown(it) }
            }

        } else {
            Row {
                Text("TTS Loaded: ${ttsInstance != null}, warming up: ${ttsInstance?.isWarmingUp?.value ?: ""}")
                Spacer(modifier = Modifier.size(32.dp))
                Text("Word engine loaded: $wordEngineLoaded")
            }
        }
    }
}

@Composable
fun VoiceSelectionDropDown(ttsInstance: TextToSpeechInstance) {
    var expanded by remember { mutableStateOf(false) }
    Button(onClick = {expanded = true}) {
        Text("Current voice: ${ttsInstance.currentVoice?.name ?: "No voice selected!"}")
    }
    val scope = rememberCoroutineScope()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false}) {
        ttsInstance.voices.forEach {
            DropdownMenuItem(onClick = {
                ttsInstance.currentVoice = it
                expanded = false
                scope.launch {
                    ttsInstance.say("Now using ${it.name}")
                }
            }) {
                with (it) {
                    Text("$name ($languageTag)")
                }
            }
        }
    }
}

@Composable
fun rememberTextToSpeechOrNull(): MutableState<TextToSpeechInstance?> {
    val textToSpeech = remember { mutableStateOf<TextToSpeechInstance?>(null) }

    LaunchedEffect(Unit) {
        textToSpeech.value = TextToSpeechFactory(window).createOrNull()
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech.value?.close()
        }
    }

    return textToSpeech
}
