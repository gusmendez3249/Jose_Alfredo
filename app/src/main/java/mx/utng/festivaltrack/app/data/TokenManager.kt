package mx.utng.festivaltrack.app.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("festival_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getUserRole(): String? {
        return prefs.getString("user_role", null)
    }

    fun saveUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
