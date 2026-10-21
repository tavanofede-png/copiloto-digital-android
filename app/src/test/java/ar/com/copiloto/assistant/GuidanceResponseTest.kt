package ar.com.copiloto.assistant

import org.junit.Assert.*
import org.junit.Test

class GuidanceResponseTest {
    private fun response(risk: GuidanceRisk = GuidanceRisk.LOW, confirmation: Boolean = false,
        x: Double? = .5, confidence: Double = .9) = GuidanceResponse(true, "Mi factura", "Tocá Facturas.",
        GuidanceTarget(true, "Facturas", x, .4, .6, .1, confidence), risk, confirmation, null, "test")

    @Test fun sensitiveActionsNeverHighlightEvenIfBackendSendsTarget() {
        assertFalse(response(GuidanceRisk.HIGH).canHighlight)
        assertFalse(response(confirmation = true).canHighlight)
        assertTrue(response().canHighlight)
    }

    @Test fun untrustedCoordinatesCannotReachOverlay() {
        for (x in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -.1, 1.01)) assertFalse(response(x = x).canHighlight)
        assertFalse(response(confidence = .64).canHighlight)
        assertTrue(response(confidence = .65).canHighlight)
    }

    @Test fun contextIsBoundedAndClearedAtSessionEnd() {
        val context = ConversationContext()
        repeat(10) { context.add("Pregunta $it", "Respuesta $it") }
        assertEquals(6, context.snapshot().size)
        assertEquals("Pregunta 7", context.snapshot().first().text)
        context.clear()
        assertTrue(context.snapshot().isEmpty())
    }
}
