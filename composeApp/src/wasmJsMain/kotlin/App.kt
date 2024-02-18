@file:OptIn(ExperimentalVoiceApi::class)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import js_interop.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechFactory
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.Voice
import nl.marc_apps.tts.experimental.ExperimentalVoiceApi

@Composable
fun App() {
    MaterialTheme {
        val ttsScope = rememberCoroutineScope()
        val ttsInstance by rememberTextToSpeechOrNull()
        var isSpeaking by remember { mutableStateOf(false) }
        var isWorking by remember { mutableStateOf(false) }
        val shouldWait by remember { derivedStateOf { isSpeaking || isWorking } }

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
        val guessWord = remember { mutableStateOf("") }
        var lastWord by remember { mutableStateOf("") }

        val attemptGuess = {
            val guess = guessWord.value.trim()
            if (guess.isNotEmpty()) {
                ttsScope.launch {
                    lastWord = wordEngine.currentWord.value ?: ""
                    val guessResult = wordEngine.guess(guess)
                    if (guessResult) {
                        "correct"
                    } else {
                        "incorrect"
                    }.apply {
                        isWorking = true
                        ttsInstance?.say(this, clearQueue = true, callback = {
                            ttsScope.launch {
                                guessWord.value = ""
                                delay(500)
                                (wordEngine.currentWord.value ?: "No more words").apply { ttsInstance?.say(this) }
                                isWorking = false
                                delay(100)
                                focusRequester.requestFocus()
                            }
                        })
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
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
                    GuessField(guessWord, shouldWait, focusRequester, attemptGuess)
                    Button(enabled = !shouldWait, onClick = attemptGuess) {
                        Text("Guess")
                    }

                    Button(enabled = !shouldWait, onClick = {
                        ttsScope.launch { ttsInstance?.say(wordEngine.currentWord.value ?: "") }
                    }) {
                        Text("Repeat the word")
                    }

                    ttsInstance?.let {
                        VoiceSelectionDropDown(it, enabled = !shouldWait, selected = {
                            ttsScope.launch {
                                ttsInstance?.say("Your word is ${wordEngine.currentWord.value}")
                            }
                        })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (lastWord.trim().isNotEmpty()) {
                        Text("Last word: $lastWord")
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        with(wordEngine) {
                            if (incorrectWordList.isNotEmpty()) {
                                WordColumn(incorrectWordList, "Incorrect Words")
                            }
                            if (incorrectWordList.isNotEmpty() && correctWordList.isNotEmpty()) {
                                Spacer(modifier = Modifier.size(50.dp))
                            }
                            if (correctWordList.isNotEmpty()) {
                                WordColumn(correctWordList, "Correct Words")
                            }
                        }
                    }
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
}

@Composable
private fun WordColumn(words: List<String>, title: String) {
    Box(modifier = Modifier.wrapContentSize().drawWithCache {
        val brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White))
        onDrawWithContent {
            drawContent()
            drawRect(brush)
        }
    }) {

        LazyColumn(modifier = Modifier.wrapContentHeight()) {
            item { Text(title) }
            items(words.reversed().take(10)) {
                Text(it)
            }
            if (words.size < 10) {
                items(10 - words.size) {
                    Text(" ")
                }
            }
        }
    }
}

@Composable
private fun GuessField(
    guessWord: MutableState<String>,
    shouldWait: Boolean,
    focusRequester: FocusRequester,
    attemptGuess: () -> Unit
) {
    TextField(
        guessWord.value,
        onValueChange = {
            guessWord.value = it
        },
        enabled = !shouldWait,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().wrapContentHeight().focusRequester(focusRequester),
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
}

@Composable
fun VoiceSelectionDropDown(ttsInstance: TextToSpeechInstance, enabled: Boolean = true, selected: (Voice) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    Button(onClick = { expanded = true }, enabled = enabled) {
        Text("Current voice:\n${ttsInstance.currentVoice?.name ?: "No voice selected!"}")
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }) {
        ttsInstance.voices.forEach {
            DropdownMenuItem(onClick = {
                ttsInstance.currentVoice = it
                expanded = false
                selected(it)
            }) {
                with(it) {
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
