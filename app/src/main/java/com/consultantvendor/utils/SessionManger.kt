package com.consultantvendor.utils

import android.content.Context
import com.consultantvendor.data.models.responses.UserSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SessionManager {
    private const val PREFS_NAME = "multi_login_prefs"
    private const val KEY_SESSIONS = "sessions"

    fun saveSessions(context: Context, sessions: List<UserSession>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val jsonString = Gson().toJson(sessions)
        editor.putString(KEY_SESSIONS, jsonString)
        editor.apply()
    }

    fun getSessions(context: Context): List<UserSession> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SESSIONS, null)
        return if (jsonString != null) {
            val type = object : TypeToken<List<UserSession>>() {}.type
            Gson().fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }

    fun saveCurrentSession(context: Context, session: UserSession) {
        saveSessions(context, getSessions(context) + session)
    }

    fun deleteSession(context: Context, session: UserSession) {
        val updatedList = getSessions(context).filter { it.userId != session.userId }
        saveSessions(context, updatedList)
    }
}