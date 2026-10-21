package ar.com.copiloto.assistant

data class ConversationTurn(val role: String, val text: String)

data class GuidanceTarget(
    val visible: Boolean, val label: String?, val x: Double?, val y: Double?,
    val width: Double?, val height: Double?, val confidence: Double,
) {
    fun isUsable(): Boolean = visible && confidence in .65..1.0 &&
        listOf(x, y, width, height).all { it != null && it.isFinite() && it in 0.0..1.0 } &&
        width!! > 0 && height!! > 0
}

enum class GuidanceRisk { LOW, MEDIUM, HIGH }

data class GuidanceResponse(
    val canHelp: Boolean, val heardQuestion: String, val message: String,
    val target: GuidanceTarget, val risk: GuidanceRisk, val requiresConfirmation: Boolean,
    val audioPcm: ByteArray?, val model: String,
) {
    val canHighlight: Boolean get() = canHelp && risk != GuidanceRisk.HIGH && !requiresConfirmation && target.isUsable()
}

class ConversationContext {
    private val turns = ArrayDeque<ConversationTurn>()
    fun snapshot(): List<ConversationTurn> = turns.toList()
    fun add(question: String, answer: String) {
        turns.addLast(ConversationTurn("user", question.take(800)))
        turns.addLast(ConversationTurn("assistant", answer.take(800)))
        while (turns.size > 6) turns.removeFirst()
    }
    fun clear() = turns.clear()
}
