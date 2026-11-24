package gestaodeprojetos.alunos.cloud

import android.content.Context
import java.security.SecureRandom
import java.util.Locale

object SyncCodeManager {
    private const val PREFS_NAME = "unifocus_cloud_prefs"
    private const val KEY_SYNC_CODE = "sync_code"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun getSyncCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_SYNC_CODE, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = generateNewCode()
        prefs.edit().putString(KEY_SYNC_CODE, generated).apply()
        return generated
    }

    fun setSyncCode(context: Context, rawCode: String): String {
        val normalized = normalize(rawCode)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SYNC_CODE, normalized)
            .apply()
        return normalized
    }

    fun normalize(rawCode: String): String {
        return rawCode.trim().uppercase(Locale.US)
    }

    fun isValid(code: String): Boolean {
        return normalize(code).length >= 6
    }

    private fun generateNewCode(): String {
        fun block(): String = (1..4).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        return "UF-${block()}-${block()}-${block()}"
    }
}
