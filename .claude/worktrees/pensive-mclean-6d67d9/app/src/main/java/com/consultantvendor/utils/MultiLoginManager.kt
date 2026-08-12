package com.consultantvendor.utils

import android.content.Context
import com.consultantvendor.data.models.responses.UserSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MultiLoginManager {
    private const val PREF_NAME = "multi_login_prefs"
    private const val KEY_USERS = "logged_in_users"

//    fun saveUser(context: Context, user: UserSession) {
//        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        val gson = Gson()
//        val users = getUsers(context).toMutableList()
//        users.removeAll { it.userId == user.userId }
//        users.add(0, user)
//        prefs.edit().putString(KEY_USERS, gson.toJson(users)).apply()
//    }

    fun saveUser(context: Context, user: UserSession) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val users = getUsers(context).toMutableList()

        // Find the index of the existing user
        val index = users.indexOfFirst { it.userId == user.userId }

        if (index != -1) {
            users[index] = user // Replace at the same position
        } else {
            users.add(user) // New user, add to the end
        }

        prefs.edit().putString(KEY_USERS, gson.toJson(users)).apply()
    }

    fun getUsers(context: Context): List<UserSession> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_USERS, "[]")
        val type = object : TypeToken<List<UserSession>>() {}.type
        return gson.fromJson(json, type)
    }

    fun removeUser(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val updatedList = getUsers(context).filterNot { it.userId == userId }
        prefs.edit().putString(KEY_USERS, Gson().toJson(updatedList)).apply()
    }
}