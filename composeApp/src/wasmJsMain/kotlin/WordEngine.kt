import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
class WordEngine {

    private val _allWords = mutableListOf<String>()
    val allWords: List<String> = _allWords

    private val pendingWordList = mutableListOf<String>()

    private val incorrectWordList = mutableListOf<String>()
    private val correctWordList = mutableListOf<String>()

    private var _currentWord: MutableState<String?> = mutableStateOf("")
    val currentWord: State<String?> = _currentWord

    suspend fun prepare(wordList: String = "wordlist.txt") {
        val words = resource(wordList).readBytes().decodeToString().split("\r\n", "\n").map { it.trim() }
        with(_allWords) {
            clear()
            addAll(words)
        }
        with(pendingWordList) {
            addAll(words.shuffled().take(10))
            println("pending word list: ")
            forEach { println(it) }
        }
        _currentWord.value = pendingWordList.removeFirst()
        println("current word: ${_currentWord.value}")
    }

    fun guess(guess: String): Boolean {
        val word = currentWord.value?.trim() ?: return false
        println("current word: $word, guess is $guess")
        val correct = (guess == word)
        println("guess was correct: $correct")
        if (!correct) {
            println(guess.encodeToByteArray().joinToString())
            println(word.encodeToByteArray().joinToString())
        }
        if (correct) { correctWordList } else { incorrectWordList }.add(word)
        _currentWord.value = pendingWordList.removeFirstOrNull()
        return correct
    }
}