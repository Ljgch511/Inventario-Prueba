package com.globalr.mobile

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val PREFS = "globalr_mobile_prefs"
    private const val KEY_URL = "server_url"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER = "user_summary"

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getServerUrl(ctx: Context): String {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, "http://10.0.2.2") ?: "http://10.0.2.2"
        return raw.trimEnd('/')
    }

    fun setServerUrl(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url.trim().trimEnd('/')).apply()
    }

    fun saveSession(ctx: Context, token: String, userJson: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).putString(KEY_USER, userJson).apply()
    }

    fun getToken(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun getUserJson(ctx: Context): JsonObject? {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, null) ?: return null
        return gson.fromJson(raw, JsonObject::class.java)
    }

    fun clearSession(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    fun testHealth(ctx: Context): String {
        val req = Request.Builder()
            .url("${getServerUrl(ctx)}/actuator/health")
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw RuntimeException("HTTP ${res.code}")
            return res.body?.string() ?: "OK"
        }
    }

    fun login(ctx: Context, username: String, password: String): JsonObject {
        val bodyObj = JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
        }
        val req = Request.Builder()
            .url("${getServerUrl(ctx)}/api/v1/auth/login")
            .header("X-Client-Channel", "APK")
            .post(gson.toJson(bodyObj).toRequestBody(jsonMedia))
            .build()
        http.newCall(req).execute().use { res ->
            val txt = res.body?.string() ?: "{}"
            if (!res.isSuccessful) throw RuntimeException(parseError(txt, res.code))
            val json = gson.fromJson(txt, JsonObject::class.java)
            saveSession(ctx, json.get("token").asString, json.getAsJsonObject("user").toString())
            return json
        }
    }

    fun scanQr(ctx: Context, encryptedPayload: String): JsonObject {
        val bodyObj = JsonObject().apply {
            addProperty("encryptedPayload", encryptedPayload)
        }
        val req = Request.Builder()
            .url("${getServerUrl(ctx)}/api/v1/assets/qr/scan")
            .header("Authorization", "Bearer ${getToken(ctx)}")
            .header("X-Client-Channel", "APK")
            .post(gson.toJson(bodyObj).toRequestBody(jsonMedia))
            .build()
        http.newCall(req).execute().use { res ->
            val txt = res.body?.string() ?: "{}"
            if (!res.isSuccessful) throw RuntimeException(parseError(txt, res.code))
            return gson.fromJson(txt, JsonObject::class.java)
        }
    }

    fun uploadPhoto(ctx: Context, assetId: String, stage: String, bytes: ByteArray): JsonObject {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "field_photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val req = Request.Builder()
            .url("${getServerUrl(ctx)}/api/v1/assets/$assetId/media?stage=$stage")
            .header("Authorization", "Bearer ${getToken(ctx)}")
            .header("X-Client-Channel", "APK")
            .post(multipart)
            .build()
        http.newCall(req).execute().use { res ->
            val txt = res.body?.string() ?: "{}"
            if (!res.isSuccessful) throw RuntimeException(parseError(txt, res.code))
            return gson.fromJson(txt, JsonObject::class.java)
        }
    }

    fun createQuickTicket(ctx: Context, assetId: String, assetNumber: String, reason: String): JsonObject {
        val bodyObj = JsonObject().apply {
            addProperty("ticketType", "MANTENIMIENTO")
            addProperty("priority", "MEDIA")
            addProperty("title", "Reporte de campo APK — $assetNumber")
            addProperty("reason", reason)
            addProperty("assetId", assetId)
        }
        val req = Request.Builder()
            .url("${getServerUrl(ctx)}/api/v1/tickets")
            .header("Authorization", "Bearer ${getToken(ctx)}")
            .header("X-Client-Channel", "APK")
            .post(gson.toJson(bodyObj).toRequestBody(jsonMedia))
            .build()
        http.newCall(req).execute().use { res ->
            val txt = res.body?.string() ?: "{}"
            if (!res.isSuccessful) throw RuntimeException(parseError(txt, res.code))
            return gson.fromJson(txt, JsonObject::class.java)
        }
    }

    private fun parseError(txt: String, code: Int): String {
        return try {
            val obj = gson.fromJson(txt, JsonObject::class.java)
            obj.get("message")?.asString ?: "Error HTTP $code"
        } catch (_: Exception) {
            "Error HTTP $code"
        }
    }
}
