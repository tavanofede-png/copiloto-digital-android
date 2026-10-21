package ar.com.copiloto.voice

/** Only the active in-process service receives audio; no exported broadcasts or files. */
object VoiceInputBus {
    private var listener: ((Result<ByteArray>) -> Unit)? = null
    private var requestId = 0L
    private var cancelRecording: (() -> Unit)? = null
    fun await(callback: (Result<ByteArray>) -> Unit): Long { clear(); listener = callback; return requestId }
    fun attach(id: Long, cancel: () -> Unit) { if (id == requestId) cancelRecording = cancel else cancel() }
    fun detach(id: Long) { if (id == requestId) cancelRecording = null }
    fun complete(id: Long, result: Result<ByteArray>) {
        if (id != requestId) return
        val current = listener; listener = null; cancelRecording = null; current?.invoke(result)
    }
    fun clear() {
        requestId++
        listener = null
        val cancel = cancelRecording; cancelRecording = null; cancel?.invoke()
    }
}
