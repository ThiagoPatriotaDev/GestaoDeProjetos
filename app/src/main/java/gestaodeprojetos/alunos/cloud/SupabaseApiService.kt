package gestaodeprojetos.alunos.cloud

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SupabaseApiService {

    @GET("rest/v1/projects")
    suspend fun getProjects(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String?,
        @Header("x-sync-code") syncCodeHeader: String,
        @Query("sync_code") syncCodeFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseProjectDto>>

    @POST("rest/v1/projects")
    suspend fun createProject(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String?,
        @Header("x-sync-code") syncCodeHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body payload: SupabaseProjectPayload
    ): Response<List<SupabaseProjectDto>>

    @PATCH("rest/v1/projects")
    suspend fun updateProject(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String?,
        @Header("x-sync-code") syncCodeHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body payload: SupabaseProjectPayload
    ): Response<List<SupabaseProjectDto>>

    @DELETE("rest/v1/projects")
    suspend fun deleteProject(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String?,
        @Header("x-sync-code") syncCodeHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>
}

object SupabaseApiClient {
    private const val LOG_TAG = "SupabaseApi"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(LOG_TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("apikey")
        redactHeader("Authorization")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.normalizedBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApiService::class.java)
    }
}
