package gestaodeprojetos.alunos.cloud

/**
 * Configuração do projeto Supabase (Settings -> API no painel do Supabase).
 *
 * A chave `anon` (publishable) foi projetada pelo Supabase para uso em apps clientes
 * combinada com Row Level Security (RLS) na tabela `projects`.
 */
object SupabaseConfig {
    const val SUPABASE_URL: String = "https://azskzfruswjmvpbnfyeb.supabase.co/"
    const val SUPABASE_ANON_KEY: String = "sb_publishable_ymlND4IoMZ1od5j5ihpeYQ_JeCw4Wpb"

    fun isConfigured(): Boolean {
        val url = SUPABASE_URL.trim()
        val key = SUPABASE_ANON_KEY.trim()
        return url.startsWith("https://") &&
            !url.contains("SEU_PROJETO") &&
            key.isNotBlank() &&
            !key.contains("SUA_CHAVE")
    }

    fun normalizedBaseUrl(): String {
        val trimmed = SUPABASE_URL.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    /**
     * As chaves novas do Supabase (`sb_publishable_...`) NÃO são JWT e devem ser enviadas
     * apenas no header `apikey` (sem `Authorization: Bearer`). Já a chave legada `anon` (`eyJ...`)
     * é um JWT e pode ser enviada em ambos.
     */
    fun authorizationHeaderOrNull(): String? {
        val key = SUPABASE_ANON_KEY.trim()
        return if (key.startsWith("sb_")) null else "Bearer $key"
    }
}
