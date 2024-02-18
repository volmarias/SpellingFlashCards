import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
class WordEngine {

    private val _allWords = mutableListOf<String>()
//    val allWords: List<String> = _allWords

    private val pendingWordList = mutableListOf<String>()

    private val _incorrectWordList = mutableStateListOf<String>()
    val incorrectWordList: SnapshotStateList<String> = _incorrectWordList
    private val _correctWordList = mutableStateListOf<String>()
    val correctWordList: SnapshotStateList<String> = _correctWordList

    private var _currentWord: MutableState<String?> = mutableStateOf("")
    val currentWord: State<String?> = _currentWord

    suspend fun prepare(wordList: String = "wordlist.txt") {
        val words = resource(wordList).readBytes().decodeToString().split("\r\n", "\n").map { it.trim().lowercase() }
        with(_allWords) {
            clear()
            addAll(words)
        }
        with(pendingWordList) {
            clear()
            addAll(words.shuffled())
        }
        correctWordList.clear()
        incorrectWordList.clear()
        _currentWord.value = pendingWordList.removeFirst()
    }

    fun guess(guess: String): Boolean {
        val word = currentWord.value ?: return false
        println("current word: $word, guess is $guess")
        val correct = (guess.trim().lowercase() == word)
        if (correct) {
            _correctWordList
        } else {
            _incorrectWordList
        }.add(word)
        _currentWord.value = pendingWordList.removeFirstOrNull()
        return correct
    }
}