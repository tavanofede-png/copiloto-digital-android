package ar.com.copiloto.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SessionState(
    val starting: Boolean = false,
    val active: Boolean = false,
    val capturing: Boolean = false,
    val captureSummary: String? = null,
    val error: String? = null,
    val status: String? = null,
    val lastMessage: String? = null,
)

/** Process-local state only: no images, questions or consent tokens are persisted. */
object AssistanceSessionManager {
    private val mutableState = MutableStateFlow(SessionState())
    val state = mutableState.asStateFlow()

    fun starting() { mutableState.value = SessionState(starting = true) }
    fun started() { mutableState.value = SessionState(active = true) }
    fun capturing() { mutableState.update { it.copy(capturing = true, error = null) } }
    fun captured(summary: String) {
        mutableState.update { it.copy(capturing = false, captureSummary = summary, error = null) }
    }
    fun error(message: String) {
        mutableState.update { it.copy(starting = false, capturing = false, status = null, error = message) }
    }
    fun stopped(message: String? = null) { mutableState.value = SessionState(error = message) }
    fun processing(message: String) { mutableState.update { it.copy(capturing = true, status = message, error = null) } }
    fun answered(message: String) { mutableState.update { it.copy(capturing = false, status = null, lastMessage = message, error = null) } }
    fun idle() { mutableState.update { it.copy(capturing = false, status = null) } }
}
