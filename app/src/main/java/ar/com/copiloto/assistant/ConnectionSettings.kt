package ar.com.copiloto.assistant

import android.content.Context
import ar.com.copiloto.BuildConfig
import java.net.URI

class ConnectionSettings(context: Context) {
    private val preferences = context.getSharedPreferences("backend_connection", Context.MODE_PRIVATE)
    var baseUrl: String
        get() = preferences.getString("base_url", BuildConfig.ASSISTANT_API_BASE_URL)!!
        private set(value) { preferences.edit().putString("base_url", value).apply() }
    var token: String
        get() = preferences.getString("connection_code", "")!!
        private set(value) { preferences.edit().putString("connection_code", value).apply() }

    fun save(url: String, code: String) {
        val uri = URI(url.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "El servidor debe usar una dirección HTTPS válida."
        }
        require(code.trim().length in 16..128 && code.trim().none { it.isWhitespace() || it.code < 33 || it.code > 126 }) {
            "Pegá el código de conexión que recibiste con el APK."
        }
        baseUrl = url.trim().trimEnd('/')
        token = code.trim()
    }
}
