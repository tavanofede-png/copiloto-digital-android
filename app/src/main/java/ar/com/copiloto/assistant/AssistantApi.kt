package ar.com.copiloto.assistant

import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import ar.com.copiloto.capture.ScreenCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

interface AssistantApi {
    suspend fun getGuidance(screen: ScreenCapture?, screenWidth: Int, screenHeight: Int,
        question: String?, audio: ByteArray?, context: List<ConversationTurn>): GuidanceResponse
    fun cancel()
}

class HttpAssistantApi(private val settings: ConnectionSettings) : AssistantApi {
    private val active = AtomicReference<HttpURLConnection?>()

    override suspend fun getGuidance(screen: ScreenCapture?, screenWidth: Int, screenHeight: Int,
        question: String?, audio: ByteArray?, context: List<ConversationTurn>): GuidanceResponse = withContext(Dispatchers.IO) {
        if (settings.token.isBlank()) throw IOException("Primero pegá el código de conexión desde la pantalla de inicio de Copiloto.")
        val body = JSONObject().apply {
            put("question", question ?: "")
            if (audio != null) put("audioBase64", Base64.encodeToString(audio, Base64.NO_WRAP))
            if (screen != null && !screen.likelyProtected) put("imageBase64", Base64.encodeToString(withCoordinateGrid(screen.jpeg), Base64.NO_WRAP))
            put("screen", JSONObject().put("width", screenWidth).put("height", screenHeight))
            put("context", JSONArray().apply { context.takeLast(6).forEach { turn ->
                put(JSONObject().put("role", turn.role).put("text", turn.text.take(800)))
            } })
        }
        val connection = open("/assist", "POST")
        active.set(connection)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val json = stream?.use { input ->
                val buffer = input.readBytesLimited(5_000_000)
                JSONObject(String(buffer, Charsets.UTF_8))
            } ?: throw IOException("El servidor no respondió. Intentá nuevamente.")
            if (status !in 200..299) throw IOException(json.optString("message", "No pude comunicarme con el asistente. Intentá nuevamente."))
            parseGuidance(json)
        } catch (error: IOException) {
            throw error
        } catch (_: Exception) {
            throw IOException("No pude entender la respuesta del asistente. Volvé a intentar.")
        } finally {
            active.compareAndSet(connection, null)
            connection.disconnect()
        }
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        require(settings.token.isNotBlank()) { "Falta el código de conexión." }
        val connection = open("/assist", "POST")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write("{}".toByteArray()) }
            // A validated 400 proves auth passed without spending an inference.
            val status = connection.responseCode
            if (status == 400) "Conexión lista. Ya podés iniciar el acompañamiento."
            else if (status == 401) throw IOException("El código de conexión no es correcto.")
            else throw IOException("El servidor no está disponible en este momento (HTTP $status).")
        } finally { connection.disconnect() }
    }

    private fun open(path: String, method: String): HttpURLConnection {
        val url = URL(settings.baseUrl + path)
        require(url.protocol == "https") { "La conexión necesita HTTPS." }
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 55_000
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer ${settings.token}")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun parseGuidance(json: JSONObject): GuidanceResponse {
        val target = json.getJSONObject("target")
        fun nullableNumber(key: String): Double? = if (target.isNull(key)) null else target.getDouble(key)
        val message = json.getString("message").trim()
        require(message.isNotBlank() && message.length <= 600)
        val pcm = json.optString("audioBase64").takeIf { it.isNotEmpty() && it != "null" }
            ?.let { Base64.decode(it, Base64.DEFAULT) }
            ?.takeIf { it.isNotEmpty() && it.size <= 1_500_000 && it.size % 2 == 0 && json.optInt("audioSampleRate", 24_000) == 24_000 }
        return GuidanceResponse(json.getBoolean("canHelp"), json.getString("heardQuestion"), message,
            GuidanceTarget(target.getBoolean("visible"), target.optString("label").takeUnless { it == "null" },
                nullableNumber("x"), nullableNumber("y"), nullableNumber("width"), nullableNumber("height"), target.getDouble("confidence")),
            GuidanceRisk.valueOf(json.getString("risk")), json.getBoolean("requiresConfirmation"), pcm, json.optString("model"))
    }

    override fun cancel() { active.getAndSet(null)?.disconnect() }
}

/** Adds reference axes to a private copy for grounding; never drawn on the user's screen. */
private fun withCoordinateGrid(jpeg: ByteArray): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size,
        BitmapFactory.Options().apply { inMutable = true }) ?: return jpeg
    try {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(115, 0, 145, 175); strokeWidth = 1.5f }
        for (i in 1..9) {
            val x = bitmap.width * i / 10f
            val y = bitmap.height * i / 10f
            canvas.drawLine(x, 0f, x, bitmap.height.toFloat(), paint)
            canvas.drawLine(0f, y, bitmap.width.toFloat(), y, paint)
        }
        val size = (bitmap.width * .027f).coerceAtLeast(16f)
        paint.textSize = size
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        for (i in 1..9) {
            val y = bitmap.height * i / 10f
            val x = bitmap.width * i / 10f
            paint.color = Color.argb(225, 255, 255, 255)
            canvas.drawRect(0f, y, size * 3.4f, y + size * 1.3f, paint)
            canvas.drawRect(x, 0f, x + size * 3.4f, size * 1.3f, paint)
            paint.color = Color.rgb(0, 100, 130)
            canvas.drawText("y=0.$i", 2f, y + size, paint)
            canvas.drawText("x=0.$i", x + 2f, size, paint)
        }
        return java.io.ByteArrayOutputStream().use { output ->
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)) output.toByteArray() else jpeg
        }
    } finally { bitmap.recycle() }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var count: Int
    while (read(buffer).also { count = it } != -1) {
        if (output.size() + count > limit) throw IOException("La respuesta del servidor es demasiado grande.")
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
